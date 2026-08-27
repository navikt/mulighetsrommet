-- Bytt primærnøkkel i bruker_utbetaling til kompositt (id, behandling_id)
-- oppdater tilskudd FK til å referere til kompositt

alter table tilskudd
    drop constraint if exists tilskudd_bruker_utbetaling_id_fkey;

alter table bruker_utbetaling
    drop constraint bruker_utbetaling_pkey;

alter table bruker_utbetaling
    add primary key (id, behandling_id);

alter table tilskudd
    add constraint tilskudd_bruker_utbetaling_fkey
        foreign key (bruker_utbetaling_id, bruker_utbetaling_behandling_id)
            references bruker_utbetaling (id, behandling_id);
