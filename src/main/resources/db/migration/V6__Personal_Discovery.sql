ALTER TABLE task ADD COLUMN discovery_limit integer NOT NULL DEFAULT 20 CHECK (discovery_limit BETWEEN 1 AND 50);
ALTER TABLE task ADD COLUMN replace_playlist boolean NOT NULL DEFAULT false;
ALTER TABLE task ADD COLUMN last_run_message varchar(1000);
ALTER TABLE task ADD COLUMN last_run_failed boolean NOT NULL DEFAULT false;
ALTER TABLE added_item ADD COLUMN recording_key varchar(255);
ALTER TABLE added_item ADD COLUMN recording_isrc varchar(255);
ALTER TABLE added_item ADD COLUMN reason varchar(1000);
CREATE INDEX added_item_task_track_idx ON added_item (task_id, item_type, external_id);
