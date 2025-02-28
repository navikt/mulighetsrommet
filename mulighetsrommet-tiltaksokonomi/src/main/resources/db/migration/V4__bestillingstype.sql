create type bestillingstype as enum ('TILTAK', 'INVESTERING');

alter table public.tiltak_kontering_oebs
    add column bestillingstype text not null default 'TILTAK';
