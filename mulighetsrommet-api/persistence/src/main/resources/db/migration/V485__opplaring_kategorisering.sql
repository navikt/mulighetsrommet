create table opplaring_kategorisering_bransje
(
    id         uuid primary key not null,
    kode       text             not null,
    navn       text             not null,
    created_at timestamptz      NOT NULL DEFAULT now(),
    updated_at timestamptz      NOT NULL DEFAULT now(),
    constraint uc_opplaring_kategorisering_bransje_kode unique (kode)
);

create trigger set_timestamp
    before update
    on opplaring_kategorisering_bransje
    for each row
execute procedure trigger_set_timestamp();

create table opplaring_kategorisering_kurstype
(
    id         uuid primary key not null,
    kode       text             not null,
    navn       text             not null,
    aktiv      boolean          not null default true,
    created_at timestamptz      NOT NULL DEFAULT now(),
    updated_at timestamptz      NOT NULL DEFAULT now(),
    constraint uc_opplaring_kategorisering_kurstype_kode unique (kode)
);

create trigger set_timestamp
    before update
    on opplaring_kategorisering_kurstype
    for each row
execute procedure trigger_set_timestamp();

create table opplaring_kategorisering_forerkort
(
    id         uuid primary key not null,
    kode       text             not null,
    navn       text             not null,
    created_at timestamptz      NOT NULL DEFAULT now(),
    updated_at timestamptz      NOT NULL DEFAULT now(),
    constraint uc_opplaring_kategorisering_forerkort_kode unique (kode)
);

create trigger set_timestamp
    before update
    on opplaring_kategorisering_forerkort
    for each row
execute procedure trigger_set_timestamp();
