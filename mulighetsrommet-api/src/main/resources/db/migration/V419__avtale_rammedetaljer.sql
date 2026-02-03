create table avtale_rammedetaljer
(
    avtale_id      uuid     primary key references avtale (id),
    valuta         currency not null,
    total_ramme    bigint   not null,
    utbetalt_arena bigint   null
);
