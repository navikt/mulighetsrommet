drop view if exists refusjonskrav_aft_view;
drop view if exists refusjonskrav_admin_dto_view;

create view refusjonskrav_admin_dto_view as
select refusjonskrav.id,
       refusjonskrav.status,
       gjennomforing.id                  as gjennomforing_id,
       gjennomforing.navn                as gjennomforing_navn,
       arrangor.id                       as arrangor_id,
       arrangor.organisasjonsnummer      as arrangor_organisasjonsnummer,
       arrangor.navn                     as arrangor_navn,
       arrangor.slettet_dato is not null as arrangor_slettet,
       tiltakstype.navn                  as tiltakstype_navn
from refusjonskrav
         inner join tiltaksgjennomforing gjennomforing on gjennomforing.id = refusjonskrav.gjennomforing_id
         inner join arrangor on gjennomforing.arrangor_id = arrangor.id
         inner join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id;

create view refusjonskrav_aft_view as
with deltakelse_perioder as (select refusjonskrav_id,
                                    deltakelse_id,
                                    jsonb_agg(jsonb_build_object(
                                            'start', lower(periode),
                                            'slutt', upper(periode),
                                            'stillingsprosent', prosent_stilling
                                              )) as perioder
                             from refusjonskrav_deltakelse_periode
                             group by refusjonskrav_id, deltakelse_id),
     krav_perioder as (select refusjonskrav_id,
                              jsonb_agg(jsonb_build_object(
                                      'deltakelseId', deltakelse_id,
                                      'perioder', deltakelse_perioder.perioder
                                        )) as deltakelser
                       from deltakelse_perioder
                       group by refusjonskrav_id),
     krav_manedsverk as (select refusjonskrav_id,
                                jsonb_agg(jsonb_build_object(
                                        'deltakelseId', deltakelse_id,
                                        'manedsverk', manedsverk
                                          )) as deltakelser
                         from refusjonskrav_deltakelse_manedsverk
                         group by refusjonskrav_id)
select krav.*,
       beregning.belop,
       beregning.sats,
       lower(beregning.periode)                           as periode_start,
       upper(beregning.periode)                           as periode_slutt,
       coalesce(krav_perioder.deltakelser, '[]'::jsonb)   as perioder_json,
       coalesce(krav_manedsverk.deltakelser, '[]'::jsonb) as manedsverk_json
from refusjonskrav_admin_dto_view krav
         join
     refusjonskrav_beregning_aft beregning on krav.id = beregning.refusjonskrav_id
         left join
     krav_perioder on krav.id = krav_perioder.refusjonskrav_id
         left join
     krav_manedsverk on krav.id = krav_manedsverk.refusjonskrav_id;
