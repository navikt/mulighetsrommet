insert into totrinnskontroll_type (value)
values ('ENKELTPLASS_PRISENDRING');

create table enkeltplass_prisendring
(
    totrinnskontroll_id uuid primary key references totrinnskontroll (id),
    gjennomforing_id    uuid                     not null references gjennomforing (id),
    prismodell_id       uuid                     not null references prismodell (id),
    created_at          timestamp with time zone not null default now()
);

create index on enkeltplass_prisendring (gjennomforing_id);
