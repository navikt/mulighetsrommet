-- ${flyway:timestamp}

drop view if exists view_deltaker;

create view view_deltaker as
select deltaker.id,
       deltaker.gjennomforing_id,
       deltaker.start_dato,
       deltaker.slutt_dato,
       deltaker.registrert_dato,
       deltaker.registrert_tidspunkt,
       deltaker.endret_tidspunkt,
       deltaker.status_type,
       deltaker.status_aarsak,
       deltaker.status_opprettet_tidspunkt,
       deltakelsesmengder_json
from deltaker
         left join lateral (select jsonb_agg(
                                           jsonb_build_object(
                                                   'gyldigFra', gyldig_fra,
                                                   'deltakelsesprosent', deltakelsesprosent
                                           ) order by gyldig_fra
                                   ) as deltakelsesmengder_json
                            from deltaker_deltakelsesmengde
                            where deltaker_id = deltaker.id) on true
