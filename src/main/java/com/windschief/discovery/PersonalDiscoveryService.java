package com.windschief.discovery;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.windschief.auth.SpotifyTokenException;
import com.windschief.auth.SpotifyTokenService;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.TasteSource;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.added_item.AddedItemType;
import com.windschief.task.item.TaskItem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PersonalDiscoveryService {
    public record Candidate(DiscoveryTrack track, double score, boolean recent, String reason) {}
    /** Seed songs for a recently-added task and how many of them fall inside the look-back window. */
    public record RecentSeeds(List<DiscoveryTrack> tracks, int inWindow) {}
    /** Fewer additions inside the window are topped up with the most recently added songs. */
    static final int MIN_RECENT_SEEDS = 10;
    static final int MAX_RECENT_SEEDS = 25;
    /** Releases from this many days are preferred for up to half of each selection. */
    static final int RECENT_RELEASE_DAYS = 30;
    private final MusicCatalog catalog;
    private final SimilarSongs similar;
    private final SpotifyTokenService tokens;
    private final TaskRepository tasks;
    private final AddedItemRepository history;

    @Inject
    public PersonalDiscoveryService(MusicCatalog catalog, SimilarSongs similar, SpotifyTokenService tokens,
            TaskRepository tasks, AddedItemRepository history) {
        this.catalog = catalog;
        this.similar = similar;
        this.tokens = tokens;
        this.tasks = tasks;
        this.history = history;
    }

    public String execute(long taskId) throws SpotifyTokenException {
        Task task = load(taskId);
        boolean recentMode = task.getTasteSource() == TasteSource.RECENT_ADDITIONS;
        if (recentMode ? task.getSourcePlaylistIds().isEmpty() || task.getSourcePlaylistIds().contains(task.getPlaylistId())
                : Objects.equals(task.getPlaylistId(), task.getFavoritePlaylistId())) {
            throw new DiscoveryException("Choose separate source and discovery playlists.");
        }
        String token = tokens.getValidBearerAccessToken(task.getUserId());
        List<DiscoveryTrack> saved = catalog.savedTracks(token);
        List<DiscoveryTrack> source;
        List<DiscoveryTrack> seeds;
        String seedNote = null;
        if (recentMode) {
            List<MusicCatalog.PlaylistAddition> additions = new ArrayList<>();
            for (String playlistId : task.getSourcePlaylistIds()) {
                // Newest playlist positions first, so entries without a timestamp still favor recent additions.
                additions.addAll(catalog.playlistAdditions(token, playlistId).reversed());
            }
            source = additions.stream().map(MusicCatalog.PlaylistAddition::track).toList();
            RecentSeeds recent = recentSeeds(additions, task.getRecentDays(), Instant.now());
            seeds = recent.tracks();
            seedNote = describe(recent, task.getRecentDays());
        } else {
            source = task.getTasteSource() == TasteSource.LIKED_SONGS ? saved
                    : catalog.playlistTracks(token, task.getFavoritePlaylistId());
            seeds = source.stream().sorted(Comparator.comparingInt(t -> rotation(task, t.id()))).toList();
        }
        if (source.isEmpty()) throw new DiscoveryException("Add some songs to your taste source before starting discovery.");
        List<DiscoveryTrack> destination = catalog.playlistTracks(token, task.getPlaylistId());
        Set<String> known = new HashSet<>();
        saved.forEach(t -> remember(known, t));
        source.forEach(t -> remember(known, t));
        destination.forEach(t -> remember(known, t));
        for (AddedItem item : readHistory(task.getUserId())) {
            if (item.getItemType() == AddedItemType.TRACK) {
                known.add("id:" + item.getExternalId());
                if (item.getRecordingKey() != null) known.add("meta:" + item.getRecordingKey());
                if (item.getRecordingIsrc() != null) known.add("isrc:" + item.getRecordingIsrc());
            }
        }

        Map<String, Integer> affinity = new HashMap<>();
        (recentMode ? seeds : source).forEach(t -> t.artists().forEach(a -> affinity.merge(a.id(), 1, Integer::sum)));
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Set<String> artistIds = new java.util.LinkedHashSet<>();
        task.getTaskItems().stream().filter(item -> item.getItemType() == com.windschief.task.item.TaskItemType.ARTIST)
                .map(TaskItem::getExternalId).filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(id -> rotation(task, id))).limit(10).forEach(artistIds::add);
        affinity.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry::getKey)).limit(10).forEach(e -> {
                    if (artistIds.size() < 10) artistIds.add(e.getKey());
                });
        // A rolling window: personal discovery ignores the task's fixed checkFrom date.
        LocalDate from = LocalDate.now().minusDays(RECENT_RELEASE_DAYS);
        for (String artistId : artistIds) {
            for (DiscoveryTrack track : catalog.artistTracks(token, artistId)) {
                boolean recent = track.releaseDate() != null && !track.releaseDate().isBefore(from);
                double score = 10 + Math.log1p(affinity.getOrDefault(artistId, 0)) + (recent ? 10 : 0);
                add(candidates, new Candidate(track, score, recent,
                        (recent ? "New release from " : "More from an artist in your taste: ") + track.primaryArtist()));
            }
        }

        String warning = null;
        if (!similar.available()) {
            warning = "Similar-artist discovery is not configured; this run uses artists from your taste source and artist selection.";
        } else {
            // Legacy sources sample one song for each of ten artists; recent additions use each new song,
            // allowing two per artist so a single artist does not dominate.
            int perArtist = recentMode ? 2 : 1, maxSeeds = recentMode ? MAX_RECENT_SEEDS : 10;
            Map<String, Integer> seedArtists = new HashMap<>();
            int usedSeeds = 0;
            Map<String, SimilarSongs.Match> matches = new LinkedHashMap<>();
            Map<String, DiscoveryTrack> reasons = new HashMap<>();
            for (DiscoveryTrack seed : seeds) {
                if (seedArtists.merge(seed.artistKey(), 1, Integer::sum) > perArtist) continue;
                usedSeeds++;
                try {
                    for (SimilarSongs.Match match : similar.find(seed)) {
                        String key = DiscoveryTrack.normalize(match.artist()) + "|" + DiscoveryTrack.normalize(match.title());
                        if (matches.containsKey(key)) {
                            SimilarSongs.Match previous = matches.get(key);
                            matches.put(key, new SimilarSongs.Match(match.artist(), match.title(), previous.score() + match.score()));
                        } else {
                            matches.put(key, match);
                            reasons.put(key, seed);
                        }
                    }
                } catch (DiscoveryException e) {
                    warning = "Similar-song search is temporarily unavailable; this run uses the available artist discoveries.";
                    break;
                }
                if (usedSeeds >= maxSeeds) break;
            }
            int resolved = 0, maxResolved = recentMode ? Math.max(30, 2 * task.getDiscoveryLimit()) : 30;
            for (var entry : matches.entrySet().stream()
                    .sorted(Map.Entry.<String, SimilarSongs.Match>comparingByValue(
                            Comparator.comparingDouble(SimilarSongs.Match::score)).reversed()).toList()) {
                if (resolved++ >= maxResolved) break;
                SimilarSongs.Match match = entry.getValue();
                DiscoveryTrack seed = reasons.get(entry.getKey());
                catalog.resolve(token, match.artist(), match.title()).ifPresent(track ->
                    add(candidates, new Candidate(track, 40 + match.score() * 10,
                            track.releaseDate() != null && !track.releaseDate().isBefore(from),
                            (recentMode ? "Similar to your recent addition " : "Similar to ")
                                    + seed.name() + " by " + seed.primaryArtist())));
            }
        }

        String notes = (seedNote == null ? "" : " " + seedNote) + (warning == null ? "" : " " + warning);
        List<Candidate> selected = select(new ArrayList<>(candidates.values()), known, task.getDiscoveryLimit(), task);
        if (selected.isEmpty()) {
            return "No unheard songs found. Your discovery playlist was kept." + notes;
        }
        catalog.writePlaylist(token, task.getPlaylistId(), selected.stream().map(Candidate::track).toList(),
                task.isReplacePlaylist());
        store(task, selected);
        return "Added " + selected.size() + " discoveries." + notes;
    }

    /**
     * Uses every song added within the window (newest first, at most {@value #MAX_RECENT_SEEDS}). Quiet
     * periods are topped up to the {@value #MIN_RECENT_SEEDS} most recently added songs so a run never starves.
     * Expects newest playlist positions first for entries without a timestamp.
     */
    public static RecentSeeds recentSeeds(List<MusicCatalog.PlaylistAddition> additions, int days, Instant now) {
        Instant since = now.minus(Duration.ofDays(days));
        Map<String, MusicCatalog.PlaylistAddition> newest = new LinkedHashMap<>();
        for (MusicCatalog.PlaylistAddition addition : additions) {
            newest.merge(addition.track().id(), addition, (old, added) -> old.addedAt() == null
                    || (added.addedAt() != null && added.addedAt().isAfter(old.addedAt())) ? added : old);
        }
        List<MusicCatalog.PlaylistAddition> sorted = newest.values().stream()
                .sorted(Comparator.comparing(MusicCatalog.PlaylistAddition::addedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        int inWindow = (int) sorted.stream()
                .filter(a -> a.addedAt() != null && !a.addedAt().isBefore(since)).count();
        int count = Math.min(MAX_RECENT_SEEDS, Math.max(inWindow, MIN_RECENT_SEEDS));
        return new RecentSeeds(sorted.stream().limit(count).map(MusicCatalog.PlaylistAddition::track).toList(), inWindow);
    }

    private static String describe(RecentSeeds seeds, int days) {
        String window = days == 1 ? "the last day" : "the last " + days + " days";
        int used = seeds.tracks().size(), inWindow = seeds.inWindow();
        if (inWindow > used) return "Based on the " + used + " newest of " + inWindow + " songs added in " + window + ".";
        if (inWindow == used) return "Based on " + songs(used) + " added in " + window + ".";
        return (inWindow == 0 ? "No songs were" : "Only " + songs(inWindow) + (inWindow == 1 ? " was" : " were"))
                + " added in " + window + ", so the " + songs(used).replace(" song", " most recently added song")
                + " " + (used == 1 ? "was" : "were") + " used.";
    }

    private static String songs(int count) { return count + (count == 1 ? " song" : " songs"); }

    public static List<Candidate> select(List<Candidate> candidates, Set<String> known, int limit, Task task) {
        List<Candidate> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(Candidate::score).reversed()
                        .thenComparingInt(c -> rotation(task, c.track().id()))).toList();
        List<Candidate> selected = new ArrayList<>();
        Map<String, Integer> albums = new HashMap<>(), artists = new HashMap<>();
        Set<String> seen = new HashSet<>(known);
        for (Candidate candidate : sorted) {
            if (selected.size() >= limit / 2) break;
            if (candidate.recent()) pick(candidate, seen, albums, artists, selected);
        }
        for (Candidate candidate : sorted) {
            if (selected.size() >= limit) break;
            pick(candidate, seen, albums, artists, selected);
        }
        return List.copyOf(selected);
    }

    private static void pick(Candidate c, Set<String> known, Map<String, Integer> albums,
            Map<String, Integer> artists, List<Candidate> selected) {
        DiscoveryTrack t = c.track();
        if (isKnown(known, t) || albums.getOrDefault(t.albumId(), 0) >= 2
                || artists.getOrDefault(t.artistKey(), 0) >= 2) return;
        selected.add(c);
        remember(known, t);
        albums.merge(t.albumId(), 1, Integer::sum);
        artists.merge(t.artistKey(), 1, Integer::sum);
    }
    public static boolean isKnown(Set<String> known, DiscoveryTrack t) {
        return known.contains("id:" + t.id()) || known.contains("meta:" + t.recordingKey())
                || (t.isrc() != null && known.contains("isrc:" + t.isrc()));
    }
    public static void remember(Set<String> known, DiscoveryTrack t) {
        known.add("id:" + t.id());
        known.add("meta:" + t.recordingKey());
        if (t.isrc() != null) known.add("isrc:" + t.isrc());
    }
    private static int rotation(Task task, String id) {
        long day = (task.getLastTimeExecuted() == null ? Instant.now() : task.getLastTimeExecuted())
                .atOffset(ZoneOffset.UTC).toLocalDate().toEpochDay();
        return Objects.hash(task.getId(), day, id);
    }
    private void add(Map<String, Candidate> candidates, Candidate c) {
        candidates.merge(c.track().id(), c, (old, added) -> added.score() > old.score() ? added : old);
    }
    @Transactional
    protected Task load(long id) {
        Task task = tasks.findById(id);
        if (task == null) throw new DiscoveryException("Task not found.");
        task.getTaskItems().size();
        return task;
    }
    @Transactional
    protected List<AddedItem> readHistory(String userId) { return history.findByUserId(userId); }
    @Transactional
    protected void store(Task task, List<Candidate> candidates) {
        Task managedTask = tasks.findById(task.getId());
        for (Candidate candidate : candidates) {
            AddedItem item = new AddedItem();
            item.setTask(managedTask);
            item.setItemType(AddedItemType.TRACK);
            item.setExternalId(candidate.track().id());
            item.setAddedAt(Instant.now());
            item.setRecordingKey(candidate.track().recordingKey());
            item.setRecordingIsrc(candidate.track().isrc());
            item.setReason(candidate.reason().substring(0, Math.min(candidate.reason().length(), 1000)));
            history.persist(item);
        }
    }
}
