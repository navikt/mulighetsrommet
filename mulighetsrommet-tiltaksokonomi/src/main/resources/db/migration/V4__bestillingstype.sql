create type tilskuddstype as enum ('TILTAK_DRIFTSTILSKUDD', 'TILTAK_INVESTERINGER');

alter table tiltak_kontering_oebs
    add column tilskuddstype text not null default 'TILTAK_DRIFTSTILSKUDD';

alter table tiltak_kontering_oebs
    alter column tilskuddstype drop default;

alter table tiltak_kontering_oebs
    drop constraint tiltak_kontering_oebs_tiltakskode_periode_excl;

alter table tiltak_kontering_oebs
    add constraint tiltak_kontering_oebs_periode_excl
        exclude using gist (tilskuddstype with =, tiltakskode with =, periode with &&);
