ALTER TABLE task DROP CONSTRAINT IF EXISTS task_taste_source_check;
ALTER TABLE task ADD CONSTRAINT task_taste_source_check
    CHECK (taste_source IN ('LIKED_SONGS', 'PLAYLIST', 'RECENT_ADDITIONS'));
ALTER TABLE task ADD COLUMN recent_days integer NOT NULL DEFAULT 7 CHECK (recent_days BETWEEN 1 AND 90);
CREATE TABLE task_source_playlist (
    task_id bigint NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    playlist_id varchar(255) NOT NULL,
    PRIMARY KEY (task_id, playlist_id)
);
