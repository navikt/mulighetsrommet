alter table user_notification
    rename read_at to done_at;

alter type notification_type rename value 'Task' to 'TASK';
alter type notification_type rename value 'Notification' to 'NOTIFICATION';
