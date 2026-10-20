package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.admin.journalpost.ForventetBruker
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest.TilskuddRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.Validated
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object TilskuddBehandlingValidator {
    fun validate(
        request: TilskuddBehandlingRequest,
        gjennomforing: Gjennomforing,
        gyldigTilsagnPeriode: Periode?,
        arrangorBetalingsinformasjon: Betalingsinformasjon?,
        behandlendeEnhet: NavEnhetNummer,
        journalpostValidator: (String, Int) -> Validated<JournalpostId>,
    ): Validated<TilskuddBehandling> = validation {
        val tilskudd = request.tilskudd.mapIndexed { index, v ->
            validateTilskuddRequest(
                req = v,
                index = index,
                gjennomforing = gjennomforing,
                gyldigTilsagnPeriode = gyldigTilsagnPeriode,
                arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
                journalpostValidator,
            ).bind()
        }

        TilskuddBehandling(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            tilskudd = tilskudd,
            status = TilskuddBehandlingStatus.TIL_GODKJENNING,
            type = TilskuddBehandlingType.REGISTRERING,
            behandlendeEnhet = behandlendeEnhet,
        )
    }

    fun validateTilskuddRequest(
        req: TilskuddRequest,
        index: Int,
        gjennomforing: Gjennomforing,
        gyldigTilsagnPeriode: Periode?,
        arrangorBetalingsinformasjon: Betalingsinformasjon?,
        journalpostValidator: (String, Int) -> Validated<JournalpostId>,
    ): Validated<TilskuddVedtak> = validation {
        validateNotNull(req.kostnadssted) {
            FieldError("/tilskudd/$index/kostnadssted", "Kostnadssted er påkrevd")
        }
        val periodeStart = req.periodeStart?.parseOrNull()
        validateNotNull(periodeStart) {
            FieldError("/tilskudd/$index/periodeStart", "Periodestart er påkrevd")
        }
        val periodeSlutt = req.periodeSlutt?.parseOrNull()
        validateNotNull(periodeSlutt) {
            FieldError("/tilskudd/$index/periodeSlutt", "Periodeslutt er påkrevd")
        }
        if (periodeStart != null && periodeSlutt != null) {
            validate(periodeStart.year <= LocalDate.now().year) {
                FieldError(
                    "/tilskudd/$index/periodeStart",
                    "Periodestart kan ikke være etter siste dag i inneværende år",
                )
            }
            validate(periodeSlutt.year <= LocalDate.now().year) {
                FieldError(
                    "/tilskudd/$index/periodeSlutt",
                    "Periodeslutt kan ikke være etter siste dag i inneværende år",
                )
            }
            validate(periodeStart.year == periodeSlutt.year) {
                FieldError(
                    pointer = "/tilskudd/$index/periodeSlutt",
                    detail = "Periode kan ikke strekke seg over årsskiftet",
                )
            }
            validate(!periodeStart.isAfter(periodeSlutt)) {
                FieldError("/tilskudd/$index/periodeStart", "Periodestart må være før sluttdato")
            }
            validate(gjennomforing.sluttDato == null || !periodeSlutt.isAfter(gjennomforing.sluttDato)) {
                FieldError(
                    "/tilskudd/$index/periodeSlutt",
                    "Periodeslutt kan ikke være etter gjennomføringsperioden",
                )
            }
            if (req.vedtakResultat == VedtakResultat.INNVILGELSE && req.utbetalingMottaker == TilskuddMottaker.ARRANGOR) {
                requireNotNull(gyldigTilsagnPeriode) {
                    FieldError(
                        pointer = "/tilskudd/$index/periodeStart",
                        detail = "Tilsagn for tiltakstype ${gjennomforing.tiltakstype.navn} er ikke støttet enda",
                    )
                }
                validate(!periodeStart.isBefore(gyldigTilsagnPeriode.start)) {
                    val dato = gyldigTilsagnPeriode.start.formaterDatoTilEuropeiskDatoformat()
                    FieldError(
                        pointer = "/tilskudd/$index/periodeStart",
                        detail = "Minimum startdato for tilsagn til ${gjennomforing.tiltakstype.navn} er $dato",
                    )
                }
                validate(!periodeSlutt.isAfter(gyldigTilsagnPeriode.getLastInclusiveDate())) {
                    val dato = gyldigTilsagnPeriode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                    FieldError(
                        pointer = "/tilskudd/$index/periodeSlutt",
                        detail = "Maksimum sluttdato for tilsagn til ${gjennomforing.tiltakstype.navn} er $dato",
                    )
                }
            }
        }
        validateNotNull(req.soknadDato) {
            FieldError("/tilskudd/$index/soknadDato", "Søknadsdato er påkrevd")
        }
        req.soknadDato?.let { soknadDato ->
            validate(!soknadDato.isAfter(LocalDate.now())) {
                FieldError("/tilskudd/$index/soknadDato", "Søknadsdato kan ikke være frem i tid")
            }
        }
        validateNotNull(req.soknadJournalpostId) {
            FieldError("/tilskudd/$index/soknadJournalpostId", "JournalpostId er påkrevd")
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
        if (req.utbetalingMottaker == TilskuddMottaker.ARRANGOR) {
            validateNotNull(arrangorBetalingsinformasjon) {
                FieldError(
                    "/tilskudd/$index/utbetalingMottaker",
                    "Betalingsinformasjon for arrangøren må være registrert",
                )
            }
        }
        if (req.vedtakResultat == VedtakResultat.INNVILGELSE && req.utbetalingMottaker == TilskuddMottaker.ARRANGOR) {
            validate(!gjennomforing.arrangor.slettet) {
                FieldError(
                    "/tilskudd/$index/utbetalingMottaker",
                    "Tilskudd kan ikke opprettes fordi arrangøren er slettet i Brreg",
                )
            }
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
                "Beløp fra faktura må være positivt",
            )
        }
        if (req.vedtakResultat == VedtakResultat.INNVILGELSE) {
            validate(req.belop != null && req.belop > 0) {
                FieldError(
                    "/tilskudd/$index/belop",
                    "Beløp til utbetaling må være positivt",
                )
            }
        }

        requireValid(periodeStart == null || !periodeStart.isAfter(periodeSlutt))
        val periode = Periode.fromInclusiveDates(requireNotNull(periodeStart), requireNotNull(periodeSlutt))

        requireNotNull(req.soknadDato)
        requireNotNull(req.soknadJournalpostId)
        requireNotNull(req.kostnadssted)
        requireNotNull(req.tilskuddOpplaeringType)
        requireNotNull(req.vedtakResultat)
        requireNotNull(req.utbetalingMottaker)
        requireNotNull(req.soknadBelop?.belop)

        val jId = journalpostValidator(req.soknadJournalpostId, index).bind()

        TilskuddVedtak(
            id = req.id,
            tilskuddId = req.tilskuddId,
            tilskuddOpplaeringType = req.tilskuddOpplaeringType,
            soknadJournalpostId = jId,
            soknadDato = req.soknadDato,
            soknadBelop = ValutaBelop(req.soknadBelop.belop, Valuta.NOK),
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

    fun createJournalpostValidator(
        forventetBruker: ForventetBruker,
        journalpostValidator: JournalpostValidator,
        valideringEnabled: Boolean,
    ): (String, Int) -> Validated<JournalpostId> {
        if (!valideringEnabled) {
            return { journalpostId: String, index: Int ->
                JournalpostId.parse(journalpostId)
                    ?.right()
                    ?: FieldError(
                        pointer = "/tilskudd/$index/soknadJournalpostId",
                        detail = "Feil format på Journalpost-ID: $journalpostId",
                    ).nel().left()
            }
        }
        val journalpostValidatorFunc: (String, Int) -> Validated<JournalpostId> = { journalpostId: String, index: Int ->
            runBlocking {
                journalpostValidator.validerJournalpost(
                    journalpostId = journalpostId,
                    forventetBruker = forventetBruker,
                    accessType = AccessType.M2M,
                )
                    .mapLeft {
                        when (it) {
                            JournalpostValidator.JournalpostValideringError.NotFound ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Fant ingen journalpost med id $journalpostId",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.SafError ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Klarte ikke å slå opp journalpost",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.FeilFormat ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Feil format på Journalpost-ID: $journalpostId",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerVirksomhet ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en virksomhet",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerPerson ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en person",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerAnnenPerson ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en annen person enn deltakeren",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerAnnenVirksomhet ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en annen virksomhet",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.KunneIkkeVeksleAktoerId ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Kunne ikke slå opp person i PDL for å verifisere journalposten",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.IngenTilknytning ->
                                FieldError(
                                    pointer = "/tilskudd/$index/soknadJournalpostId",
                                    detail = "Journalposten var ikke tilknyttet en bruker eller virksomhet",
                                ).nel()
                        }
                    }
            }
        }
        return journalpostValidatorFunc
    }
}
