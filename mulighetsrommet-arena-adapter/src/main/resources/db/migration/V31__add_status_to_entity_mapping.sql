create type entity_status as enum ('Handled', 'Ignored', 'Unhandled');

alter table arena_entity_mapping
    add column status entity_status default 'Unhandled';
