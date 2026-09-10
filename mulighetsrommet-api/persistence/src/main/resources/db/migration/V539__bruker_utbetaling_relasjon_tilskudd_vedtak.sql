drop view if exists view_tilskudd_behandling;


--
-- Tilskudd vedtak på bruker_utbetaling
--
alter table bruker_utbetaling
    add column tilskudd_vedtak_id uuid;

update bruker_utbetaling bu
    set tilskudd_vedtak_id = tv.id
from tilskudd_vedtak tv
    inner join tilskudd_vedtak_bruker_utbetaling tvbu on tv.id = tvbu.tilskudd_vedtak_id
where bu.id = tvbu.bruker_utbetaling_id
  and bu.behandling_id = tvbu.bruker_utbetaling_behandling_id;

drop table tilskudd_vedtak_bruker_utbetaling;

-- Foreløpig bare aktiv i dev, så fjerner bruker_utbetalinger uten referanser til tilskudd (2 stk) for å kunne sette not null constraint.
delete from bruker_utbetaling bu
using (values
           ('ceb68206-2df3-42de-8701-f01db551e4fd'::uuid,'2025/12721'),
           ('21a3a00c-0c57-4648-b6d5-946952fd7656'::uuid, '2025/12721'),
           ('1e847de0-067d-441b-871e-ace5fa53b24b'::uuid,'2025/12726')
) as v(id, sakId)
where bu.id = v.id
  and bu.sak_id = v.sakId;

alter table bruker_utbetaling
    alter column tilskudd_vedtak_id set not null,
    add constraint bruker_utbetaling_tilskudd_vedtak_id_fkey
        foreign key (tilskudd_vedtak_id) references tilskudd_vedtak (id);
