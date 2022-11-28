alter table events
    rename to arena_events;

alter table arena_events
    rename column key to arena_id;

alter table arena_events
    rename column topic to arena_table;

update arena_events
set arena_table = payload ->> 'table';

alter table arena_events
    drop column id cascade;

alter table arena_events
    add primary key (arena_table, arena_id);

alter table arena_events
    drop constraint events_unique;
