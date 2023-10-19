drop view tiltaksgjennomforing_admin_dto_view;

alter table tiltaksgjennomforing
add column nav_region text references nav_enhet(enhetsnummer);

update tiltaksgjennomforing tg
set nav_region = a.nav_region
from avtale a
where a.id = tg.avtale_id;

with regioner as (
    select id, nav_region from avtale
)
insert into avtale_nav_enhet(avtale_id, enhetsnummer)
select * from regioner where nav_region is not null
on conflict(avtale_id, enhetsnummer) do nothing;

alter table avtale
drop column nav_region;
