drop view if exists view_utbetaling_beregning_manedsverk_fast_sats_admin;

alter table utbetaling_beregning_sats
    add periode daterange;

update utbetaling_beregning_sats
set periode = utbetaling.periode
from utbetaling
where utbetaling.id = utbetaling_id;

alter table utbetaling_beregning_sats
    alter periode set not null;


alter table utbetaling_beregning_sats
    drop constraint utbetaling_beregning_aft_pkey;

alter table utbetaling_beregning_sats
    rename to utbetaling_sats_periode;

alter table utbetaling_sats_periode
    add constraint utbetaling_sats_no_overlap exclude using gist (
        utbetaling_id with =,
        sats with =,
        periode with &&);
