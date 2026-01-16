drop view if exists view_avtale;
drop view if exists view_gjennomforing;

alter type prismodell rename to prismodell_type;

alter table avtale_prismodell
    alter id drop default;

alter table avtale_prismodell
    rename to prismodell;

alter table prismodell
    add created_at timestamp default now(),
    add updated_at timestamp default now();

create trigger set_timestamp
    before update
    on prismodell
    for each row
execute procedure trigger_set_timestamp();


create table avtale_prismodell
(
    avtale_id     uuid not null references avtale (id),
    prismodell_id uuid not null references prismodell (id),
    created_at    timestamp default now(),
    updated_at    timestamp default now(),
    primary key (avtale_id, prismodell_id)
);

create trigger set_timestamp
    before update
    on avtale_prismodell
    for each row
execute procedure trigger_set_timestamp();

insert into avtale_prismodell(avtale_id, prismodell_id)
select avtale_id, id as prismodell_id
from prismodell;

alter table prismodell
    drop avtale_id;

alter table prismodell
    add system_id text unique;
