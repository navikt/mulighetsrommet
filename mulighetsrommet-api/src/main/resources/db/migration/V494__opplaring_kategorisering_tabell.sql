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

insert into opplaring_innhold_element (id, kode, navn)
values ('312a02cd-8330-4c32-ae4b-8e9cde0060fb', 'GRUNNLEGGENDE_FERDIGHETER', 'Grunnleggende ferdigheter'),
       ('0770648f-210e-4b2e-9524-0b87226c8f4b', 'TEORETISK_OPPLAERING', 'Teoretisk opplæring'),
       ('7ee66328-2dd0-4180-9bb3-165e7854f3b6', 'JOBBSOKER_KOMPETANSE', 'Jobbsøkerkompetanse'),
       ('4264da96-3b68-426a-8629-37a2fafafa27', 'PRAKSIS', 'Praksis'),
       ('8f729892-dbb0-448e-ac4c-7169464f955c', 'ARBEIDSMARKEDSKUNNSKAP', 'Arbeidsmarkedskunnskap'),
       ('c645e920-618c-4dcf-a3b7-516f32040e04', 'NORSKOPPLAERING', 'Norskopplæring'),
       ('331d61f7-c957-4d9c-a229-41d1d1b9c675', 'BRANSJERETTET_OPPLARING', 'Bransjerettet opplæring');

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
