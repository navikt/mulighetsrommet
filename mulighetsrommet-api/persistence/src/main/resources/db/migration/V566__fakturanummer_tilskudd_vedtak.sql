drop view if exists view_utbetaling;
drop view if exists view_arrangorflate_utbetaling;

alter table utbetaling
    rename column kid to betalingsreferanse_kid;

alter table utbetaling
    add column betalingsreferanse_melding text;


