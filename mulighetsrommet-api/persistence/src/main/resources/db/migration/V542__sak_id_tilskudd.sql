drop view if exists view_tilskudd_behandling;

alter table tilskudd
    add column sak_id text;

with base as (
    select distinct
        t.id,
        g.lopenummer
    from tilskudd t
             join tilskudd_vedtak tv on tv.tilskudd_id = t.id
             join tilskudd_behandling tb on tb.id = tv.tilskudd_behandling_id
             join gjennomforing g on g.id = tb.gjennomforing_id
),
     nummerert as (
         select
             id,
             lopenummer,
             row_number() over (
                 partition by lopenummer
                 order by id
                 ) as enumerasjon
         from base
     )
update tilskudd t
set sak_id = nummerert.lopenummer || '-' || nummerert.enumerasjon
from nummerert
where t.id = nummerert.id;
