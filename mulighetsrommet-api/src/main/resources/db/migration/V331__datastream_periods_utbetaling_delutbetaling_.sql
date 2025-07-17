-- Legger til datastream spesifikke kolonner, da data range ikke støttes, og heller ikke genererte/trigger endret kolonner
alter table utbetaling
    add column datastream_periode_start date,
    add column datastream_periode_slutt date;

update utbetaling set datastream_periode_start = lower(periode),
                   datastream_periode_slutt = (upper(periode) - INTERVAL '1 day')::DATE;

alter table delutbetaling
    add column datastream_periode_start date,
    add column datastream_periode_slutt date;

update delutbetaling set datastream_periode_start = lower(periode),
                      datastream_periode_slutt = (upper(periode) - INTERVAL '1 day')::DATE;



