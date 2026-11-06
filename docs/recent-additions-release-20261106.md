# Recently-added-songs release — 2026-11-06

Activated on 2026-11-06 through the Portainer API after the user explicitly granted a one-time API exception for this deployment. The general Portainer UI policy for stack 1 in `/home/vinci/AGENTS.md` is unchanged.

## Artifacts

- Backend: `windschief/music-release-radar-service:recent-additions-20261106` (stack 1), migration V7.
- Frontend: `windschief/music-release-radar-frontend:recent-additions-20261106` (stack 10).
- 100 backend tests, 25 frontend tests (also run inside the frontend image build), typecheck and targeted lint passed. A restored copy of the production database migrated V6 → V7 before activation.

## Procedure

Only the image tag of each service was replaced in the stored stack file; stack variables were sent back unchanged, `PullImage: false`, `Prune: false`. Backend first, frontend second. Portainer again recreated `postgres-db`; its data volume was preserved.

Protected records (database dump before deployment, stack files before the change, public frontend build values) are under `/home/vinci/.local/state/music-release-radar/build-20261106/`. They contain private operational state and must stay outside Git and the knowledge vault.

## Checks after activation

Schema V7; both existing tasks, 88 history entries and both Spotify token rows unchanged. Backend zero restarts, user `1000:1000`, read-only Last.fm key mount, DNS override, `unless-stopped` and Watchtower exclusion preserved for both containers. Public and local frontend serve the same new bundle; login/callback/new-task routes return 200; the API returns 401 without authentication locally and publicly and allows the music frontend's CORS preflight. No real Spotify discovery run was triggered.

## Rollback

Put the `discovery-20261030` tags back (backend and frontend). V7 is additive; the old backend validates it only with `QUARKUS_FLYWAY_IGNORE_MIGRATION_PATTERNS=versioned:future`, and should then run with scheduling disabled if a `RECENT_ADDITIONS` task exists, because the old enum cannot load it. Do not downgrade the schema.

## Follow-up: rolling 30-day release window (2026-11-06)

After a second explicit one-time Portainer API exception, both stacks were switched from `recent-additions-20261106` to `rolling-window-20261106` (backend first) using the same tag-only procedure. No migration; `postgres-db` was not recreated this time. Verified: schema V7, three tasks and both Spotify token rows present, backend zero restarts, user/DNS/read-only Last.fm mount/`unless-stopped`/Watchtower exclusion preserved, public bundle equals local and contains the new hint, API 401 without authentication and CORS preflight allowed. Pre-deployment dump and stack files: `before-rolling-window.dump`, `stack-1-before-rolling.json`, `stack-10-before-rolling.json` in the protected build directory. Rollback: restore the `recent-additions-20261106` tags.
