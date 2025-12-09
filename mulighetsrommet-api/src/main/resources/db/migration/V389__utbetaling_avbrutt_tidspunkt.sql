drop view if exists view_utbetaling;

alter type utbetaling_status add value 'AVBRUTT';
alter table utbetaling add avbrutt_tidspunkt timestamptz;
alter table utbetaling add avbrutt_begrunnelse text;
