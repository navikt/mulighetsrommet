create type bestillingstype as enum ('TILTAK', 'INVESTERING');

alter table tiltak_kontering_oebs
    add column bestillingstype text not null default 'TILTAK';

alter table tiltak_kontering_oebs
    alter column bestillingstype drop default;

alter table tiltak_kontering_oebs
    drop constraint tiltak_kontering_oebs_tiltakskode_periode_excl;

alter table tiltak_kontering_oebs
    add constraint tiltak_kontering_oebs_periode_excl
        exclude using gist (bestillingstype with =, tiltakskode with =, periode with &&);
