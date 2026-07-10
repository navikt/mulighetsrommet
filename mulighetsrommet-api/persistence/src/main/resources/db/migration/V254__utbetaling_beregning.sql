drop view if exists avtale_admin_dto_view;
drop view if exists utbetaling_aft_view;
drop view if exists utbetaling_dto_view;

alter type avtale_prismodell
    rename to prismodell;

alter table utbetaling
    rename beregningsmodell to prismodell;
alter table utbetaling
    alter prismodell type prismodell using prismodell::text::prismodell;

alter table utbetaling_beregning_aft
    rename to utbetaling_beregning_forhandsgodkjent;

alter table utbetaling_beregning_forhandsgodkjent
    drop periode;
