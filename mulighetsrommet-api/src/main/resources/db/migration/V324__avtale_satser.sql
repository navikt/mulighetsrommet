alter type prismodell add value 'AVTALT_SATS_PER_MANED';

create table avtale_sats
(
    avtale_id uuid      not null references avtale (id) on delete cascade,
    periode   daterange not null,
    sats      int       not null,
    exclude using gist (avtale_id with =, periode with &&)
);
