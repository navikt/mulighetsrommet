drop view if exists view_utbetaling_linje;

alter table delutbetaling
    rename faktura_status_sist_oppdatert to faktura_status_endret_tidspunkt;
