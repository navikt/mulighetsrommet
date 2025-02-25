create extension if not exists btree_gist;

create table tiltak_kontering_oebs
(
    id                     int generated always as identity,
    tiltakskode            text      not null,
    periode                daterange not null,
    statlig_regnskapskonto text      not null,
    statlig_artskonto      text      not null,

    exclude using gist(tiltakskode with =, periode with &&)
);
