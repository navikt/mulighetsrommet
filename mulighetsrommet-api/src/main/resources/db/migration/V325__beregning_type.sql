drop view if exists tilsagn_admin_dto_view;
drop view if exists utbetaling_dto_view;

create type tilsagn_beregning_type as enum (
    'FRI',
    'AVTALT_PRIS_PER_MANEDSVERK');

alter table tilsagn
    add column beregning_type tilsagn_beregning_type;

update tilsagn
set beregning_type = case
                         when prismodell = 'ANNEN_AVTALT_PRIS' then 'FRI'
                         when prismodell = 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK' then 'AVTALT_PRIS_PER_MANEDSVERK'
    end::tilsagn_beregning_type;

alter table tilsagn
    alter column beregning_type set not null,
    drop column prismodell;

create type utbetaling_beregning_type as enum (
    'FRI',
    'AVTALT_PRIS_PER_MANEDSVERK');

alter table utbetaling
    add column beregning_type utbetaling_beregning_type;

update utbetaling
set beregning_type = case
                         when prismodell = 'ANNEN_AVTALT_PRIS' then 'FRI'
                         when prismodell = 'FORHANDSGODKJENT_PRIS_PER_MANEDSVERK' then 'AVTALT_PRIS_PER_MANEDSVERK'
    end::utbetaling_beregning_type;

alter table utbetaling
    alter column beregning_type set not null,
    drop column prismodell;
