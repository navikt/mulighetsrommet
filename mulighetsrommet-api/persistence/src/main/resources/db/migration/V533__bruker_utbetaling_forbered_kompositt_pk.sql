-- Gjør behandling til int for fremtidig endring til kompositt nøkkel i bruker_utbetaling

drop view if exists view_tilskudd_behandling;

alter table bruker_utbetaling
    alter column behandling_id type integer using behandling_id::integer;

alter table tilskudd
    add column bruker_utbetaling_behandling_id integer,
    drop constraint tilskudd_bruker_utbetaling_id_fkey;

update tilskudd t
set bruker_utbetaling_behandling_id = bu.behandling_id
from bruker_utbetaling bu
where t.bruker_utbetaling_id = bu.id
  and t.bruker_utbetaling_behandling_id is null;
