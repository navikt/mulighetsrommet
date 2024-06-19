create type utdanningstype as enum ('FAGSKOLE', 'TILSKUDDSORDNING', 'VIDEREGAENDE', 'UNIVERSITET_OG_HOGSKOLE');

create type studieretning as enum ('YRKESFAG', 'STUDIEFORBEREDENDE');

create table utdanning
(
    id                            text primary key,
    utdanning_no_sammenligning_id text unique             not null,
    title                         text                    not null,
    description                   text,
    aktiv                         boolean                 not null,
    studieretning                 studieretning,
    utdanningstype                utdanningstype[]        not null,
    sokeord                       text[]                  not null,
    interesser                    text[]                  not null,
    created_at                    timestamp default now() not null,
    updated_at                    timestamp default now() not null
);

create trigger set_timestamp
    before update
    on utdanning
    for each row
execute procedure trigger_set_timestamp();

create table utdanning_nus_kode
(
    utdanning_id uuid not null references utdanning (id),
    nus_kode_id  uuid not null references utdanning_nus_kode_innhold (id)
);

create table utdanning_nus_kode_innhold
(
    id         uuid primary key default gen_random_uuid(),
    title      text                           not null,
    nus_kode   text unique                    not null,
    aktiv      boolean                        not null,
    created_at timestamp        default now() not null,
    updated_at timestamp        default now() not null
);

create trigger set_timestamp
    before update
    on utdanning_nus_kode
    for each row
execute procedure trigger_set_timestamp();

