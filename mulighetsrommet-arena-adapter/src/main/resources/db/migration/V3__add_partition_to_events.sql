alter table events
    add column partition int;

alter table events
    drop constraint events_unique;

alter table events
    add constraint events_unique UNIQUE (topic, record_offset, partition);
