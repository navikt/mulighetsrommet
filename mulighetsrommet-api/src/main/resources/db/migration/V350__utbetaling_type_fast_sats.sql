drop view if exists view_utbetaling_beregning_manedsverk_med_deltakelsesmengder;
drop view if exists utbetaling_dto_view;

create type utbetaling_beregning_type_new as enum ('FRI', 'PRIS_PER_MANEDSVERK', 'PRIS_PER_UKESVERK', 'FAST_SATS_PER_TILTAKSPLASS_PER_MANED');

alter table utbetaling
    alter column beregning_type type utbetaling_beregning_type_new
        using case beregning_type
                  when 'PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER' then 'FAST_SATS_PER_TILTAKSPLASS_PER_MANED'
                  else beregning_type::text::utbetaling_beregning_type_new
        end;

drop type utbetaling_beregning_type;

alter type utbetaling_beregning_type_new rename to utbetaling_beregning_type;
