alter table utdanning
    add column nus_koder text[];

with nus_koder as (select utdanning_id,
                          array_agg(nus_kode) as koder
                   from utdanning_nus_kode
                   group by utdanning_id)
update utdanning
set nus_koder = nus_koder.koder
from nus_koder
where utdanning.utdanning_id = nus_koder.utdanning_id;

drop table utdanning_nus_kode;

drop table utdanning_nus_kode_innhold;
