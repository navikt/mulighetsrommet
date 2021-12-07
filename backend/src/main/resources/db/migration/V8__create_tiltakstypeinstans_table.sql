CREATE TABLE tiltaksinstans
(
    id             SERIAL PRIMARY KEY,
    tiltakstype_id INT,
    tittel         TEXT NOT NULL,
    beskrivelse    TEXT NOT NULL,
    tiltaksnummer  INT,
    fra_dato       TIMESTAMP DEFAULT NULL,
    til_dato       TIMESTAMP DEFAULT NULL,
    CONSTRAINT fk_tiltakstype FOREIGN KEY (tiltakstype_id) REFERENCES Tiltakstype (id)
)