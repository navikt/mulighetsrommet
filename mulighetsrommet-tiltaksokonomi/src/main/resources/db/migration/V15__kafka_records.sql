alter table kafka_consumer_record
    alter last_retry type timestamptz using last_retry at time zone 'UTC',
    alter created_at type timestamptz using created_at at time zone 'UTC';

alter table kafka_producer_record
    alter created_at type timestamptz using created_at at time zone 'UTC';

drop table failed_events;
