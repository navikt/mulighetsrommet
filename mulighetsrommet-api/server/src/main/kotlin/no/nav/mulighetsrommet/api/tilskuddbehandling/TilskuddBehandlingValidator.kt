package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.admin.journalpost.ForventetBruker
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.BrukerUtbetalingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddVedtakRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.utils.DatoUtils.parseOrNull
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate

data class TilskuddSaksopplysninger(
    val periode: Periode,
    val soknadDato: LocalDate,
    val soknadJournalpostId: JournalpostId,
    val kostnadssted: NavEnhetNummer,
    val tilskuddOpplaeringType: Opplaeringtilskudd.Kode,
    val soknadBelop: Int,
    val utbetalingMottaker: TilskuddMottaker,
    val kid: Kid?,
    val brukerUtbetalingType: BrukerUtbetalingType?,
)

object TilskuddBehandlingValidator {
    data class Ctx(
        val gjennomforing: Gjennomforing,
        val betalingsinformasjonArrangor: Betalingsinformasjon?,
        val journalpostValidator: (String, Int) -> Validated<JournalpostId>,
        val gyldigTilsagnPeriode: Periode?,
    )

    fun validate(
        request: TilskuddBehandlingRequest,
        behandlendeEnhet: NavEnhetNummer,
        ctx: Ctx,
    ): Validated<TilskuddBehandling> = validation {
        val tilskudd = request.tilskuddVedtak.mapIndexed { index, v ->
            validateTilskuddRequest(
                req = v,
                index = index,
                ctx = ctx,
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
        req: TilskuddVedtakRequest,
        index: Int,
        ctx: Ctx,
    ): Validated<TilskuddVedtak> = validation {
        validateNotNull(req.vedtakResultat) {
            FieldError(
                "/tilskuddVedtak/$index/vedtakResultat",
                "Du må velge et resultat",
            )
        }

        validate((req.kommentarVedtaksbrev?.length ?: 0) <= 500) {
            FieldError(
                "/tilskuddVedtak/$index/kommentarVedtaksbrev",
                "Kommentar kan ikke inneholde mer enn 500 tegn",
            )
        }

        if (req.vedtakResultat == VedtakResultat.INNVILGELSE) {
            validate(req.belop != null && req.belop > 0) {
                FieldError(
                    "/tilskuddVedtak/$index/belop",
                    "Beløp til utbetaling må være positivt",
                )
            }
        }
        requireNotNull(req.vedtakResultat)
        val saksopplysninger = validateSaksopplysningTilskuddVedtak(req, index, ctx).bind()
        TilskuddVedtak(
            id = req.id,
            tilskuddId = req.tilskuddId,
            tilskuddOpplaeringType = saksopplysninger.tilskuddOpplaeringType,
            soknadJournalpostId = saksopplysninger.soknadJournalpostId,
            soknadDato = saksopplysninger.soknadDato,
            soknadBelop = ValutaBelop(saksopplysninger.soknadBelop, Valuta.NOK),
            periode = saksopplysninger.periode,
            kostnadssted = saksopplysninger.kostnadssted,
            utbetalingMottaker = saksopplysninger.utbetalingMottaker,
            kid = saksopplysninger.kid,
            brukerUtbetalingType = saksopplysninger.brukerUtbetalingType,
            vedtakResultat = req.vedtakResultat,
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

    fun validateSaksopplysninger(
        request: TilskuddBehandlingRequest,
        ctx: Ctx,
    ): Validated<Unit> = validation {
        request.tilskuddVedtak.forEachIndexed { index, req ->
            validateSaksopplysningTilskuddVedtak(
                req = req,
                index = index,
                ctx,
            ).fold(
                ifLeft = { fieldErrors -> fieldErrors.forEach { fieldError -> error { fieldError } } },
                ifRight = {},
            )
        }
    }

    fun validateSaksopplysningTilskuddVedtak(
        req: TilskuddVedtakRequest,
        index: Int,
        ctx: Ctx,
    ): Validated<TilskuddSaksopplysninger> = validation {
        validateNotNull(req.kostnadssted) {
            FieldError("/tilskuddVedtak/$index/kostnadssted", "Kostnadssted er påkrevd")
        }

        val periodeStart = req.periodeStart?.parseOrNull()
        validateNotNull(periodeStart) {
            FieldError("/tilskuddVedtak/$index/periodeStart", "Periodestart er påkrevd")
        }
        val periodeSlutt = req.periodeSlutt?.parseOrNull()
        validateNotNull(periodeSlutt) {
            FieldError("/tilskuddVedtak/$index/periodeSlutt", "Periodeslutt er påkrevd")
        }
        if (periodeStart != null && periodeSlutt != null) {
            validate(periodeStart.year <= LocalDate.now().year) {
                FieldError(
                    "/tilskuddVedtak/$index/periodeStart",
                    "Periodestart kan ikke være etter siste dag i inneværende år",
                )
            }
            validate(periodeSlutt.year <= LocalDate.now().year) {
                FieldError(
                    "/tilskuddVedtak/$index/periodeSlutt",
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
                FieldError("/tilskuddVedtak/$index/periodeStart", "Periodestart må være før sluttdato")
            }
            validate(ctx.gjennomforing.sluttDato == null || !periodeSlutt.isAfter(ctx.gjennomforing.sluttDato)) {
                FieldError(
                    "/tilskuddVedtak/$index/periodeSlutt",
                    "Periodeslutt kan ikke være etter gjennomføringsperioden",
                )
            }
            if (req.vedtakResultat == VedtakResultat.INNVILGELSE && req.utbetalingMottaker == TilskuddMottaker.ARRANGOR) {
                requireNotNull(ctx.gyldigTilsagnPeriode) {
                    FieldError(
                        pointer = "/tilskudd/$index/periodeStart",
                        detail = "Tilsagn for tiltakstype ${ctx.gjennomforing.tiltakstype.navn} er ikke støttet enda",
                    )
                }
                validate(!periodeStart.isBefore(ctx.gyldigTilsagnPeriode.start)) {
                    val dato = ctx.gyldigTilsagnPeriode.start.formaterDatoTilEuropeiskDatoformat()
                    FieldError(
                        pointer = "/tilskudd/$index/periodeStart",
                        detail = "Minimum startdato for tilsagn til ${ctx.gjennomforing.tiltakstype.navn} er $dato",
                    )
                }
                validate(!periodeSlutt.isAfter(ctx.gyldigTilsagnPeriode.getLastInclusiveDate())) {
                    val dato = ctx.gyldigTilsagnPeriode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
                    FieldError(
                        pointer = "/tilskudd/$index/periodeSlutt",
                        detail = "Maksimum sluttdato for tilsagn til ${ctx.gjennomforing.tiltakstype.navn} er $dato",
                    )
                }
                validate(!ctx.gjennomforing.arrangor.slettet) {
                    FieldError(
                        "/tilskudd/$index/utbetalingMottaker",
                        "Tilskudd kan ikke opprettes fordi arrangøren er slettet i Brreg",
                    )
                }
            }
        }
        validateNotNull(req.soknadDato) {
            FieldError("/tilskuddVedtak/$index/soknadDato", "Søknadsdato er påkrevd")
        }
        req.soknadDato?.let { soknadDato ->
            validate(!soknadDato.isAfter(LocalDate.now())) {
                FieldError("/tilskudd/$index/soknadDato", "Søknadsdato kan ikke være frem i tid")
            }
        }
        validateNotNull(req.soknadJournalpostId) {
            FieldError("/tilskuddVedtak/$index/soknadJournalpostId", "JournalpostId er påkrevd")
        }
        validateNotNull(req.tilskuddOpplaeringType) {
            FieldError(
                "/tilskuddVedtak/$index/tilskuddOpplaeringType",
                "Du må velge en tilskuddstype",
            )
        }
        validateNotNull(req.utbetalingMottaker) {
            FieldError(
                "/tilskuddVedtak/$index/utbetalingMottaker",
                "Du må velge en mottaker",
            )
        }
        if (req.utbetalingMottaker == TilskuddMottaker.ARRANGOR) {
            validateNotNull(ctx.betalingsinformasjonArrangor) {
                FieldError(
                    "/tilskuddVedtak/$index/utbetalingMottaker",
                    "Betalingsinformasjon for arrangøren må være registrert",
                )
            }
            req.kidNummer?.let { value ->
                validateNotNull(Kid.parse(value)) {
                    FieldError(
                        "/tilskuddVedtak/$index/kidNummer",
                        "Ugyldig kid",
                    )
                }
            }
        }
        if (req.utbetalingMottaker == TilskuddMottaker.BRUKER) {
            validateNotNull(req.brukerUtbetalingType) {
                FieldError(
                    "/tilskuddVedtak/$index/brukerUtbetalingType",
                    "Du må velge forskudd eller refusjon",
                )
            }
        }

        validate(req.soknadBelop?.belop != null && req.soknadBelop.belop > 0) {
            FieldError(
                "/tilskuddVedtak/$index/soknadBelop/belop",
                "Beløp fra faktura må være positivt",
            )
        }

        requireValid(periodeStart == null || !periodeStart.isAfter(periodeSlutt))
        val periode = Periode.fromInclusiveDates(
            requireNotNull(periodeStart),
            requireNotNull(periodeSlutt),
        )

        requireNotNull(req.soknadDato)
        requireNotNull(req.soknadJournalpostId)
        requireNotNull(req.kostnadssted)
        requireNotNull(req.tilskuddOpplaeringType)
        requireNotNull(req.utbetalingMottaker)
        requireNotNull(req.soknadBelop?.belop)

        val jId = ctx.journalpostValidator(req.soknadJournalpostId, index).bind()

        TilskuddSaksopplysninger(
            periode = periode,
            soknadDato = req.soknadDato,
            soknadJournalpostId = jId,
            kostnadssted = req.kostnadssted,
            tilskuddOpplaeringType = req.tilskuddOpplaeringType,
            soknadBelop = req.soknadBelop.belop,
            utbetalingMottaker = req.utbetalingMottaker,
            kid = req.kidNummer?.let { Kid.parse(it) }.takeIf { req.utbetalingMottaker == TilskuddMottaker.ARRANGOR },
            brukerUtbetalingType = req.brukerUtbetalingType.takeIf { req.utbetalingMottaker == TilskuddMottaker.BRUKER },
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
                        pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
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
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Fant ingen journalpost med id $journalpostId",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.SafError ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Klarte ikke å slå opp journalpost",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.FeilFormat ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Feil format på Journalpost-ID: $journalpostId",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerVirksomhet ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en virksomhet",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerPerson ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en person",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerAnnenPerson ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en annen person enn deltakeren",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.TilhorerAnnenVirksomhet ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Journalposten tilhører en annen virksomhet",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.KunneIkkeVeksleAktoerId ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Kunne ikke slå opp person i PDL for å verifisere journalposten",
                                ).nel()

                            JournalpostValidator.JournalpostValideringError.IngenTilknytning ->
                                FieldError(
                                    pointer = "/tilskuddVedtak/$index/soknadJournalpostId",
                                    detail = "Journalposten var ikke tilknyttet en bruker eller virksomhet",
                                ).nel()
                        }
                    }
            }
        }
        return journalpostValidatorFunc
    }
}
