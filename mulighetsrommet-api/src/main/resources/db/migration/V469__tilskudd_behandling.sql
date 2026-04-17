CREATE TABLE tilskudd_type (
    value TEXT NOT NULL PRIMARY KEY
);

INSERT INTO tilskudd_type (value) VALUES
    ('SKOLEPENGER'),
    ('SEMESTERAVGIFT'),
    ('EKSAMENSGEBYR');

CREATE TABLE vedtak_resultat (
    value TEXT NOT NULL PRIMARY KEY
);

INSERT INTO vedtak_resultat (value) VALUES
    ('INNVILGELSE'),
    ('AVSLAG');

CREATE TABLE tilskudd_behandling (
    id uuid PRIMARY KEY,
    gjennomforing_id uuid NOT NULL REFERENCES gjennomforing(id),
    soknad_journalpost_id TEXT NOT NULL,
    soknad_dato DATE NOT NULL,
    periode DATERANGE NOT NULL,
    kostnadssted TEXT NOT NULL
);

CREATE TABLE tilskudd_vedtak (
    id uuid PRIMARY KEY,
    tilskudd_behandling_id uuid NOT NULL REFERENCES tilskudd_behandling(id),
    tilskudd_type TEXT NOT NULL REFERENCES tilskudd_type(value),
    soknad_belop INT NOT NULL,
    soknad_valuta currency NOT NULL,
    vedtak_resultat TEXT NOT NULL REFERENCES vedtak_resultat(value),
    kommentar_vedtaksbrev TEXT,
    utbetaling_mottaker TEXT NOT NULL
);
