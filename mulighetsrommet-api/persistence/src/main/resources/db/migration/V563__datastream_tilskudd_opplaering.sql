-- Legger til datastream spesifikke kolonner, da data range ikke støttes, og heller ikke genererte kolonner
alter table tilskudd_vedtak
    add column datastream_periode_start date,
    add column datastream_periode_slutt date;

update tilskudd_vedtak set datastream_periode_start = lower(periode),
                   datastream_periode_slutt = (upper(periode) - INTERVAL '1 day')::DATE;
