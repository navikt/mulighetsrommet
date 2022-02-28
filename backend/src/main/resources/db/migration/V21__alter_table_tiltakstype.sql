-- Add new columns to table
ALTER TABLE tiltakstype
    ADD COLUMN sanity_id   INT,
    ADD COLUMN tiltakskode tiltakskode UNIQUE,
    ADD COLUMN dato_fra TIMESTAMP,
    ADD COLUMN dato_til TIMESTAMP,
    ADD COLUMN created_by TEXT,
    ADD COLUMN updated_by TEXT;

-- Change current table
ALTER TABLE tiltakstype
    RENAME COLUMN tittel TO navn;

ALTER TABLE tiltakstype
    DROP COLUMN beskrivelse,
    DROP COLUMN ingress,
    DROP COLUMN archived;
