alter table utbetaling_deltakelse_faktor
    add sats int;

update utbetaling_deltakelse_faktor
set sats = utbetaling_sats_periode.sats
from utbetaling_sats_periode
where utbetaling_sats_periode.utbetaling_id = utbetaling_deltakelse_faktor.utbetaling_id;

alter table utbetaling_deltakelse_faktor
    alter sats set not null;
