drop table gjennomforing_koordinator;

create table gjennomforing_koordinator
(
    id               uuid primary key        not null,
    nav_ident        text                    not null,
    gjennomforing_id uuid                    not null
        constraint fk_gjennomforing references gjennomforing (id) on delete cascade,
    created_at       timestamp default now() not null,
    updated_at       timestamp default now() not null,
    constraint unique_nav_ident_gjennomforing_id unique (nav_ident, gjennomforing_id)
);
