package com.windschief.discovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.TasteSource;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.added_item.AddedItemType;

class PersonalDiscoveryServiceTest {
    final MusicCatalog catalog = mock(MusicCatalog.class);
    final SimilarSongs similar = mock(SimilarSongs.class);
    final SpotifyTokenService tokens = mock(SpotifyTokenService.class);
    final TaskRepository tasks = mock(TaskRepository.class);
    final AddedItemRepository history = mock(AddedItemRepository.class);
    final PersonalDiscoveryService service = new PersonalDiscoveryService(catalog, similar, tokens, tasks, history);
    Task task;
    DiscoveryTrack favorite;

    static DiscoveryTrack track(String id, String name, String artist, String album, String isrc) {
        return new DiscoveryTrack(id, name, List.of(new DiscoveryTrack.Artist(artist, artist)), album,
                LocalDate.of(2020, 1, 1), isrc);
    }

    @BeforeEach
    void setup() throws Exception {
        task = new Task();
        task.setId(42L);
        task.setUserId("user");
        task.setPlaylistId("destination");
        task.setTasteSource(TasteSource.LIKED_SONGS);
        task.setCheckFrom(LocalDate.of(2026, 1, 1));
        favorite = track("favorite", "My favorite", "artist-a", "old-album", "KNOWN");
        when(tasks.findById(42L)).thenReturn(task);
        when(tokens.getValidBearerAccessToken("user")).thenReturn("Bearer fixture");
        when(catalog.savedTracks("Bearer fixture")).thenReturn(List.of(favorite));
        when(catalog.playlistTracks("Bearer fixture", "destination")).thenReturn(List.of());
        when(history.findByUserId("user")).thenReturn(List.of());
    }

    @Test
    void worksWithoutASimilarityKeyAndStoresRecordingIdentityAndReason() throws Exception {
        DiscoveryTrack candidate = track("new", "Unheard song", "artist-a", "album", "NEW");
        when(catalog.artistTracks("Bearer fixture", "artist-a")).thenReturn(List.of(favorite, candidate));

        String message = service.execute(42L);

        verify(catalog).writePlaylist("Bearer fixture", "destination", List.of(candidate), false);
        ArgumentCaptor<AddedItem> stored = ArgumentCaptor.forClass(AddedItem.class);
        verify(history).persist(stored.capture());
        assertEquals(candidate.recordingKey(), stored.getValue().getRecordingKey());
        assertEquals("NEW", stored.getValue().getRecordingIsrc());
        assertTrue(stored.getValue().getReason().contains("artist-a"));
        assertTrue(message.contains("not configured"));
    }

    @Test
    void playlistSourceStillExcludesSavedSongsAndDestinationSongs() throws Exception {
        task.setTasteSource(TasteSource.PLAYLIST);
        task.setFavoritePlaylistId("source");
        DiscoveryTrack source = track("source-song", "Source", "artist-a", "album-1", "SOURCE");
        DiscoveryTrack inDestination = track("existing", "Already there", "artist-a", "album-2", "DEST");
        DiscoveryTrack newSong = track("unheard", "Unheard", "artist-a", "album-3", "NEW");
        when(catalog.playlistTracks("Bearer fixture", "source")).thenReturn(List.of(source));
        when(catalog.playlistTracks("Bearer fixture", "destination")).thenReturn(List.of(inDestination));
        when(catalog.artistTracks("Bearer fixture", "artist-a")).thenReturn(List.of(source, favorite, inDestination, newSong));

        service.execute(42L);

        verify(catalog).writePlaylist("Bearer fixture", "destination", List.of(newSong), false);
    }

