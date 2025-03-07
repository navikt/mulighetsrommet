create table utbetaling_stengt_hos_arrangor
(
    utbetaling_id uuid      not null references utbetaling (id),
    periode       daterange not null,
    beskrivelse   text      not null,
    exclude using gist(utbetaling_id with =, periode with &&)
);

alter table utbetaling_deltakelse_manedsverk
    alter manedsverk type numeric(6, 5)
