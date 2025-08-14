drop view if exists gjennomforing_admin_dto_view;
drop view if exists utbetaling_dto_view;
drop view if exists avtale_admin_dto_view;

alter table gjennomforing
  alter column avbrutt_aarsak type text[]
  using case
    when avbrutt_aarsak is null then null
    else array[avbrutt_aarsak]
  end;

alter table gjennomforing
    rename column avbrutt_aarsak to avbrutt_aarsaker;

alter table gjennomforing
    add column avbrutt_forklaring text;

alter table avtale
  alter column avbrutt_aarsak type text[]
  using case
    when avbrutt_aarsak is null then null
    else array[avbrutt_aarsak]
  end;

alter table avtale
    rename column avbrutt_aarsak to avbrutt_aarsaker;

alter table avtale
    add column avbrutt_forklaring text;

alter type totrinnskontroll_type add value 'AVBRYT';
alter type utbetaling_status add value 'TIL_AVBRYTELSE';

alter table utbetaling
    drop column avbrutt_aarsaker,
    drop column avbrutt_forklaring,
    drop column avbrutt_tidspunkt;
