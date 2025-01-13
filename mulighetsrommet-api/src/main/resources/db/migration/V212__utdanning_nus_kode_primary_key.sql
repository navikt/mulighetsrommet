alter table utdanning_nus_kode
    add column tmp_primary_key bigint generated always as identity;

alter table utdanning_nus_kode
    add primary key (tmp_primary_key);

with cte as (select ctid, row_number() over (partition by utdanning_id, nus_kode order by ctid) as row_num
             from utdanning_nus_kode)
delete
from utdanning_nus_kode
where ctid in (select ctid
               from cte
               where row_num > 1);

alter table utdanning_nus_kode
    drop column tmp_primary_key;

alter table utdanning_nus_kode
    add primary key (utdanning_id, nus_kode);
