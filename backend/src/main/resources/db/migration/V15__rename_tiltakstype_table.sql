ALTER TABLE tiltakstype
    RENAME TO tiltakstype;
ALTER TABLE tiltakstype
    RENAME CONSTRAINT tiltakstype_pkey TO tiltakstype_pkey;
ALTER TABLE tiltaksgjennomforing
    RENAME COLUMN tiltakstype_id to tiltakstype_id;

