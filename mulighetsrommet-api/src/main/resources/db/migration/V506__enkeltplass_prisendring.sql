alter type besluttelse add value if not exists 'PA_VENT';

update totrinnskontroll
set besluttelse = 'PA_VENT'
where type = 'ENKELTPLASS_OKONOMI'
  and besluttelse = 'AVVIST';;;;


insert into totrinnskontroll_type (value)
values ('ENKELTPLASS_PRISENDRING');

create table enkeltplass_prisendring
(
    totrinnskontroll_id uuid primary key references totrinnskontroll (id) on delete cascade,
    gjennomforing_id    uuid                     not null references gjennomforing (id) on delete cascade,
    prismodell_id       uuid                     not null references prismodell (id) on delete cascade,
    created_at          timestamp with time zone not null default now()
);

create index on enkeltplass_prisendring (gjennomforing_id);
