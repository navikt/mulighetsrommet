alter index if exists topic rename to arena_events_arena_table_idx;

create index arena_events_consumption_status_idx on arena_events (consumption_status);
