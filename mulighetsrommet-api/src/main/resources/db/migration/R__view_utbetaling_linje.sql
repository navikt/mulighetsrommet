-- ${flyway:timestamp}

drop view if exists view_utbetaling_linje;

create view view_utbetaling_linje as
select delutbetaling.id,
       delutbetaling.tilsagn_id,
       delutbetaling.utbetaling_id,
       delutbetaling.status,
       delutbetaling.belop,
       delutbetaling.gjor_opp_tilsagn,
       delutbetaling.periode,
       delutbetaling.lopenummer,
       delutbetaling.fakturanummer,
       delutbetaling.faktura_status,
       delutbetaling.faktura_status_sist_oppdatert,
       delutbetaling.sendt_til_okonomi_tidspunkt,
       utbetaling.utbetales_tidligst_tidspunkt
from delutbetaling
         join utbetaling on delutbetaling.utbetaling_id = utbetaling.id
order by delutbetaling.created_at
