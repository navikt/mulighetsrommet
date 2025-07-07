alter table tilsagn_forhandsgodkjent_beregning
    rename to tilsagn_beregning_sats;

drop view if exists view_utbetaling_beregning_forhandsgodkjent;
drop view if exists view_utbetaling_beregning_ukesverk;

alter table utbetaling_beregning_forhandsgodkjent
    rename to utbetaling_beregning_sats;
