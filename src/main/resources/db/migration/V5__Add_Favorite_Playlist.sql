ALTER TABLE task ADD COLUMN favorite_playlist_id varchar(255);
ALTER TABLE task ADD COLUMN taste_source varchar(255)
    CHECK (taste_source IN ('LIKED_SONGS', 'PLAYLIST'));
