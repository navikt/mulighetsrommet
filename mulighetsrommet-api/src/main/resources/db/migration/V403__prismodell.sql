drop view if exists view_avtale;
drop view if exists view_gjennomforing;

alter type prismodell rename to prismodell_type;

alter table avtale_prismodell
    alter id drop default;

alter table avtale_prismodell
    rename to prismodell;

create table avtale_prismodell
(
    avtale_id     uuid not null,
    prismodell_id uuid not null,
    primary key (avtale_id, prismodell_id)
);

insert into avtale_prismodell(avtale_id, prismodell_id)
select avtale_id, id as prismodell_id
from prismodell;

alter table prismodell
    drop avtale_id;
