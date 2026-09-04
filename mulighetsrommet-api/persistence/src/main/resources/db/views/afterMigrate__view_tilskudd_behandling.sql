create or replace view view_tilskudd_behandling as
select tb.id,
       tb.status,
       tb.gjennomforing_id,
       tb.soknad_journalpost_id,
       vedtak_json
from tilskudd_behandling tb
         left join lateral (
    select coalesce(jsonb_agg(
                            jsonb_build_object(
                                    'id', v.id,
                                    'soknad_dato',v.soknad_dato,
                                    'periode', v.periode,
                                    'kostnadssted_enhetsnummer', v.kostnadssted,
                                    'kostnadssted_navn', nav_enhet.navn,
                                    'tilskuddOpplaeringType', tilskudd_opplaering.kode,
                                    'soknadBelop', jsonb_build_object(
                                            'valuta', v.soknad_valuta,
                                            'belop', v.soknad_belop
                                                   ),
                                    'utbetalingBelop', CASE
                                                           WHEN v.belop IS NULL THEN NULL
                                                           ELSE jsonb_build_object(
                                                                   'valuta', v.valuta,
                                                                   'belop', v.belop
                                                                )
                                        END,
                                    'vedtakResultat', jsonb_build_object('type', v.vedtak_resultat),
                                    'kommentarVedtaksbrev', v.kommentar_vedtaksbrev,
                                    'utbetalingMottaker', v.utbetaling_mottaker,
                                    'kid', v.kid,
                                    'kommentarIntern', v.kommentar_intern,
                                    'vedtakJournalpostId', v.vedtak_journalpost_id,
                                    'vedtakJournalpostDistribueringId', v.vedtak_journalpost_distribuering_id,
                                    'vedtakJournalfortTidspunkt', v.vedtak_journalfort_tidspunkt,
                                    'vedtakDistribuertTidspunkt', v.vedtak_distribuert_tidspunkt
                            )
                    ), '[]') as vedtak_json
    from tilskudd_vedtak v
        inner join nav_enhet on nav_enhet.enhetsnummer = v.kostnadssted
        inner join tilskudd on v.tilskudd_id = tilskudd.id
        inner join tilskudd_opplaering on tilskudd_opplaering.id = tilskudd.tilskudd_opplaering_id
    where v.tilskudd_behandling_id = tb.id) on true;
