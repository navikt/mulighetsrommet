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
       opprett_to_trinnskontroll_json,
       annuller_to_trinnskontroll_json
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join arrangor on arrangor.id = tilsagn.arrangor_id
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         left join lateral (
             select jsonb_build_object(
                 'opprett', jsonb_build_object(
                    'navIdent', to_trinnskontroll.opprettet_av,
                    'tidspunkt', to_trinnskontroll.opprettet_tidspunkt,
                    'navn', concat(nav_ansatt_opprettet.fornavn, ' ', nav_ansatt_opprettet.etternavn)
                 ),
                 'beslutt',
                     CASE
                         WHEN to_trinnskontroll.besluttet_av IS NULL THEN NULL
                         ELSE jsonb_build_object(
                             'navIdent', to_trinnskontroll.besluttet_av,
                             'tidspunkt', to_trinnskontroll.besluttet_tidspunkt,
                             'besluttelse', to_trinnskontroll.besluttelse,
                             'navn', concat(nav_ansatt_besluttet.fornavn, ' ', nav_ansatt_besluttet.etternavn)
                         )
                     END,
                 'aarsaker', to_trinnskontroll.aarsaker,
                 'forklaring', to_trinnskontroll.forklaring
             ) as opprett_to_trinnskontroll_json
             from to_trinnskontroll
                 left join nav_ansatt nav_ansatt_opprettet on nav_ansatt_opprettet.nav_ident = to_trinnskontroll.opprettet_av
                 left join nav_ansatt nav_ansatt_besluttet on nav_ansatt_besluttet.nav_ident = to_trinnskontroll.besluttet_av
             where to_trinnskontroll.entity_id = tilsagn.id
                 and to_trinnskontroll.type = 'OPPRETT_TILSAGN'
             order by to_trinnskontroll.opprettet_tidspunkt desc
             limit 1
         ) ttt on true
         left join lateral (
             select jsonb_build_object(
                 'opprett', jsonb_build_object(
                    'navIdent', to_trinnskontroll.opprettet_av,
                    'tidspunkt', to_trinnskontroll.opprettet_tidspunkt,
                    'navn', concat(nav_ansatt_opprettet.fornavn, ' ', nav_ansatt_opprettet.etternavn)
                 ),
                 'beslutt',
                     CASE
                         WHEN to_trinnskontroll.besluttet_av IS NULL THEN NULL
                         ELSE jsonb_build_object(
                             'navIdent', to_trinnskontroll.besluttet_av,
                             'tidspunkt', to_trinnskontroll.besluttet_tidspunkt,
                             'besluttelse', to_trinnskontroll.besluttelse,
                             'navn', concat(nav_ansatt_besluttet.fornavn, ' ', nav_ansatt_besluttet.etternavn)
                         )
                     END,
                 'aarsaker', to_trinnskontroll.aarsaker,
                 'forklaring', to_trinnskontroll.forklaring
             ) as annuller_to_trinnskontroll_json
             from to_trinnskontroll
                 left join nav_ansatt nav_ansatt_opprettet on nav_ansatt_opprettet.nav_ident = to_trinnskontroll.opprettet_av
                 left join nav_ansatt nav_ansatt_besluttet on nav_ansatt_besluttet.nav_ident = to_trinnskontroll.besluttet_av
             where to_trinnskontroll.entity_id = tilsagn.id
                 and to_trinnskontroll.type = 'ANNULLER_TILSAGN'
             order by to_trinnskontroll.opprettet_tidspunkt desc
             limit 1
         ) tttt on true;

