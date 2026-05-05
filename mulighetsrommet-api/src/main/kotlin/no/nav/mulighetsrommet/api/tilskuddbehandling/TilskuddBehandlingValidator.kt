package no.nav.mulighetsrommet.api.tilskuddbehandling

import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TilskuddBehandlingValidator {
    fun validate(request: TilskuddBehandlingRequest): Validated<TilskuddBehandlingDbo> = validation {
        validateNotNull(request.kostnadssted) {
            FieldError.of("Kostnadssted er påkrevd", TilskuddBehandlingRequest::kostnadssted)
        }
        val periodeStart = request.periodeStart?.parseOrNull()
        validateNotNull(periodeStart) {
            FieldError.of("Periodestart må være satt", TilsagnRequest::periodeStart)
        }
        val periodeSlutt = request.periodeSlutt?.parseOrNull()
        validateNotNull(periodeSlutt) {
            FieldError.of("Periodeslutt må være satt", TilskuddBehandlingRequest::periodeSlutt)
        }
        validateNotNull(request.soknadDato) {
            FieldError.of("Søknadsdato må være satt", TilskuddBehandlingRequest::soknadDato)
        }
        validateNotNull(request.soknadJournalpostId) {
            FieldError.of("JournalpostId må være satt", TilskuddBehandlingRequest::soknadJournalpostId)
        }
        val tilskudd = request.tilskudd.mapIndexed { index, v ->
            validateTilskuddRequest(v, index).bind()
        }
        requireValid(request.soknadDato != null && request.soknadJournalpostId != null && request.kostnadssted != null && periodeStart != null && periodeSlutt != null)
        requireValid(periodeStart.isBefore(periodeSlutt)) {
            FieldError.of("Periodestart må være før slutt", TilsagnRequest::periodeStart)
        }

        TilskuddBehandlingDbo(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            soknadJournalpostId = request.soknadJournalpostId,
            soknadDato = request.soknadDato,
            periode = Periode(periodeStart, periodeSlutt),
            kostnadssted = request.kostnadssted,
            tilskudd = tilskudd,
            status = TilskuddBehandlingStatus.TIL_ATTESTERING,
            kommentarIntern = request.kommentarIntern,
        )
    }

    fun validateTilskuddRequest(req: TilskuddBehandlingRequest.TilskuddRequest, index: Int): Validated<TilskuddDbo> = validation {
        validateNotNull(req.tilskuddOpplaeringType) {
            FieldError(
                "/tilskudd/$index/tilskuddOpplaeringType",
                "Du må velge en tilskuddstype",
            )
        }
        validateNotNull(req.vedtakResultat) {
            FieldError(
                "/tilskudd/$index/vedtakResultat",
                "Du må velge et resultat",
            )
        }
        validateNotNull(req.utbetalingMottaker) {
            FieldError(
                "/tilskudd/$index/utbetalingMottaker",
                "Du må velge en mottaker",
            )
        }
        validate((req.kommentarVedtaksbrev?.length ?: 0) <= 500) {
            FieldError(
                "/tilskudd/$index/kommentarVedtaksbrev",
                "Kommentar kan ikke inneholde mer enn 500 tegn",
            )
        }
        val kid = req.kidNummer?.let { value ->
            validateNotNull(Kid.parse(value)) {
                FieldError(
                    "/tilskudd/$index/kidNummer",
                    "Ugyldig kid",
                )
            }
        }
        validate(req.soknadBelop?.belop != null && req.soknadBelop.belop > 0 && req.soknadBelop.valuta != null) {
            FieldError(
                "/tilskudd/$index/soknadBelop/belop",
                "Søknadsbeløp må være positivt",
            )
        }
        if (req.vedtakResultat == VedtakResultat.INNVILGELSE) {
            validate(req.belop != null && req.belop > 0) {
                FieldError(
                    "/tilskudd/$index/belop",
                    "Beløp må være positivt",
                )
            }
        }
        requireValid(req.soknadBelop?.belop != null && req.soknadBelop.valuta != null && req.vedtakResultat != null && req.utbetalingMottaker != null && req.tilskuddOpplaeringType != null)
        requireValid(req.vedtakResultat != VedtakResultat.INNVILGELSE || req.belop != null)

        TilskuddDbo(
            id = req.id,
            tilskuddOpplaeringType = req.tilskuddOpplaeringType,
            soknadBelop = ValutaBelop(req.soknadBelop.belop, req.soknadBelop.valuta),
            vedtakResultat = req.vedtakResultat,
            kommentarVedtaksbrev = req.kommentarVedtaksbrev,
            utbetalingMottaker = req.utbetalingMottaker,
            kid = kid,
            utbetalingBelop = if (req.vedtakResultat == VedtakResultat.INNVILGELSE) {
                ValutaBelop(
                    requireNotNull(req.belop),
                    Valuta.NOK,
                )
            } else {
                null
            },
        )
    }
}
