# Discovery release — 2026-10-30

Activated on 2026-10-30 through the Portainer API after the user explicitly granted a one-time stack 1 API exception and authorized restarting the backend. This does not change the general Portainer UI policy in `/home/vinci/AGENTS.md`. Existing Cloudflare routes were preserved.

## Verified artifacts

- Backend: `windschief/music-release-radar-service:discovery-20261030`.
- Frontend: `windschief/music-release-radar-frontend:discovery-20261030`.
- Backend: 92 passing tests and a successful JVM package.
- Frontend: 23 passing tests and a successful production build inside its standard Dockerfile.
- Three existing frontend Fast Refresh export-rule lint errors remain; targeted checks pass.
- An isolated PostgreSQL 14 restored from the production backup migrated from V4 through V6. Original rows remained unchanged in that test copy.
- Real Last.fm similarity was exercised through the candidate application and mounted key file. All Spotify calls and playlist writes were simulated.
- Both taste sources, renewed Spotify permissions, bounded selections, repeat filtering, selection reasons, empty-selection protection and frontend routes passed.
- All temporary verification containers and their network were removed.

## Intentional task reset

On 2026-10-30 the user explicitly requested starting over. After a separate database backup, the five production tasks, their 188 task items and 5,302 history entries were deleted transactionally. Zero tasks remain. Spotify token rows were verified unchanged; no Spotify playlists were modified. The production schema reached V6 during activation.

Protected release records and backups are under `/home/vinci/.local/state/music-release-radar/releases/discovery-20261030T082507Z/`. Files include `manifest.json`, `activation-plan.json`, `before-discovery.dump`, `before-task-reset.dump` and `task-reset.json`. These contain private operational state and must remain outside Git and the knowledge vault.

## Applied activation configuration

Apply the backend first, then the frontend, using the permitted Portainer interface. Preserve all existing environment values except the explicit overrides below. Preserve PostgreSQL, its volume, networks, DNS, ports and Cloudflare routes. Keep `restart: unless-stopped` and disable Watchtower for these local candidate tags. Use local images without pulling or pruning.

Merge this fragment into the existing backend service; it is not a replacement stack:

```yaml
user: "1000:1000"
volumes:
  - /home/vinci/.config/lastfm/api-key:/run/secrets/lastfm-api-key:ro
environment:
  LASTFM_API_KEY_FILE: /run/secrets/lastfm-api-key
  QUARKUS_SCHEDULER_ENABLED: "true"
  JAVA_TOOL_OPTIONS: "-Xms32m -Xmx384m -XX:ActiveProcessorCount=2 -Dquarkus.http.host=0.0.0.0"
```

UID/GID 1000 allows reading the key without loosening its mode 600. The shared secret is not mounted. Preserve real production Spotify configuration; never deploy simulated endpoints or credentials from verification.

Verification containers disabled scheduling to isolate execution. Production can use normal scheduling because all legacy tasks have now been deleted. New tasks run according to their chosen schedule. Recheck zero tasks and the paused backend before activation.

After activation, verify V6, API authentication/CORS, frontend routes and Spotify sign-in. A fresh sign-in may be needed for saved-library permissions. Configure a taste source and a separate destination in the UI. A real-account discovery run has not yet been performed.

## Rollback

Original image IDs are retained in the protected manifest. The old backend successfully started against the migrated V6 database copy with these rollback settings:

```properties
QUARKUS_FLYWAY_IGNORE_MIGRATION_PATTERNS=versioned:future
QUARKUS_SCHEDULER_ENABLED=false
```

Old Flyway cannot otherwise validate V5/V6, which that image does not contain. Keep scheduling disabled on rollback so old code cannot process new discovery tasks using legacy behavior. Do not repair Flyway history or downgrade the schema.

A database restore is not the normal rollback: both backups include intentionally deleted tasks. Restore only after a separate explicit decision.


## Production checks after activation

Backend and frontend run the verified candidate image IDs with zero restarts at verification. Schema V6, zero tasks, the read-only Last.fm key mount, normal scheduling and Watchtower exclusion were confirmed.

The new frontend assets are identical locally and at https://music.windschief.cc. Login, callback, new-task and privacy routes return HTML. The local/public API returns 401 without authentication and permits the music frontend's CORS preflight. Cloudflare blocks the default Python user agent with error 1010; the public checks passed with a browser user agent. No Cloudflare setting was changed.

Portainer recreated the PostgreSQL container during the backend stack update despite an unchanged database service definition. Its existing data volume was preserved. Spotify token rows were compared exactly with the protected pre-reset backup and remained unchanged. The initial container-ID equality check was therefore replaced with checks of the data volume, schema and token rows.

No actual user-account recommendation run or Spotify playlist write was performed during activation. Users should sign in again for the saved-library permission before creating their first discovery task.
