drop view if exists view_utbetaling;
drop view if exists view_utbetaling_output_deltakelse_perioder_json;
drop view if exists view_utbetaling_input_satser_json;
drop view if exists view_utbetaling_linje;

alter table utbetaling
    add column valuta currency not null default 'NOK';
alter table utbetaling
    alter valuta drop default;

alter table utbetaling_deltakelse_faktor
    add column valuta currency not null default 'NOK';
alter table utbetaling_deltakelse_faktor
    alter valuta drop default;

alter table utbetaling_sats_periode
    add column valuta currency not null default 'NOK';
alter table utbetaling_sats_periode
    alter valuta drop default;

alter table delutbetaling
    add column valuta currency not null default 'NOK';
alter table delutbetaling
    alter valuta drop default;
