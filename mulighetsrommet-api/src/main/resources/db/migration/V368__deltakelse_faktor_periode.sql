drop view if exists view_utbetaling_beregning_manedsverk_fast_sats_admin;
drop view if exists view_utbetaling_beregning_manedsverk;
drop view if exists view_utbetaling_beregning_pris_per_time_oppfolging;
drop view if exists view_utbetaling_beregning_ukesverk;

alter table utbetaling_deltakelse_faktor
    add periode daterange;
update utbetaling_deltakelse_faktor f
set periode = utbetaling_deltakelse_input.merged_periode
from (select utbetaling_id,
             deltakelse_id,
             range_merge(range_agg(periode)) as merged_periode
      from utbetaling_deltakelse_periode
      group by utbetaling_id, deltakelse_id) utbetaling_deltakelse_input
where f.utbetaling_id = utbetaling_deltakelse_input.utbetaling_id
  and f.deltakelse_id = utbetaling_deltakelse_input.deltakelse_id;

alter table utbetaling_deltakelse_faktor
    alter periode set not null;

alter table utbetaling_deltakelse_faktor
    drop constraint utbetaling_deltakelse_mane_refusjonskrav_id_deltakelse_i_key;

alter table utbetaling_deltakelse_faktor
    add constraint utbetaling_deltakelse_faktor_no_overlap exclude using gist (
        utbetaling_id with =,
        deltakelse_id with =,
        periode with &&);
