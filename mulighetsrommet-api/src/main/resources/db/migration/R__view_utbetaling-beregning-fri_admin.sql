-- ${flyway:timestamp}

drop view if exists view_utbetaling_beregning_fri;

create view view_utbetaling_beregning_fri as
select utbetaling.id,
       utbetaling.periode,
       utbetaling.belop_beregnet
from utbetaling
