package no.nav.mulighetsrommet.api.tilskuddbehandling

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest.TilskuddRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.Validated
import no.nav.mulighetsrommet.validation.validation
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TilskuddBehandlingValidator {
    fun validate(
        request: TilskuddBehandlingRequest,
        gjennomforing: Gjennomforing,
        behandlendeEnhet: NavEnhetNummer,
        journalpostValidator: (String, Int) -> Validated<JournalpostId>,
    ): Validated<TilskuddBehandling> = validation {
        val tilskudd = request.tilskudd.mapIndexed { index, v ->
            validateTilskuddRequest(
                req = v,
                index = index,
                gjennomforing = gjennomforing,
                journalpostValidator,
            ).bind()
        }

        TilskuddBehandling(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            tilskudd = tilskudd,
            status = TilskuddBehandlingStatus.TIL_ATTESTERING,
            type = TilskuddBehandlingType.REGISTRERING,
            behandlendeEnhet = behandlendeEnhet,
        )
    }

    fun validateTilskuddRequest(
        req: TilskuddRequest,
        index: Int,
        gjennomforing: Gjennomforing,
        journalpostValidator: (String, Int) -> Validated<JournalpostId>,
    ): Validated<TilskuddVedtak> = validation {
        validateNotNull(req.kostnadssted) {
            FieldError("Kostnadssted er påkrevd", "/tilskudd/$index/kostnadssted")
        }
        val periodeStart = req.periodeStart?.parseOrNull()
        validateNotNull(periodeStart) {
            FieldError("Periodestart må være satt", "/tilskudd/$index/periodeStart")
        }
        val periodeSlutt = req.periodeSlutt?.parseOrNull()
        validateNotNull(periodeSlutt) {
            FieldError("Periodeslutt må være satt", "/tilskudd/$index/periodeSlutt")
        }
        validateNotNull(req.soknadDato) {
            FieldError("Søknadsdato må være satt", "/tilskudd/$index/soknadDato")
        }
        validateNotNull(req.soknadJournalpostId) {
            FieldError("JournalpostId må være satt", "/tilskudd/$index/soknadJournalpostId")
        }
        requireValid(req.soknadDato != null && req.soknadJournalpostId != null && req.kostnadssted != null && periodeStart != null && periodeSlutt != null)
        requireValid(!periodeStart.isAfter(periodeSlutt)) {
            FieldError("Periodestart må være før slutt", "/tilskudd/$index/periodeStart")
        }
        validate(gjennomforing.sluttDato == null || !periodeSlutt.isAfter(gjennomforing.sluttDato)) {
            FieldError(
                "Sluttdato kan ikke være etter gjennomføringsperioden",
                "/tilskudd/$index/periodeSlutt",
            )
        }
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
        validate(req.soknadBelop?.belop != null && req.soknadBelop.belop > 0) {
            FieldError(
                "/tilskudd/$index/soknadBelop/belop",
                "Søknadsbeløp må være positivt",
            )
        }
        validateNotNull(req.soknadBelop?.valuta) {
            FieldError(
                "/tilskudd/$index/soknadBelop/valuta",
                "Søknadsvaluta må være positivt",
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
        val periode = Periode.fromInclusiveDates(requireNotNull(periodeStart), requireNotNull(periodeSlutt))

        val jId = journalpostValidator(req.soknadJournalpostId, index).bind()

        TilskuddVedtak(
            id = req.id,
            tilskuddId = req.tilskuddId,
            tilskuddOpplaeringType = req.tilskuddOpplaeringType,
            soknadJournalpostId = jId,
            soknadDato = req.soknadDato,
            soknadBelop = ValutaBelop(req.soknadBelop.belop, req.soknadBelop.valuta),
            periode = periode,
            kostnadssted = req.kostnadssted,
            vedtakResultat = req.vedtakResultat,
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
            kommentarIntern = req.kommentarIntern,
            kommentarVedtaksbrev = req.kommentarVedtaksbrev,
        )
    }
}
