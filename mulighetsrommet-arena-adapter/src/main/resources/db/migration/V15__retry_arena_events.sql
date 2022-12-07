alter table arena_events
    add column retries int not null default 0;
