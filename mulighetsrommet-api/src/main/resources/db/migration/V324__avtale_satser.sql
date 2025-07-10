alter type prismodell add value 'AVTALT_PRIS_PER_MANEDSVERK';
alter type prismodell rename value 'FRI' to 'ANNEN_AVTALT_PRIS';
alter type prismodell rename value 'FORHANDSGODKJENT' to 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK';

create table avtale_sats
(
    avtale_id uuid      not null references avtale (id) on delete cascade,
    periode   daterange not null,
    sats      int       not null,
    exclude using gist (avtale_id with =, periode with &&)
);
