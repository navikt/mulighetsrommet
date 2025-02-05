drop view if exists tilsagn_admin_dto_view;

create view tilsagn_admin_dto_view as
select tilsagn.id,
       tilsagn.gjennomforing_id,
       tilsagn.periode_start,
       tilsagn.periode_slutt,
       tilsagn.beregning,
       tilsagn.lopenummer,
       tilsagn.kostnadssted,
       tilsagn.status,
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
       tiltakstype.tiltakskode                                                   as tiltakskode,
       sist_handling_json
from tilsagn
    inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join arrangor on arrangor.id = tilsagn.arrangor_id
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         left join lateral (
             select jsonb_build_object(
                 'opprettetAv', to_trinnskontroll_handling_log.opprettet_av,
                 'opprettetAvNavn', concat(nav_ansatt_handling.fornavn, ' ', nav_ansatt_handling.etternavn),
                 'createdAt', to_trinnskontroll_handling_log.created_at,
                 'handling', to_trinnskontroll_handling_log.handling,
                 'aarsaker', to_trinnskontroll_handling_log.aarsaker,
                 'forklaring', to_trinnskontroll_handling_log.forklaring
             ) as sist_handling_json
             from to_trinnskontroll_handling_log
                 left join nav_ansatt nav_ansatt_handling on nav_ansatt_handling.nav_ident = to_trinnskontroll_handling_log.opprettet_av
             where to_trinnskontroll_handling_log.entity_id = tilsagn.id
             order by to_trinnskontroll_handling_log.id desc limit 1
         ) sist_handling on true;

