ALTER TABLE tiltaksgjennomforing
    ADD CONSTRAINT arena_id_unique UNIQUE (arena_id);

CREATE TYPE deltakerstatus AS ENUM ('IKKE_AKTUELL', 'VENTER', 'DELTAR', 'AVSLUTTET');

CREATE TABLE deltaker
(
    id                      SERIAL PRIMARY KEY,
    arena_id                INT UNIQUE     NOT NULL,
    tiltaksgjennomforing_id INT            NOT NULL,
    person_id               INT            NOT NULL,
    fra_dato                TIMESTAMP DEFAULT NULL,
    til_dato                TIMESTAMP DEFAULT NULL,
    status                  deltakerstatus NOT NULL,
    CONSTRAINT fk_tiltaksgjennomforing FOREIGN KEY (tiltaksgjennomforing_id) REFERENCES tiltaksgjennomforing (arena_id)
);
