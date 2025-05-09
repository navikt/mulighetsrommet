alter table tilsagn
    add column belop_brukt int NOT NULL DEFAULT 0;

update tilsagn
set belop_brukt = coalesce(
        (select sum(belop)
         from delutbetaling du
         where tilsagn_id = tilsagn.id
           and tilsagn.status not in ('TIL_GODKJENNING', 'RETURNERT')
           and du.status not in ('TIL_GODKJENNING', 'RETURNERT'))
    , 0);

alter table tilsagn
    drop column belop_gjenstaende;