    @Test
    void historyFromAnotherTaskBlocksDifferentIdsOfTheSameRecording() throws Exception {
        DiscoveryTrack previous = track("old-id", "A song", "artist-a", "old", "SAME");
        DiscoveryTrack reissue = track("new-id", "A song - 2026 Remaster", "artist-a", "new", "DIFFERENT");
        AddedItem item = new AddedItem();
        item.setItemType(AddedItemType.TRACK);
        item.setExternalId(previous.id());
        item.setRecordingKey(previous.recordingKey());
        item.setRecordingIsrc(previous.isrc());
        when(history.findByUserId("user")).thenReturn(List.of(item));
        when(catalog.artistTracks("Bearer fixture", "artist-a")).thenReturn(List.of(reissue));
        task.setReplacePlaylist(true);

        assertTrue(service.execute(42L).contains("No unheard songs"));
        verify(catalog, never()).writePlaylist(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void similarSongsCanBeOlderAndComeFromANewArtist() throws Exception {
        DiscoveryTrack older = track("older-discovery", "Older gem", "new-artist", "older-album", "OLDER");
        when(similar.available()).thenReturn(true);
        when(similar.find(favorite)).thenReturn(List.of(new SimilarSongs.Match("new-artist", "Older gem", .9)));
        when(catalog.resolve("Bearer fixture", "new-artist", "Older gem")).thenReturn(Optional.of(older));

        service.execute(42L);

        verify(catalog).writePlaylist("Bearer fixture", "destination", List.of(older), false);
        ArgumentCaptor<AddedItem> item = ArgumentCaptor.forClass(AddedItem.class);
        verify(history).persist(item.capture());
        assertTrue(item.getValue().getReason().contains("Similar to My favorite"));
    }

    @Test
    void similarityOutageHasAnExplicitWarningAndKeepsArtistDiscoveries() throws Exception {
        DiscoveryTrack candidate = track("new", "Unheard", "artist-a", "album", "NEW");
        when(similar.available()).thenReturn(true);
        when(similar.find(favorite)).thenThrow(new DiscoveryException("Unavailable"));
        when(catalog.artistTracks("Bearer fixture", "artist-a")).thenReturn(List.of(candidate));

        assertTrue(service.execute(42L).contains("temporarily unavailable"));
        verify(catalog).writePlaylist("Bearer fixture", "destination", List.of(candidate), false);
    }

    @Test
    void doesNotWriteWithEmptyOrUnreadableSources() throws Exception {
        when(catalog.savedTracks("Bearer fixture")).thenReturn(List.of());
        assertThrows(DiscoveryException.class, () -> service.execute(42L));
        when(catalog.savedTracks("Bearer fixture")).thenThrow(new DiscoveryException("Reconnect Spotify"));
        assertThrows(DiscoveryException.class, () -> service.execute(42L));
        verify(catalog, never()).writePlaylist(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void neverWritesWhenTheDestinationCannotBeReadCompletely() {
        when(catalog.playlistTracks("Bearer fixture", "destination"))
                .thenThrow(new DiscoveryException("Incomplete collection"));
        assertThrows(DiscoveryException.class, () -> service.execute(42L));
        verify(catalog, never()).writePlaylist(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void replacementRequiresTheStoredExplicitChoice() throws Exception {
        task.setReplacePlaylist(true);
        DiscoveryTrack candidate = track("new", "Unheard", "artist-a", "album", "NEW");
        when(catalog.artistTracks("Bearer fixture", "artist-a")).thenReturn(List.of(candidate));

        service.execute(42L);

        verify(catalog).writePlaylist("Bearer fixture", "destination", List.of(candidate), true);
    }

    @Test
    void selectionCapsTheBatchAndBalancesAlbumsAndArtists() {
        List<PersonalDiscoveryService.Candidate> candidates = new ArrayList<>();
        for (int a = 0; a < 10; a++) {
            for (int n = 0; n < 8; n++) {
                DiscoveryTrack track = track(a + "-" + n, "Song " + n, "artist-" + a, "album-" + a, a + "-" + n);
                candidates.add(new PersonalDiscoveryService.Candidate(track, 100 - a, n % 2 == 0, "fixture"));
            }
        }

        var selected = PersonalDiscoveryService.select(candidates, new HashSet<>(), 7, task);

        assertEquals(7, selected.size());
        assertTrue(selected.stream().filter(PersonalDiscoveryService.Candidate::recent).count() >= 3);
        selected.stream().map(c -> c.track().artistKey()).distinct().forEach(a ->
            assertTrue(selected.stream().filter(c -> c.track().artistKey().equals(a)).count() <= 2));
    }

    @Test
    void deduplicatesIdsIsrcAndRemastersWhilePreservingLiveVersions() {
        DiscoveryTrack original = track("one", "A Song", "Artist", "album", "REC");
        var known = new HashSet<String>();
        PersonalDiscoveryService.remember(known, original);
        assertTrue(PersonalDiscoveryService.isKnown(known, track("two", "Different title", "Artist", "other", "REC")));
        assertTrue(PersonalDiscoveryService.isKnown(known, track("three", "A Song (2026 Remaster)", "Artist", "other", "OTHER")));
        assertFalse(PersonalDiscoveryService.isKnown(known, track("four", "A Song (Live)", "Artist", "live", "LIVE")));
    }

    static MusicCatalog.PlaylistAddition added(DiscoveryTrack track, Instant at) {
        return new MusicCatalog.PlaylistAddition(track, at);
    }

    static List<MusicCatalog.PlaylistAddition> additions(String prefix, int count, Instant newest, Duration step) {
        List<MusicCatalog.PlaylistAddition> result = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) { // playlist order: oldest first
            result.add(added(track(prefix + i, "Song " + prefix + i, "artist-" + prefix + i, "album", null),
                    newest.minus(step.multipliedBy(i))));
        }
        return result;
    }

    @Test
    void recentSeedsUseTheWindowAndTopUpQuietPeriodsToTen() {
        Instant now = Instant.parse("2026-11-06T12:00:00Z");
        // Three songs in the last week, then older ones.
        var list = additions("s", 15, now.minus(Duration.ofDays(1)), Duration.ofDays(3));
        var seeds = PersonalDiscoveryService.recentSeeds(list.reversed(), 7, now);
        assertEquals(3, seeds.inWindow());
        assertEquals(10, seeds.tracks().size());
        assertEquals("s0", seeds.tracks().getFirst().id());

        var none = PersonalDiscoveryService.recentSeeds(list.reversed(), 7, now.plus(Duration.ofDays(400)));
        assertEquals(0, none.inWindow());
        assertEquals(List.of("s0", "s1", "s2"), none.tracks().stream().limit(3).map(DiscoveryTrack::id).toList());
    }

    @Test
    void recentSeedsKeepEveryWindowSongUpToTheCapAndPreferTheNewestDuplicate() {
        Instant now = Instant.parse("2026-11-06T12:00:00Z");
        var busy = additions("b", 40, now, Duration.ofHours(1));
        var seeds = PersonalDiscoveryService.recentSeeds(busy.reversed(), 7, now);
        assertEquals(40, seeds.inWindow());
        assertEquals(PersonalDiscoveryService.MAX_RECENT_SEEDS, seeds.tracks().size());

        DiscoveryTrack shared = track("shared", "Shared", "artist-x", "album", null);
        DiscoveryTrack old = track("old", "Old", "artist-y", "album", null);
        var merged = PersonalDiscoveryService.recentSeeds(List.of(
                added(shared, now.minus(Duration.ofDays(30))), added(old, now.minus(Duration.ofDays(20))),
                added(shared, now.minus(Duration.ofDays(2))), added(track("legacy", "No date", "artist-z", "album", null), null)),
                7, now);
        assertEquals(1, merged.inWindow());
        assertEquals(List.of("shared", "old", "legacy"), merged.tracks().stream().map(DiscoveryTrack::id).toList());
    }

    @Test
    void recentAdditionsFromSeveralPlaylistsDriveSimilarSongs() throws Exception {
        task.setTasteSource(TasteSource.RECENT_ADDITIONS);
        task.setSourcePlaylistIds(List.of("mix-a", "mix-b"));
        task.setRecentDays(7);
        Instant now = Instant.now();
        DiscoveryTrack newA = track("new-a", "Fresh A", "artist-a", "album-a", "FA");
        DiscoveryTrack newB = track("new-b", "Fresh B", "artist-b", "album-b", "FB");
        DiscoveryTrack oldB = track("old-b", "Old B", "artist-c", "album-c", "OB");
        when(catalog.playlistAdditions("Bearer fixture", "mix-a")).thenReturn(List.of(added(newA, now.minus(Duration.ofDays(1)))));
        when(catalog.playlistAdditions("Bearer fixture", "mix-b")).thenReturn(List.of(
                added(oldB, now.minus(Duration.ofDays(200))), added(newB, now.minus(Duration.ofDays(2)))));
        DiscoveryTrack forA = track("rec-a", "Like A", "other-1", "x", "RA");
        DiscoveryTrack forB = track("rec-b", "Like B", "other-2", "y", "RB");
        DiscoveryTrack alreadyInSource = oldB;
        when(similar.available()).thenReturn(true);
        when(similar.find(newA)).thenReturn(List.of(new SimilarSongs.Match("other-1", "Like A", .8)));
        when(similar.find(newB)).thenReturn(List.of(new SimilarSongs.Match("other-2", "Like B", .7),
                new SimilarSongs.Match("artist-c", "Old B", .9)));
        when(catalog.resolve("Bearer fixture", "other-1", "Like A")).thenReturn(Optional.of(forA));
        when(catalog.resolve("Bearer fixture", "other-2", "Like B")).thenReturn(Optional.of(forB));
        when(catalog.resolve("Bearer fixture", "artist-c", "Old B")).thenReturn(Optional.of(alreadyInSource));

        String message = service.execute(42L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryTrack>> written = ArgumentCaptor.forClass(List.class);
        verify(catalog).writePlaylist(eq("Bearer fixture"), eq("destination"), written.capture(), eq(false));
        assertTrue(written.getValue().containsAll(List.of(forA, forB)));
        assertFalse(written.getValue().contains(oldB));
        verify(similar).find(oldB); // only two songs in the window: older additions top up the seeds
        assertTrue(message.contains("Only 2 songs were added in the last 7 days"), message);
        verify(catalog, never()).playlistTracks("Bearer fixture", "mix-a");
        ArgumentCaptor<AddedItem> items = ArgumentCaptor.forClass(AddedItem.class);
        verify(history, atLeastOnce()).persist(items.capture());
        assertTrue(items.getAllValues().stream().anyMatch(i -> i.getReason().contains("recent addition Fresh A")));
    }

    @Test
    void recentAdditionsRefuseToWriteIntoASourcePlaylist() {
        task.setTasteSource(TasteSource.RECENT_ADDITIONS);
        task.setSourcePlaylistIds(List.of("mix-a", "destination"));
        assertThrows(DiscoveryException.class, () -> service.execute(42L));
        verify(catalog, never()).writePlaylist(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void emptySourcePlaylistsDoNotWrite() {
        task.setTasteSource(TasteSource.RECENT_ADDITIONS);
        task.setSourcePlaylistIds(List.of("mix-a"));
        when(catalog.playlistAdditions("Bearer fixture", "mix-a")).thenReturn(List.of());
        assertThrows(DiscoveryException.class, () -> service.execute(42L));
        verify(catalog, never()).writePlaylist(any(), any(), anyList(), anyBoolean());
    }
}
