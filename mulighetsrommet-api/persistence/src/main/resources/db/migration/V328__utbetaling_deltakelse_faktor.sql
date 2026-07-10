drop view if exists view_utbetaling_beregning_manedsverk;
drop view if exists view_utbetaling_beregning_ukesverk;

alter table utbetaling_deltakelse_manedsverk
    rename to utbetaling_deltakelse_faktor;

alter table utbetaling_deltakelse_faktor
    rename manedsverk to faktor;

insert into utbetaling_deltakelse_faktor (utbetaling_id, deltakelse_id, faktor)
select utbetaling_id, deltakelse_id, ukesverk as faktor
from utbetaling_deltakelse_ukesverk;

drop table utbetaling_deltakelse_ukesverk;

