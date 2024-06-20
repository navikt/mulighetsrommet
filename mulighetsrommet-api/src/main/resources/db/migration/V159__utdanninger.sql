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

create table utdanning_nus_kode_innhold
(
    nus_kode   text primary key,
    title      text                    not null,
    aktiv      boolean                 not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create trigger set_timestamp
    before update
    on utdanning_nus_kode_innhold
    for each row
execute procedure trigger_set_timestamp();

create table utdanning_nus_kode
(
    utdanning_id text not null references utdanning (id),
    nus_kode_id  text not null references utdanning_nus_kode_innhold (nus_kode)
);


