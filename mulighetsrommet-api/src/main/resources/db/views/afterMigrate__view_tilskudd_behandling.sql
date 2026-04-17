create or replace view view_tilskudd_behandling as
select
    tb.id,
    tb.gjennomforing_id,
    tb.soknad_journalpost_id,
    tb.soknad_dato,
    tb.periode,
    tb.kostnadssted,
    vedtak_json
from tilskudd_behandling tb
    left join lateral (
        select coalesce(jsonb_agg(
            jsonb_build_object(
                'id', v.id,
                'tilskuddType', v.tilskudd_type,
                'soknadBelop', v.soknad_belop,
                'soknadValuta', v.soknad_valuta,
                'vedtakResultat', v.vedtak_resultat,
                'kommentarVedtaksbrev', v.kommentar_vedtaksbrev,
                'utbetalingMottaker', v.utbetaling_mottaker
            )
        ), '[]') as vedtak_json from tilskudd_vedtak v
        where v.tilskudd_behandling_id = tb.id) on true;
