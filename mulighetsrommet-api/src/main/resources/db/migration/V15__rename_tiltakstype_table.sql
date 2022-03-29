ALTER TABLE tiltakstype
    RENAME TO tiltaksvariant;
ALTER TABLE tiltaksvariant
    RENAME CONSTRAINT tiltakstype_pkey TO tiltaksvariant_pkey;
ALTER TABLE tiltaksgjennomforing
    RENAME COLUMN tiltakstype_id to tiltaksvariant_id;
