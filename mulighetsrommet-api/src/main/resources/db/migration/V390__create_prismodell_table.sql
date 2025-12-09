create table if not exists avtale_prismodell
(
    id              uuid not null primary key default gen_random_uuid(),
    avtale_id       uuid not null references avtale (id),
    prismodell_type prismodell not null,
    prisbetingelser text
);


insert into avtale_prismodell (avtale_id, prismodell_type, prisbetingelser)
select id,
       prismodell,
       prisbetingelser
from avtale;

alter table avtale_sats
    add column prismodell_id uuid references avtale_prismodell (id);

update avtale_sats s
set prismodell_id = p.id
from avtale_prismodell p
where s.avtale_id = p.avtale_id;
