drop view if exists view_utbetaling_linje;

alter table delutbetaling
    rename faktura_status_sist_oppdatert to faktura_status_endret_tidspunkt;

alter table delutbetaling
    rename sendt_til_okonomi_tidspunkt to faktura_sendt_tidspunkt;
