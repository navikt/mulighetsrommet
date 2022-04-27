ALTER TABLE tiltaksgjennomforing
RENAME COLUMN tittel to navn;

ALTER TABLE tiltaksgjennomforing
ADD COLUMN arrangor_id INT,
    ADD COLUMN arena_id INT,
    ADD COLUMN sak_id INT,
    ADD COLUMN sanity_id INT,
    ADD COLUMN created_by TEXT,
    ADD COLUMN updated_by TEXT;

ALTER TABLE tiltaksgjennomforing
DROP COLUMN beskrivelse;
