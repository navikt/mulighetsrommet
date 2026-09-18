create or replace view view_tilskudd_vedtak as
select v.id,
       v.tilskudd_id,
       v.lopenummer,
       v.tilskudd_behandling_id,
       v.soknad_journalpost_id,
       v.soknad_dato,
       v.periode,
       v.kostnadssted,
       jsonb_build_object(
               'valuta', v.soknad_valuta,
               'belop', v.soknad_belop
       ) as soknad_belop,
       CASE
           WHEN v.belop IS NULL THEN NULL
           ELSE jsonb_build_object(
                   'valuta', v.valuta,
                   'belop', v.belop
                )
           END as utbetaling_belop,
       v.vedtak_resultat,
       v.kommentar_vedtaksbrev,
       v.utbetaling_mottaker,
       v.kid,
       v.kommentar_intern,
       v.vedtak_journalpost_id,
       v.vedtak_journalpost_distribuering_id,
       v.vedtak_journalfort_tidspunkt,
       v.vedtak_distribuert_tidspunkt
from tilskudd_vedtak v
    inner join nav_enhet on nav_enhet.enhetsnummer = v.kostnadssted
