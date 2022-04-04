CREATE TABLE tiltak(
    id              SERIAL PRIMARY KEY,
    tittel          TEXT NOT NULL,
    beskrivelse     TEXT NOT NULL,
    tiltaks_type    TEXT NOT NULL,
    ingress         TEXT NOT NULL,
    kategori        TEXT NOT NULL,
    kommune_ids     INT[] NOT NULL
)