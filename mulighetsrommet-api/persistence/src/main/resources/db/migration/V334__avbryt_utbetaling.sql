drop view if exists utbetaling_dto_view;

alter table utbetaling
    add column avbrutt_aarsaker text[],
    add column avbrutt_forklaring text,
    add column avbrutt_tidspunkt timestamp with time zone;
