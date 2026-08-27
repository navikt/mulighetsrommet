-- Tilskuddsutbetalinger til bruker skal sendes ved besluttet tidspunkt, se kolonne besluttet_tidspunkt
alter table if exists bruker_utbetaling
    drop column if exists periode,
    add column if not exists transaksjon_dato date;

update bruker_utbetaling
set transaksjon_dato = besluttet_tidspunkt::date
where transaksjon_dato is null;

alter table if exists bruker_utbetaling
    alter column transaksjon_dato set not null;
