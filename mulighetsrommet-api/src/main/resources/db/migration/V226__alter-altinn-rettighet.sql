delete from altinn_person_rettighet where rettighet = 'TILTAK_ARRANGOR_REFUSJON';
drop type altinn_ressurs cascade;

create type altinn_ressurs as enum ('TILTAK_ARRANGOR_UTBETALING')
