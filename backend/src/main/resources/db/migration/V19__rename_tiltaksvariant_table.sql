ALTER TABLE tiltaksvariant
    RENAME TO tiltakstype;
ALTER TABLE tiltakstype
    RENAME CONSTRAINT tiltaksvariant_pkey TO tiltakstype_pkey;
ALTER TABLE tiltaksgjennomforing
    RENAME COLUMN tiltaksvariant_id to tiltakstype_id;
ALTER SEQUENCE tiltaksvariant_id_seq RENAME TO tiltakstype_id_seq;
