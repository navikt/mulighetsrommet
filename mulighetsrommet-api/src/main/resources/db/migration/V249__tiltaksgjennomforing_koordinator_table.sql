create table gjennomforing_koordinator
(
    nav_ident        text                    not null,
    gjennomforing_id uuid                    not null
        constraint fk_gjennomforing references gjennomforing (id) on delete cascade,
    created_at       timestamp default now() not null,
    updated_at       timestamp default now() not null,
    primary key (gjennomforing_id, nav_ident)
);
