alter table tilsagn
    rename antall_timer_oppfolging_per_deltaker to beregning_antall_timer_oppfolging_per_deltaker;

alter table tilsagn
    add beregning_antall_plasser int,
    add beregning_sats           int;

update tilsagn
set beregning_antall_plasser = antall_plasser,
    beregning_sats           = sats
from tilsagn_beregning_sats
where id = tilsagn_id;

drop table tilsagn_beregning_sats;

alter table tilsagn
    add beregning_prisbetingelser text;

update tilsagn
set beregning_prisbetingelser = prisbetingelser
from tilsagn_prisbetingelser
where id = tilsagn_id;

drop table tilsagn_prisbetingelser;
