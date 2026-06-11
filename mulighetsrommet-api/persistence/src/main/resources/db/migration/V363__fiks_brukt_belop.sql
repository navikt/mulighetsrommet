-- Oppdater to tilsagn i prod som har lidd av dobbelt-godkjenning (før db-lås var på plass)
-- basert på migrering V292

update tilsagn
set belop_brukt = coalesce(
        (select sum(belop)
         from delutbetaling du
         where tilsagn_id = tilsagn.id
           and du.status not in ('TIL_ATTESTERING', 'RETURNERT'))
    , 0)
where tilsagn.id in ('fc364a28-0bd3-4c1b-abc2-501333d64d0c', 'f3233f35-12c7-41f2-be15-6a8314ee4922')
