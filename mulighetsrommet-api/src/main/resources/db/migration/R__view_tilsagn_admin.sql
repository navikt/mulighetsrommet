-- ${flyway:timestamp}

drop view if exists tilsagn_admin_dto_view;

create view tilsagn_admin_dto_view as
select
    tilsagn.id,
    tilsagn.gjennomforing_id,
    lower(tilsagn.periode)                                                    as periode_start,
    date(upper(tilsagn.periode) - interval '1 day')                           as periode_slutt,
    tilsagn.beregning,
    tilsagn.lopenummer,
    tilsagn.bestillingsnummer,
    tilsagn.kostnadssted,
    tilsagn_status(
        opprettelse.besluttelse,
        annullering.behandlet_av,
        annullering.besluttelse
    ) as status,
    tilsagn.type,
    nav_enhet.navn                                                            as kostnadssted_navn,
    nav_enhet.overordnet_enhet                                                as kostnadssted_overordnet_enhet,
    nav_enhet.type                                                            as kostnadssted_type,
    nav_enhet.status                                                          as kostnadssted_status,
    arrangor.id                                                               as arrangor_id,
    arrangor.organisasjonsnummer                                              as arrangor_organisasjonsnummer,
    arrangor.navn                                                             as arrangor_navn,
    arrangor.slettet_dato is not null                                         as arrangor_slettet,
    gjennomforing.tiltaksnummer                                               as tiltaksnummer,
    gjennomforing.navn                                                        as gjennomforing_navn,
    tiltakstype.tiltakskode                                                   as tiltakskode
from tilsagn
    inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
    inner join arrangor on arrangor.id = tilsagn.arrangor_id
    inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
    inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
    left join lateral (
        select *
        from totrinnskontroll
        where totrinnskontroll.entity_id = tilsagn.id
          and totrinnskontroll.type = 'OPPRETT'
        order by totrinnskontroll.behandlet_tidspunkt desc
        limit 1
    ) as opprettelse on true
    left join lateral (
        select *
        from totrinnskontroll
        where totrinnskontroll.entity_id = tilsagn.id
          and totrinnskontroll.type = 'ANNULLER'
        order by totrinnskontroll.behandlet_tidspunkt desc
        limit 1
    ) as annullering on true;

