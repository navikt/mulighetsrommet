create or replace view view_deltaker as
select deltaker.id,
       deltaker.gjennomforing_id,
       deltaker.start_dato,
       deltaker.slutt_dato,
       deltaker.registrert_tidspunkt,
       deltaker.endret_tidspunkt,
       deltaker.status_type,
       deltaker.status_aarsak,
       deltaker.status_opprettet_tidspunkt,
       deltakelsesmengder_json,
       deltaker.innhold_annet,
       deltaker.nav_veileder_nav_ident,
       deltaker.nav_veileder_enhetsnummer
from deltaker
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'gyldigFra', gyldig_fra,
                                                   'deltakelsesprosent', deltakelsesprosent,
                                                   'opprettetTidspunkt', opprettet_tidspunkt
                                           ) order by gyldig_fra
                                   ) as deltakelsesmengder_json
                            from deltaker_deltakelsesmengde
                            where deltaker_id = deltaker.id) on true
