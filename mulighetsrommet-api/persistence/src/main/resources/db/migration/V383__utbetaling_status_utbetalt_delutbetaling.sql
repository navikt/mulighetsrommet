update utbetaling u
set status = sub.ny_status
from (select u2.id,
             case
                 when count(du.id) = count(case when du.faktura_status in ('FULLT_BETALT', 'DELVIS_BETALT') then 1 end)
                     then 'UTBETALT'::utbetaling_status
                 when count(case when du.faktura_status in ('FULLT_BETALT', 'DELVIS_BETALT') then 1 end) > 0
                     then 'DELVIS_UTBETALT'::utbetaling_status
                 end as ny_status
      from utbetaling u2
               inner join delutbetaling du on du.utbetaling_id = u2.id
      group by u2.id) as sub
where u.id = sub.id
  and sub.ny_status is not null
