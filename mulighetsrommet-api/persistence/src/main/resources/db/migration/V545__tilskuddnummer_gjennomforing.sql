drop view if exists view_tilskudd_behandling;

alter table tilskudd
    add column gjennomforing_id uuid references gjennomforing(id),
    add column tilskuddsnummer text;

with base as (
    select distinct
        t.id,
        g.id as gjennomforing_id,
        g.lopenummer
    from tilskudd t
             join tilskudd_vedtak tv on tv.tilskudd_id = t.id
             join tilskudd_behandling tb on tb.id = tv.tilskudd_behandling_id
             join gjennomforing g on g.id = tb.gjennomforing_id
),
     nummerert as (
         select
             id,
             gjennomforing_id,
             lopenummer,
             row_number() over (
                 partition by lopenummer
                 order by id
                 ) as enumerasjon
         from base
     )
update tilskudd t
set gjennomforing_id = nummerert.gjennomforing_id,
    tilskuddsnummer = nummerert.lopenummer || '-' || nummerert.enumerasjon
from nummerert
where t.id = nummerert.id;

alter table tilskudd
    alter column tilskuddsnummer set not null,
    add constraint tilskuddsnummer_unique unique (tilskuddsnummer),
    alter column gjennomforing_id set not null;
