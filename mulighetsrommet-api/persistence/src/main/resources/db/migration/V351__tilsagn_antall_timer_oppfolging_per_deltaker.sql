alter type tilsagn_beregning_type add value 'PRIS_PER_TIME_OPPFOLGING';

alter table tilsagn
    add antall_timer_oppfolging_per_deltaker int;
