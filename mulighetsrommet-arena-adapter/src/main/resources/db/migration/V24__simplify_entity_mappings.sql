alter table arena_entity_mapping
    add column entity_id uuid unique;

update arena_entity_mapping
set entity_id = coalesce(tiltakstype_id, tiltaksgjennomforing_id, deltaker_id);

alter table arena_entity_mapping
    alter column entity_id set not null;

alter table arena_entity_mapping
    drop column tiltakstype_id,
    drop column tiltaksgjennomforing_id,
    drop column deltaker_id;
