ALTER TABLE tiltaksinstans
    RENAME TO tiltaksgjennomforing;
ALTER TABLE tiltaksgjennomforing
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
