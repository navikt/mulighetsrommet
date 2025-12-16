DROP VIEW IF EXISTS view_gjennomforing;
DROP VIEW IF EXISTS view_avtale;

create table avtale_prismodell
(
    id              uuid primary key default gen_random_uuid(),
    avtale_id       uuid       not null references avtale (id),
    prismodell_type prismodell not null,
    prisbetingelser text,
    satser          jsonb
);

insert into avtale_prismodell (avtale_id, prismodell_type, prisbetingelser)
select id,
       prismodell,
       prisbetingelser
from avtale;

update avtale_prismodell p
set satser = s.satser
from (select avtale_id,
             jsonb_agg(jsonb_build_object('sats', sats, 'gjelderFra', gjelder_fra) order by gjelder_fra) as satser
      from avtale_sats
      group by avtale_id) s
where p.avtale_id = s.avtale_id;

alter table avtale_sats
    add column prismodell_id uuid references avtale_prismodell (id);

drop table avtale_sats;

alter table avtale
    drop column prismodell,
    drop column prisbetingelser;
