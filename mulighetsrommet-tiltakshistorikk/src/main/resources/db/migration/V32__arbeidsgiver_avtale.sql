create table arbeidsgiver_avtale
(
    avtale_id            uuid primary key,
    norsk_ident          text             not null,
    organisasjonsnummer  text             not null,
    tiltakstype          text             not null,
    start_dato           date,
    slutt_dato           date,
    status               text             not null,
    stillingsprosent     double precision,
    dager_per_uke        double precision,
    opprettet_tidspunkt  timestamptz      not null,
    oppdatert_tidspunkt  timestamptz      not null
);

create index arbeidsgiver_avtale_norsk_ident_idx on arbeidsgiver_avtale (norsk_ident);
