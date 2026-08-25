-- Tilskuddsutbetalinger til bruker skal sendes ved besluttet tidspunkt, se kolonne besluttet_tidspunkt
alter table if exists bruker_utbetaling
    drop column if exists periode;
