alter table notification
    drop type;

drop type notification_type;

alter table user_notification
    rename done_at to read_at;

alter table user_notification
    alter read_at type timestamptz using read_at::timestamptz at time zone 'Europe/Oslo';
