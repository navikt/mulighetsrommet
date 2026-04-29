create or replace view view_tilskudd_behandling as
select
    tb.id,
    tb.gjennomforing_id,
    tb.soknad_journalpost_id,
    tb.soknad_dato,
    tb.periode,
    tb.kostnadssted,
    vedtak_json,
    tb.status
from tilskudd_behandling tb
    left join lateral (
        select coalesce(jsonb_agg(
            jsonb_build_object(
                'id', v.id,
                'tilskuddOpplaeringType', tilskudd_opplaering.kode,
                'soknadBelop', v.soknad_belop,
                'soknadValuta', v.soknad_valuta,
                'vedtakResultat', v.vedtak_resultat,
                'kommentarVedtaksbrev', v.kommentar_vedtaksbrev,
                'utbetalingMottaker', v.utbetaling_mottaker
            )
        ), '[]') as vedtak_json from tilskudd v
            inner join tilskudd_opplaering on tilskudd_opplaering.id = v.tilskudd_opplaering_id
        where v.tilskudd_behandling_id = tb.id) on true;
