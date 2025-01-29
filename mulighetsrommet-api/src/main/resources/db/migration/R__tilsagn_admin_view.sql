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
       tilsagn.status_besluttet_av,
       tilsagn.status_endret_av,
       tilsagn.status_endret_tidspunkt,
       tilsagn.status_forklaring,
       tilsagn.status_aarsaker,
       tilsagn.type,
       concat(nav_ansatt_beslutter.fornavn, ' ', nav_ansatt_beslutter.etternavn) as beslutter_navn,
       concat(nav_ansatt_endret_av.fornavn, ' ', nav_ansatt_endret_av.etternavn) as endret_av_navn,
       nav_enhet.navn                                                            as kostnadssted_navn,
       nav_enhet.overordnet_enhet                                                as kostnadssted_overordnet_enhet,
       nav_enhet.type                                                            as kostnadssted_type,
       nav_enhet.status                                                          as kostnadssted_status,
       arrangor.id                                                               as arrangor_id,
       arrangor.organisasjonsnummer                                              as arrangor_organisasjonsnummer,
       arrangor.navn                                                             as arrangor_navn,
       arrangor.slettet_dato is not null                                         as arrangor_slettet,
       gjennomforing.tiltaksnummer                                               as tiltaksnummer,
       tiltakstype.tiltakskode                                                   as tiltakskode
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join arrangor on arrangor.id = tilsagn.arrangor_id
         inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
         inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
         left join nav_ansatt nav_ansatt_beslutter on nav_ansatt_beslutter.nav_ident = tilsagn.status_besluttet_av
         left join nav_ansatt nav_ansatt_endret_av on nav_ansatt_endret_av.nav_ident = tilsagn.status_endret_av;

