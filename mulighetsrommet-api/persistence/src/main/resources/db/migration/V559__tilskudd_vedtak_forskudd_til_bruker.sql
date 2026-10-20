drop view if exists view_tilskudd_vedtak;
drop view if exists view_tilskudd_behandling;

create table bruker_utbetaling_type (
    value text primary key
);

insert into bruker_utbetaling_type (value) values ('FORSKUDD'), ('REFUSJON');

alter table tilskudd_vedtak add column bruker_utbetaling_type text references bruker_utbetaling_type(value);

update tilskudd_vedtak
    set bruker_utbetaling_type = 'REFUSJON'
where utbetaling_mottaker = 'BRUKER';

