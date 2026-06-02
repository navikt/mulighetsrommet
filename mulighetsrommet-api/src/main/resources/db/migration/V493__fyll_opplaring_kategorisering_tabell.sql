-- gjennomforinger til kategoriseringstabell
with src as (select gak.gjennomforing_id,
                    gak.kurstype_id,
                    gak.bransje_id,
                    gak.norskprove,
                    gak.innhold_elementer,
                    gen_random_uuid() as new_id
             from gjennomforing_amo_kategorisering gak),
     src_innhold_elementer as (select s.new_id,
                                      unnest(s.innhold_elementer) as innhold_element
                               from src s
                               where s.innhold_elementer is not null),
     ins_kategorisering as (
         insert into opplaring_kategorisering (id, kurstype_id, bransje_id, norskprove)
             select s.new_id,
                    okk.id,
                    okb.id,
                    s.norskprove
             from src s
                left join opplaring_kategorisering_kurstype okk on s.kurstype_id = okk.id
                left join opplaring_kategorisering_bransje okb on s.bransje_id = okb.id),
     ins_innhold_element as (
         insert into opplaring_kategorisering_innhold_element (opplaring_kategorisering_id, innhold_element_id)
             select sie.new_id,
                    kie.id
             from opplaring_innhold_element kie
                      inner join src_innhold_elementer sie on kie.kode = sie.innhold_element::text
             on conflict do nothing),
     ins_forerkort as (insert into opplaring_kategorisering_forerkort (opplaring_kategorisering_id, forerkort_id)
         select s.new_id,
                fk.forerkort_id
         from src s
                  inner join gjennomforing_amo_kategorisering_forerkort fk on s.gjennomforing_id = fk.gjennomforing_id
         on conflict do nothing),
     ins_sertifisering as (
         insert into opplaring_kategorisering_sertifisering (opplaring_kategorisering_id, konsept_id)
             select s.new_id,
                    gaks.konsept_id
             from src s
                      inner join gjennomforing_amo_kategorisering_sertifisering gaks
                                 on s.gjennomforing_id = gaks.gjennomforing_id
             on conflict do nothing)
update gjennomforing g
set opplaring_kategorisering_id = s.new_id
from src s
where g.id = s.gjennomforing_id;

-- avtale til kategoriseringstabell
with src as (select gak.avtale_id,
                    gak.kurstype_id,
                    gak.bransje_id,
                    gak.norskprove,
                    gak.innhold_elementer,
                    gen_random_uuid() as new_id
             from avtale_amo_kategorisering gak),
     src_innhold_elementer as (select src.new_id,
                                      unnest(src.innhold_elementer) as innhold_element
                               from src
                               where src.innhold_elementer is not null),
     ins_kategorisering as (
         insert into opplaring_kategorisering (id, kurstype_id, bransje_id, norskprove)
             select src.new_id,
                    okk.id,
                    okb.id,
                    src.norskprove
             from src
                      left join opplaring_kategorisering_kurstype okk on src.kurstype_id = okk.id
                      left join opplaring_kategorisering_bransje okb on src.bransje_id = okb.id),
     ins_innhold_element as (
         insert into opplaring_kategorisering_innhold_element (opplaring_kategorisering_id, innhold_element_id)
             select sie.new_id,
                    kie.id
             from opplaring_innhold_element kie
                      inner join src_innhold_elementer sie on kie.kode = sie.innhold_element::text
             on conflict do nothing),
     ins_forerkort as (insert into opplaring_kategorisering_forerkort (opplaring_kategorisering_id, forerkort_id)
         select src.new_id,
                fk.forerkort_id
         from src
                  inner join avtale_amo_kategorisering_forerkort fk on src.avtale_id = fk.avtale_id
         on conflict do nothing),
     ins_sertifisering as (
         insert into opplaring_kategorisering_sertifisering (opplaring_kategorisering_id, konsept_id)
             select s.new_id,
                    aaks.konsept_id
             from src s
                      inner join avtale_amo_kategorisering_sertifisering aaks
                                 on s.avtale_id = aaks.avtale_id
             on conflict do nothing)
update avtale
set opplaring_kategorisering_id = src.new_id
from src
where avtale.id = src.avtale_id;
