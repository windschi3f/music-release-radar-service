BEGIN;

-- 1. Create a backup of the task table's check_from column
ALTER TABLE task ADD COLUMN check_from_old timestamp with time zone;
UPDATE task SET check_from_old = check_from;

-- 2. Convert check_from to date, keeping only the date part
ALTER TABLE task ALTER COLUMN check_from TYPE date USING check_from::date;

-- 3. Create a backup of added_item's external_id
ALTER TABLE added_item ADD COLUMN external_id_old varchar;
UPDATE added_item SET external_id_old = external_id;

-- 4. Extract the last part of the external_id (after the last colon)
UPDATE added_item 
    SET external_id = regexp_replace(external_id, '^.*:', '')
    WHERE external_id LIKE '%:%:%';

-- 5. Add and initialize item_type column for TRACK
ALTER TABLE added_item ADD COLUMN item_type smallint;
UPDATE added_item SET item_type = 1;

-- 6. Drop the backup columns
ALTER TABLE task DROP COLUMN check_from_old;
ALTER TABLE added_item DROP COLUMN external_id_old;

COMMIT;
