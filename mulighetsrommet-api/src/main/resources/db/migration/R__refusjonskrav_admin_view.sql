drop view if exists refusjonskrav_admin_dto_view;

create view refusjonskrav_admin_dto_view as
select refusjonskrav.id,
       refusjonskrav.periode_start,
       refusjonskrav.periode_slutt,
       gjennomforing.id                  as tiltaksgjennomforing_id,
       gjennomforing.navn                as tiltaksgjennomforing_navn,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       tiltakstype.navn as tiltakstype_navn
from refusjonskrav
         inner join tiltaksgjennomforing gjennomforing on gjennomforing.id = refusjonskrav.tiltaksgjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id;

create view refusjonskrav_aft_view as
with deltakelser as (select refusjonskrav_id,
                            jsonb_build_object(
                                    'deltakelseId',
                                    deltakelse_id,
                                    'perioder',
                                    jsonb_agg(jsonb_build_object(
                                            'start', lower(periode),
                                            'slutt', upper(periode),
                                            'stillingsprosent', prosent_stilling))
                            ) as deltakelse
                     from refusjonskrav_deltakelse_periode
                     group by refusjonskrav_id, deltakelse_id),
     beregning_aft as (select beregning.refusjonskrav_id,
                              beregning.belop,
                              beregning.sats,
                              deltakelser_json
                       from refusjonskrav_beregning_aft beregning
                                left join lateral (select jsonb_agg(deltakelser.deltakelse) as deltakelser_json
                                                   from deltakelser
                                                   where deltakelser.refusjonskrav_id = beregning.refusjonskrav_id
                                                   group by beregning.refusjonskrav_id
                           ) on true)
select krav.*, beregning_aft.belop, beregning_aft.sats, beregning_aft.deltakelser_json
from refusjonskrav_admin_dto_view krav
         left join beregning_aft on refusjonskrav_id = id;
