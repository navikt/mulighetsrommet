alter table events
    drop constraint events_unique;

alter table events
    drop column record_offset,
    drop column partition;

alter table events
    add constraint events_unique unique (topic, key);
