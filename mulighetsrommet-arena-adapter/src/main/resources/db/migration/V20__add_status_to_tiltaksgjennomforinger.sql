alter table tiltaksgjennomforing
    add column status text;

with tg as (select aem.tiltaksgjennomforing_id,
                   case
                       when (ae.payload ->> 'op_type') = 'D' then (ae.payload -> 'before' ->> 'TILTAKSTATUSKODE')
                       else (ae.payload -> 'after' ->> 'TILTAKSTATUSKODE') end
                       as status
            from arena_entity_mapping aem
                     join arena_events ae on aem.arena_id = ae.arena_id and aem.arena_table = ae.arena_table
            where aem.arena_table = 'SIAMO.TILTAKGJENNOMFORING')
update tiltaksgjennomforing
set status = tg.status from tg
where tiltaksgjennomforing.id = tg.tiltaksgjennomforing_id;

alter table tiltaksgjennomforing
    alter column status set not null;
