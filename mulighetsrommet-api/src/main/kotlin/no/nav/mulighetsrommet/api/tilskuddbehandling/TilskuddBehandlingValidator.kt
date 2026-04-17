package no.nav.mulighetsrommet.api.tilskuddbehandling

import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtakDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Periode
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TilskuddBehandlingValidator {
    fun validate(request: TilskuddBehandlingRequest): Validated<TilskuddBehandlingDbo> = validation {
        val kostnadssted = validateNotNull(request.kostnadssted) {
            FieldError.of("Kostnadssted er påkrevd", TilskuddBehandlingRequest::kostnadssted)
        }

        val periodeStart = request.periodeStart?.parseOrNull()
        validateNotNull(periodeStart) {
            FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart)
        }
        val periodeSlutt = request.periodeSlutt?.parseOrNull()
        validateNotNull(periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", TilsagnRequest::periodeSlutt)
        }
        requireValid(requireNotNull(periodeStart) < requireNotNull(periodeSlutt)) {
            FieldError.of("Periodestart må være før periodeslutt", TilskuddBehandlingRequest::periodeStart)
        }

        TilskuddBehandlingDbo(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            soknadJournalpostId = request.soknadJournalpostId,
            soknadDato = request.soknadDato,
            periode = Periode(periodeStart, periodeSlutt),
            kostnadssted = requireNotNull(kostnadssted),
            vedtak = request.vedtak.mapIndexed { index, v ->
                validateVedtakRequest(v, index).bind()
            },
        )
    }

    fun validateVedtakRequest(req: TilskuddBehandlingRequest.TilskuddVedtakRequest, index: Int): Validated<TilskuddVedtakDbo> = validation {
        requireValid(req.soknadBelop?.belop != null && req.soknadBelop.belop > 0 && req.soknadBelop.valuta != null) {
            FieldError(
                "/vedtak/$index/soknadBelop",
                "Beløp må være positivt",
            )
        }

        TilskuddVedtakDbo(
            id = req.id,
            tilskuddType = req.tilskuddType,
            soknadBelop = req.soknadBelop.belop,
            soknadValuta = req.soknadBelop.valuta,
            vedtakResultat = req.vedtakResultat,
            kommentarVedtaksbrev = req.kommentarVedtaksbrev,
            utbetalingMottaker = req.utbetalingMottaker,
        )
    }
}
