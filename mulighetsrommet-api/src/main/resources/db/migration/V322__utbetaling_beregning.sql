drop view if exists view_utbetaling_beregning_forhandsgodkjent;

alter table utbetaling
    add column belop_beregnet int;

update utbetaling
set belop_beregnet = beregning.belop
from (select utbetaling_id, belop
      from utbetaling_beregning_fri
      union all
      select utbetaling_id, belop
      from utbetaling_beregning_forhandsgodkjent) as beregning
where beregning.utbetaling_id = utbetaling.id;

alter table utbetaling
    alter column belop_beregnet set not null;

drop table utbetaling_beregning_fri;

alter table utbetaling_beregning_forhandsgodkjent
    drop belop;
