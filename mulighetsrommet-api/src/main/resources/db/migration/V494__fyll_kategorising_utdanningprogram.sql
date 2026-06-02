-- gjennomforing utdanning til kategoriseringstabell
with unike_gjennomforing_utdanningsprogram as (select distinct gu.gjennomforing_id,
                                                               gu.utdanningsprogram_id
                                               from gjennomforing_utdanningsprogram gu),
     src as (select ugu.gjennomforing_id,
                    ugu.utdanningsprogram_id,
                    gen_random_uuid() as new_id
             from unike_gjennomforing_utdanningsprogram ugu),
     new_utdanning_mapping as (select src.new_id,
                                      gu.utdanning_id
                               from src
                                        inner join gjennomforing_utdanningsprogram gu
                                                   on src.gjennomforing_id = gu.gjennomforing_id),
     ins_kategorisering_utdanning as (
         insert into opplaring_kategorisering (id, utdanningsprogram_id)
             select src.new_id,
                    src.utdanningsprogram_id
             from src
             on conflict do nothing),
        ins_kategorisering_utdanning_mapping as (
            insert into opplaring_kategorisering_utdanning (opplaring_kategorisering_id, utdanning_id)
                select num.new_id,
                       num.utdanning_id
                from new_utdanning_mapping num
            on conflict  do nothing)
update gjennomforing g
set opplaring_kategorisering_id = src.new_id
from src
where g.id = src.gjennomforing_id;

-- avtale utdanning til kategoriseringstabell
with unike_avtale_utdanningsprogram as (select distinct gu.avtale_id,
                                                               gu.utdanningsprogram_id
                                               from avtale_utdanningsprogram gu),
     src as (select ugu.avtale_id,
                    ugu.utdanningsprogram_id,
                    gen_random_uuid() as new_id
             from unike_avtale_utdanningsprogram ugu),
     new_utdanning_mapping as (select src.new_id,
                                      au.utdanning_id
                               from src
                                        inner join avtale_utdanningsprogram au
                                                   on src.avtale_id = au.avtale_id),
     ins_kategorisering_utdanning as (
         insert into opplaring_kategorisering (id, utdanningsprogram_id)
             select src.new_id,
                    src.utdanningsprogram_id
             from src
             on conflict do nothing),
     ins_kategorisering_utdanning_mapping as (
         insert into opplaring_kategorisering_utdanning (opplaring_kategorisering_id, utdanning_id)
             select num.new_id,
                    num.utdanning_id
             from new_utdanning_mapping num
             on conflict  do nothing)
update avtale a
set opplaring_kategorisering_id = src.new_id
from src
where a.id = src.avtale_id;
