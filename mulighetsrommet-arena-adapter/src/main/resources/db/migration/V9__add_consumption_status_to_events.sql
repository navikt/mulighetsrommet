create type consumption_status as enum ('Pending', 'Processed', 'Failed', 'Ignored', 'Invalid');

alter table events
    add consumption_status consumption_status not null default 'Processed',
    add message            text;
