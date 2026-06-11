create table deltaker_deltakelsesmengde
(
    deltaker_id         uuid          not null references deltaker (id),
    gyldig_fra          date          not null,
    opprettet_tidspunkt timestamptz   not null,
    deltakelsesprosent  numeric(5, 2) not null,
    primary key (deltaker_id, gyldig_fra)
);
