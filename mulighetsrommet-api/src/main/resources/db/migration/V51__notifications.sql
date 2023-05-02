create type notification_type as enum ('Notification', 'Task');

create type notification_target as enum ('All', 'User');

create table notification
(
    id          uuid primary key    not null,
    type        notification_type   not null,
    target      notification_target not null,
    title       text                not null,
    description text,
    created_at  timestamp           not null
);

create index notification_target_created_at_idx on notification (target, created_at desc);

create table user_notification
(
    notification_id uuid not null references notification (id) on delete cascade,
    user_id         text not null,
    read_at         timestamp,
    primary key (notification_id, user_id)
);

