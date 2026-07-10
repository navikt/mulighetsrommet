create or replace view view_utbetaling_linje as
select linje.id,
       linje.tilsagn_id,
       linje.utbetaling_id,
       linje.status,
       linje.belop,
       linje.valuta,
       linje.gjor_opp_tilsagn,
       linje.periode,
       linje.lopenummer,
       linje.fakturanummer,
       linje.faktura_status,
       linje.faktura_status_endret_tidspunkt,
       linje.faktura_sendt_tidspunkt,
       utbetaling.utbetales_tidligst_tidspunkt
from utbetaling_linje linje
         join utbetaling on linje.utbetaling_id = utbetaling.id
order by linje.created_at
