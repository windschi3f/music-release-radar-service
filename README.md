# music-release-radar-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Personal music discovery (2026-10-30)

Choose a taste source per task: Spotify **Liked Songs**, or an existing playlist (`tasteSource: PLAYLIST`, `favoritePlaylistId`). The destination is `playlistId` and must differ from the source. Legacy tasks with no taste source retain the original artist-release behavior.

Personalized runs:

- Read the complete saved library, the selected source and the current destination before writing. Missing permissions, incomplete pagination and empty taste sources fail without changing the playlist.
- Exclude saved songs, source songs, current destination songs and tracks previously suggested in any of the user's tasks. Spotify IDs, available ISRCs and a normalized title/artist fingerprint recognize repeat recordings and remasters. Live/remix titles remain distinct. Legacy history without recording metadata initially excludes exact IDs only.
- Search the selected artists and artists present in the taste source. Up to ten artists and three albums/singles per artist are scanned per run. Rank by artist affinity and recency; unscored album tracks rotate deterministically between runs.
- When configured, query Last.fm for similar tracks using up to ten source artists, combine matches, and resolve up to thirty candidate songs on Spotify. Matching requires the artist and title, with remaster suffixes normalized. Similar-track recommendations may be older than the 30-day window.
- Select up to `discoveryLimit` songs (default 20, range 1–50), at most two per album and primary artist. Reserve up to half the selection for recent releases (a rolling window of the last 30 days; since 2026-11-06 the task's fixed `checkFrom` is ignored and not stored for discovery tasks) when available; fill remaining places by score. Small or familiar candidate pools can yield fewer songs.
- Append by default. `replacePlaylist: true` explicitly opts into replacing the destination on each successful run. An empty selection never clears it. Suggested recordings remain in history after replacement, and each new history item records its selection reason.
- Record a safe outcome message. Failed runs preserve the previous successful execution time and release the processing lock; the UI can retry. A queued task is marked as processing synchronously to prevent simultaneous runs in this process.

This is a metadata/listening-pattern heuristic, not an audio model. “Unheard” means absent from the collections and suggestion history above; the app does not know the user's entire lifetime listening history. A source playlist is always read-only.

### Recently added to playlists (2026-11-06)

`tasteSource: RECENT_ADDITIONS` takes one to ten source playlists (`sourcePlaylistIds`) instead of a favorite playlist, plus a look-back window `recentDays` (1–90, default 7). Each source playlist is read completely, including Spotify's `added_at` per entry; a song in several sources counts once with its newest addition time.

- Seeds are all songs added within the window, newest first, capped at 25. If fewer than ten were added in that window, the seeds are topped up to the ten most recently added songs (entries without a timestamp count as oldest, later playlist positions first). The outcome message states which rule applied.
- With Last.fm configured, each seed is queried for similar tracks (at most two seeds per primary artist). Scores from several seeds add up, so songs similar to multiple recent additions rank higher. Up to `max(30, 2 × discoveryLimit)` matches are resolved on Spotify; selection reasons name the recent addition.
- Artist affinity comes from the seeds rather than the whole source, so new releases and catalog tracks of recently added artists are secondary candidates. Selected artists, exclusion of known songs (saved library, all source playlists, destination, history), selection limits and append/replace behavior are the same as for the other taste sources.
- None of the source playlists may be the destination. Without a Last.fm key the run falls back to artist-based candidates and says so.

### Optional Last.fm configuration

Create a personal API application at https://www.last.fm/api/account/create (requires signing into Last.fm). Only the API key is needed for similar-track lookups, not the shared secret.

Configure either:

- `LASTFM_API_KEY`, or
- `LASTFM_API_KEY_FILE`: an absolute path to a key file containing one line, with permissions `600`. Suggested host location: `/home/vinci/.config/lastfm/api-key`.

Pi setup verified on 2026-10-30: the API key is stored at `/home/vinci/.config/lastfm/api-key` and the unused shared secret separately at `/home/vinci/.config/lastfm/shared-secret`, both mode `600` in a directory with mode `700`. The ignored local `.env` references the key file through `LASTFM_API_KEY_FILE`. A read-only Last.fm similar-track request accepted the key and returned three matches. Production activation followed on 2026-10-30 with explicit user authorization for a one-time Portainer API exception.

The environment key takes precedence. For container deployment, mount the file read-only and set the path as seen inside the container. Configure production stack 1 exclusively in the Portainer UI. Never put keys in this repository or logs.

Without a key, artist-based personal discovery works and the outcome explains that broader similar-artist discovery is unavailable. Temporary Last.fm errors also produce an explicit fallback message. Public-track similarity responses are cached for 24 hours in a bounded in-memory cache. No Spotify tokens are sent to Last.fm.

Spotify authorization requires `user-library-read` (including when a playlist is the taste source, so saved songs can be excluded), private-playlist reading and playlist-modification scopes. Existing sessions may need to sign in again to grant the library scope. Storing a new Spotify refresh grant invalidates the old cached access token so the next run uses the newly granted permissions. New playlist `/items` routes are used with a fallback to `/tracks` only for missing/unsupported routes, never for denied access.

The prepared Pi release and rollback procedure are documented in [the release notes](docs/discovery-release-20261030.md). Production activation completed on 2026-10-30; the release notes record the live checks and rollback.

### API and migrations

Task requests/responses include `tasteSource`, `favoritePlaylistId`, `sourcePlaylistIds`, `recentDays`, `discoveryLimit` and `replacePlaylist`; responses also expose `lastRunMessage` and `lastRunFailed`. Defaults preserve append behavior. Requests without source fields remain compatible. Supplying only a favorite-playlist ID infers `PLAYLIST`.

Migration V5 adds taste-source configuration. V6 adds limits, replacement opt-in, run outcomes, recording identity and selection reasons. V7 (2026-11-06) allows `RECENT_ADDITIONS`, adds `task.recent_days` and the `task_source_playlist` table (cascade-deleted with its task). Deploy this backend before the corresponding frontend. The Pi backend and matching frontend were activated on 2026-10-30 following explicit user authorization. All legacy tasks were deleted at the user's request before activation. Future changes to stack 1 remain subject to its Portainer UI policy.

### Verification

Validation on 2026-10-30: all 92 backend tests passed against a temporary PostgreSQL 14 database, including migration through V6, source/limit/history persistence, the legacy pipeline, local API contracts and failed/concurrent execution. The automated suite used simulated Spotify/Last.fm services; separate container verification uses the configured Last.fm key and simulated Spotify endpoints.

Validation on 2026-11-06 (recently added songs): all 100 backend tests passed (102 after the rolling 30-day window change) against a temporary PostgreSQL 14 database, including Hibernate schema validation through V7. A restored copy of the production database migrated from V6 to V7 with existing tasks and history unchanged. On the Pi the suite runs in `maven:3.9-eclipse-temurin-23` because no host JDK is installed.

JDK 23 is required. Run the full suite against an isolated test PostgreSQL database with `quarkus.datasource.devservices.enabled=false`, `quarkus.scheduler.enabled=false` and `quarkus.http.test-port=0`; never use the production database. HTTP contract tests use local simulated Spotify/Last.fm services, including pagination, unavailable tracks, matching, missing permissions, optional keys and error redaction.

References: [Spotify saved tracks](https://developer.spotify.com/documentation/web-api/reference/get-users-saved-tracks), [Last.fm similar tracks](https://www.last.fm/api/show/track.getSimilar).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/music-release-radar-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Running the application with Docker

### Step 1: Create a Docker network and configure the PostgreSQL container

A custom Docker network is required to enable communication between the PostgreSQL container and the application container. The PostgreSQL container needs to be connected to the same network as the application container, so they can communicate using container names instead of IP addresses.

This configuration allows the application container to resolve `postgres-db` to the PostgreSQL container within the same Docker network:
```properties
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-db:5432/music_release_radar_service_db
```

Create the Docker network:

```bash
docker network create music-net
```

Then, start the PostgreSQL container with the following command, ensuring it is connected to the `music-net` network:

```bash
docker run -d
    --name postgres-db
    --network music-net
    -e POSTGRES_USER=quarkus
    -e POSTGRES_PASSWORD=quarkus
    -e POSTGRES_DB=music_release_radar_service_db
    postgres:16
```

### Step 2: Build and run the application container

Build the application Docker image:

```bash
docker build -f src/main/docker/Dockerfile.jvm -t music-release-radar-service .
```

Start the application container, linking it to the `music-net` network:

```bash
docker run -d
    --name music-release-radar-service
    --network music-net
    -e QUARKUS_PROFILE=prod
    -p 8080:8080
    music-release-radar-service
```

### Step 3: Manage and verify containers

- verify running containers:
```bash
docker ps
```

- view logs of containers to ensure they started correctly:

```bash
docker logs <container_id>
```

- stop and remove the containers:

```bash
docker stop music-release-radar-service postgres-db
docker rm music-release-radar-service postgres-db
```

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Release

```bash
git push -f
mvn release:prepare
mvn release:perform
```

- To undo the prepare step in case of errors delete the commits and execute:
```bash
mvn release:clean
git tag -d {version}
git push origin :refs/tags/{version}
```
