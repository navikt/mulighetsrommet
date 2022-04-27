CREATE TYPE deltakerstatus AS ENUM ('VENTER', 'DELTAR', 'AVSLUTTET');

CREATE TABLE deltaker
(
    id                      SERIAL PRIMARY KEY,
    tiltaksgjennomforing_id INT NOT NULL,
    person_id               INT NOT NULL,
    fra_dato       TIMESTAMP DEFAULT NULL,
    til_dato       TIMESTAMP DEFAULT NULL,
    status         deltakerstatus NOT NULL DEFAULT 'VENTER'
);
