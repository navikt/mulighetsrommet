create type filter_dokument_type as enum ('Avtale', 'Tiltaksgjennomføring');

create table
    lagret_filter
(
    id         uuid primary key default gen_random_uuid(),
    bruker_id  text                           not null,
    navn       text                           not null,
    type       filter_dokument_type           not null,
    filter     jsonb                          not null,
    sort_order int                            not null,
    created_at timestamp        default now() not null,
    updated_at timestamp        default now() not null
)
