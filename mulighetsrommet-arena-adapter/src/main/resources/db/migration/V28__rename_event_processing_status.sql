alter type consumption_status rename to processing_status;

alter table arena_events
    rename column consumption_status to processing_status;
