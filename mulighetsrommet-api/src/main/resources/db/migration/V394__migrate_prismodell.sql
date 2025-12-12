DROP VIEW IF EXISTS view_gjennomforing;
DROP VIEW IF EXISTS view_avtale;

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

alter table avtale_sats
    add constraint fk_sats_prismodell
        foreign key (prismodell_id)
            references avtale_prismodell (id);

alter table avtale
    drop column prismodell,
    drop column prisbetingelser;
