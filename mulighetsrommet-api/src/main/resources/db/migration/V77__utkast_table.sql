create type utkasttype as enum ('Avtale', 'Tiltaksgjennomforing');

create table utkast
(
    id uuid primary key,
    bruker text references nav_ansatt(nav_ident) on delete cascade,
    utkast_data jsonb not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    utkast_type utkasttype not null
)
