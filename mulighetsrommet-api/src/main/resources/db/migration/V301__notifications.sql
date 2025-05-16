alter table notification
    drop type;

drop type notification_type;

alter table user_notification
    rename done_at to read_at;
