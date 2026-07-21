drop view if exists view_avtale;
drop view if exists view_gjennomforing_avtale_detaljer;
drop view if exists view_gjennomforing_opplaring_kategorisering;
drop view if exists view_avtale_opplaring_kategorisering;

create table opplaring_kategorisering
(
    id                   uuid        not null primary key,
    kurstype_id          uuid        references opplaring_kategorisering_kurstype (id) on delete set null,
    bransje_id           uuid        references opplaring_kategorisering_bransje (id) on delete set null,
    utdanningsprogram_id uuid        references utdanningsprogram (id) on delete set null,
    norskprove           boolean,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now()
);

create trigger set_timestamp
    before update
    on opplaring_kategorisering
    for each row
execute procedure trigger_set_timestamp();

create table opplaring_innhold_element
(
    id         uuid primary key not null,
    kode       text             not null,
    navn       text             not null,
    created_at timestamptz      not null default now(),
    updated_at timestamptz      not null default now(),
    constraint uc_opplaring_innhold_element_kode unique (kode)
);

create trigger set_timestamp
    before update
    on opplaring_innhold_element
    for each row
execute procedure trigger_set_timestamp();

create table opplaring_kategorisering_innhold_element
(
    opplaring_kategorisering_id uuid not null references opplaring_kategorisering (id) on delete cascade,
    innhold_element_id          uuid not null references opplaring_innhold_element (id) on delete cascade,
    primary key (opplaring_kategorisering_id, innhold_element_id)
);

alter table if exists opplaring_kategorisering_forerkort
    rename to opplaring_forerkort;

create table opplaring_kategorisering_forerkort
(
    opplaring_kategorisering_id uuid not null references opplaring_kategorisering (id) on delete cascade,
    forerkort_id                uuid not null references opplaring_forerkort (id) on delete cascade,
    primary key (opplaring_kategorisering_id, forerkort_id)
);

create table opplaring_kategorisering_sertifisering
(
    opplaring_kategorisering_id uuid not null references opplaring_kategorisering (id) on delete cascade,
    konsept_id                  int  not null references amo_sertifisering (konsept_id) on delete cascade,
    primary key (opplaring_kategorisering_id, konsept_id)
);

create table opplaring_kategorisering_utdanning
(
    opplaring_kategorisering_id uuid not null references opplaring_kategorisering (id) on delete cascade,
    utdanning_id                uuid not null references utdanning (id),
    primary key (opplaring_kategorisering_id, utdanning_id)
);
