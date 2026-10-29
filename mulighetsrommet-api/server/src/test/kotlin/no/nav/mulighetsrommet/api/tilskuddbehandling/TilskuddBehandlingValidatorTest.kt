package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingValidatorTest : FunSpec({
    val periodeStart = LocalDate.of(2025, 1, 1)
    val periodeSlutt = LocalDate.of(2025, 7, 1)
    val gjennomforingSluttDato = LocalDate.of(2025, 12, 31)
    val gyldigTilsagnPeriode = Periode.fromInclusiveDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
    val behandlendeEnhet = NavEnhetNummer("0502")
    val arrangorBetalingsinformasjon = mockk<Betalingsinformasjon>(relaxed = true)
    val tiltakstypeNavn = "Arbeidsforberedende trening"

    fun createGjennomforing(sluttDato: LocalDate? = gjennomforingSluttDato): Gjennomforing = mockk<Gjennomforing>().also { gjennomforing ->
        every { gjennomforing.sluttDato } returns sluttDato
        every { gjennomforing.tiltakstype.navn } returns tiltakstypeNavn
        every { gjennomforing.arrangor.slettet } returns false
    }

    fun createCtx(
        gjennomforing: Gjennomforing = createGjennomforing(),
        betalingsinformasjonArrangor: Betalingsinformasjon? = arrangorBetalingsinformasjon,
        journalpostValidator: (String, Int) -> Validated<JournalpostId> = { journalpostId, _ -> JournalpostId(journalpostId).right() },
        tilsagnPeriode: Periode = gyldigTilsagnPeriode,
    ) = TilskuddBehandlingValidator.Ctx(
        gjennomforing = gjennomforing,
        betalingsinformasjonArrangor = betalingsinformasjonArrangor,
        journalpostValidator = journalpostValidator,
        gyldigTilsagnPeriode = tilsagnPeriode,
    )

    fun validTilskuddVedtakRequest() = TilskuddVedtakRequest(
        id = UUID.randomUUID(),
        tilskuddId = UUID.randomUUID(),
        tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
        soknadJournalpostId = "12345",
        soknadDato = LocalDate.of(2024, 1, 15),
        soknadBelop = ValutaBelopRequest(belop = 1200, valuta = Valuta.SEK),
        vedtakResultat = VedtakResultat.INNVILGELSE,
        kommentarVedtaksbrev = "Vedtakskommentar",
        utbetalingMottaker = TilskuddMottaker.ARRANGOR,
        kidNummer = "116",
        brukerUtbetalingType = null,
        belop = 2000,
        periodeStart = periodeStart.toString(),
        periodeSlutt = periodeSlutt.toString(),
        kostnadssted = behandlendeEnhet,
        kommentarIntern = "Intern kommentar",
    )

    fun createRequest(vararg tilskuddVedtak: TilskuddVedtakRequest) = TilskuddBehandlingRequest(
        id = UUID.randomUUID(),
        gjennomforingId = UUID.randomUUID(),
        tilskuddVedtak = tilskuddVedtak.toList(),
    )

    test("validerer og mapper en gyldig tilskuddsbehandling") {
        val request = createRequest(validTilskuddVedtakRequest())

        TilskuddBehandlingValidator.validate(
            request = request,
            behandlendeEnhet = behandlendeEnhet,
            ctx = createCtx(),
        ) shouldBeRight TilskuddBehandling(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            tilskudd = listOf(
                TilskuddVedtak(
                    id = request.tilskuddVedtak.single().id,
                    tilskuddId = request.tilskuddVedtak.single().tilskuddId,
                    tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                    soknadJournalpostId = JournalpostId("12345"),
                    soknadDato = LocalDate.of(2024, 1, 15),
                    soknadBelop = ValutaBelop(1200, Valuta.NOK),
                    periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
                    kostnadssted = behandlendeEnhet,
                    vedtakResultat = VedtakResultat.INNVILGELSE,
                    utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                    kid = Kid.parse("116"),
                    brukerUtbetalingType = null,
                    utbetalingBelop = ValutaBelop(2000, Valuta.NOK),
                    kommentarIntern = "Intern kommentar",
                    kommentarVedtaksbrev = "Vedtakskommentar",
                ),
            ),
            status = TilskuddBehandlingStatus.TIL_GODKJENNING,
            type = TilskuddBehandlingType.REGISTRERING,
            behandlendeEnhet = behandlendeEnhet,
        )
    }

    test("avslag trenger ikke beløp til utbetaling") {
        val request = validTilskuddVedtakRequest().copy(
            vedtakResultat = VedtakResultat.AVSLAG,
            belop = null,
        )

        val result = TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(),
        ).shouldBeRight()

        result.vedtakResultat shouldBe VedtakResultat.AVSLAG
        result.utbetalingBelop shouldBe null
    }

    test("samler feltfeil for ugyldig tilskuddsrequest") {
        val request = validTilskuddVedtakRequest().copy(
            tilskuddOpplaeringType = null,
            soknadJournalpostId = null,
            soknadDato = null,
            soknadBelop = ValutaBelopRequest(belop = 0, valuta = Valuta.NOK),
            kommentarVedtaksbrev = "x".repeat(501),
            utbetalingMottaker = null,
            kidNummer = "ugyldig",
            belop = 0,
            periodeStart = "ugyldig dato",
            periodeSlutt = "ugyldig dato",
            kostnadssted = null,
        )

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 2,
            ctx = createCtx(betalingsinformasjonArrangor = null),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskuddVedtak/2/kostnadssted", "Kostnadssted er påkrevd"),
                FieldError("/tilskuddVedtak/2/periodeStart", "Periodestart er påkrevd"),
                FieldError("/tilskuddVedtak/2/periodeSlutt", "Periodeslutt er påkrevd"),
                FieldError("/tilskuddVedtak/2/soknadDato", "Søknadsdato er påkrevd"),
                FieldError("/tilskuddVedtak/2/soknadJournalpostId", "JournalpostId er påkrevd"),
                FieldError("/tilskuddVedtak/2/tilskuddOpplaeringType", "Du må velge en tilskuddstype"),
                FieldError("/tilskuddVedtak/2/utbetalingMottaker", "Du må velge en mottaker"),
                FieldError(
                    "/tilskuddVedtak/2/kommentarVedtaksbrev",
                    "Kommentar kan ikke inneholde mer enn 500 tegn",
                ),
                FieldError("/tilskuddVedtak/2/soknadBelop/belop", "Beløp fra faktura må være positivt"),
                FieldError("/tilskuddVedtak/2/belop", "Beløp til utbetaling må være positivt"),
            )
    }

    test("avviser søknadsdato frem i tid") {
        val request = validTilskuddVedtakRequest().copy(soknadDato = LocalDate.now().plusDays(1))

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(
                journalpostValidator = { journalpostId, _ -> JournalpostId(journalpostId).right() },
            ),
        ) shouldBeLeft listOf(
            FieldError("/tilskudd/0/soknadDato", "Søknadsdato kan ikke være frem i tid"),
        )
    }

    test("krever vedtaksresultat") {
        val request = validTilskuddVedtakRequest().copy(vedtakResultat = null)

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(),
        ) shouldBeLeft listOf(
            FieldError("/tilskuddVedtak/0/vedtakResultat", "Du må velge et resultat"),
        )
    }

    test("krever betalingsinformasjon når utbetaling går til arrangør") {
        val request = validTilskuddVedtakRequest()

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(betalingsinformasjonArrangor = null),
        ) shouldBeLeft listOf(
            FieldError("/tilskuddVedtak/0/utbetalingMottaker", "Betalingsinformasjon for arrangøren må være registrert"),
        )
    }

    BrukerUtbetalingType.entries.forEach { brukerUtbetalingType ->
        test("setter type utbetaling til $brukerUtbetalingType når mottaker er bruker") {
            val request = validTilskuddVedtakRequest().copy(
                utbetalingMottaker = TilskuddMottaker.BRUKER,
                brukerUtbetalingType = brukerUtbetalingType,
            )

            val result = TilskuddBehandlingValidator.validateTilskuddRequest(
                req = request,
                index = 0,
                ctx = createCtx(betalingsinformasjonArrangor = null),
            ).shouldBeRight()

            result.brukerUtbetalingType shouldBe brukerUtbetalingType
        }
    }

    test("krever valg om type utbetaling når mottaker er bruker") {
        val request = validTilskuddVedtakRequest().copy(
            utbetalingMottaker = TilskuddMottaker.BRUKER,
            brukerUtbetalingType = null,
        )

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 1,
            ctx = createCtx(betalingsinformasjonArrangor = null),
        ) shouldBeLeft listOf(
            FieldError("/tilskuddVedtak/1/brukerUtbetalingType", "Du må velge forskudd eller refusjon"),
        )
    }

    test("ignorerer forskuddsutbetalingTilBruker når mottaker er arrangør") {
        val request = validTilskuddVedtakRequest().copy(brukerUtbetalingType = BrukerUtbetalingType.FORSKUDD)

        val result = TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(),
        ).shouldBeRight()

        result.brukerUtbetalingType shouldBe null
    }

    test("validerer periodens rekkefølge og grensen for gjennomføringen") {
        val request = validTilskuddVedtakRequest().copy(
            periodeStart = "2025-08-01",
            periodeSlutt = "2025-07-01",
        )

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 1,
            ctx = createCtx(gjennomforing = createGjennomforing(LocalDate.of(2025, 6, 30))),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskuddVedtak/1/periodeStart", "Periodestart må være før sluttdato"),
                FieldError(
                    "/tilskuddVedtak/1/periodeSlutt",
                    "Periodeslutt kan ikke være etter gjennomføringsperioden",
                ),
            )
    }

    test("feil fra et tilskudd bruker indeksen til elementet i behandlingen") {
        val request = createRequest(
            validTilskuddVedtakRequest(),
            validTilskuddVedtakRequest().copy(kostnadssted = null),
        )

        TilskuddBehandlingValidator.validate(
            request = request,
            behandlendeEnhet = behandlendeEnhet,
            ctx = createCtx(),
        ) shouldBeLeft listOf(
            FieldError("/tilskuddVedtak/1/kostnadssted", "Kostnadssted er påkrevd"),
        )
    }

    test("avviser innvilgelse til arrangør når sluttdato er etter gyldig tilsagnsperiode") {
        val request = validTilskuddVedtakRequest().copy(
            periodeStart = "2026-01-01",
            periodeSlutt = "2026-07-01",
        )
        val validPeriode = Periode.fromInclusiveDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            ctx = createCtx(
                gjennomforing = createGjennomforing(sluttDato = LocalDate.of(2027, 1, 1)),
                tilsagnPeriode = validPeriode,
            ),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError(
                    "/tilskudd/0/periodeSlutt",
                    "Maksimum sluttdato for tilsagn til $tiltakstypeNavn er 30.06.2026",
                ),
            )
    }

    test("avviser periode som går over årsskiftet") {
        val request = validTilskuddVedtakRequest().copy(
            periodeStart = "2025-12-31",
            periodeSlutt = "2026-01-01",
        )
        val validPeriode = Periode.fromInclusiveDates(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31))

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            createCtx(gjennomforing = createGjennomforing(sluttDato = LocalDate.of(2028, 1, 1)), tilsagnPeriode = validPeriode),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskudd/0/periodeSlutt", "Periode kan ikke strekke seg over årsskiftet"),
            )
    }

    test("avviser innvilgelse til slettet arrangør") {
        val gjennomforing = createGjennomforing().also { g ->
            every { g.arrangor.slettet } returns true
        }

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = validTilskuddVedtakRequest(),
            index = 0,
            createCtx(gjennomforing = gjennomforing),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError(
                    "/tilskudd/0/utbetalingMottaker",
                    "Tilskudd kan ikke opprettes fordi arrangøren er slettet i Brreg",
                ),
            )
    }

    test("avslag utløser ikke tilsagnsperiodevalidering") {
        val request = validTilskuddVedtakRequest().copy(
            vedtakResultat = VedtakResultat.AVSLAG,
            belop = null,
        )
        val validPeriode = Periode.fromInclusiveDates(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30))

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            createCtx(),
        ).shouldBeRight()
    }

    test("samler feltfeil fra flere tilskudd i saksopplysningene") {
        val request = createRequest(
            validTilskuddVedtakRequest().copy(kostnadssted = null),
            validTilskuddVedtakRequest().copy(soknadDato = null),
        )

        TilskuddBehandlingValidator.validateSaksopplysninger(
            request = request,
            ctx = createCtx(),
        ).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskuddVedtak/0/kostnadssted", "Kostnadssted er påkrevd"),
                FieldError("/tilskuddVedtak/1/soknadDato", "Søknadsdato er påkrevd"),
            )
    }

    test("journalpostvalidator uten oppslag godtar kun journalpost-id med tall") {
        val journalpostValidator = mockk<JournalpostValidator>()
        val validate = TilskuddBehandlingValidator.createJournalpostValidator(
            forventetBruker = forventetBruker(),
            journalpostValidator = journalpostValidator,
            valideringEnabled = false,
        )

        validate("12345", 3) shouldBeRight JournalpostId("12345")
        validate("abc", 3) shouldBeLeft listOf(
            FieldError(
                "/tilskuddVedtak/3/soknadJournalpostId",
                "Feil format på Journalpost-ID: abc",
            ),
        )
    }

    test("journalpostvalidator bruker M2M og returnerer journalposten ved gyldig oppslag") {
        val forventetBruker = forventetBruker()
        val journalpostValidator = mockk<JournalpostValidator>()
        coEvery {
            journalpostValidator.validerJournalpost("12345", forventetBruker, AccessType.M2M)
        } returns JournalpostId("12345").right()

        val validate = TilskuddBehandlingValidator.createJournalpostValidator(
            forventetBruker = forventetBruker,
            journalpostValidator = journalpostValidator,
            valideringEnabled = true,
        )

        validate("12345", 3) shouldBeRight JournalpostId("12345")
        coVerify(exactly = 1) {
            journalpostValidator.validerJournalpost("12345", forventetBruker, AccessType.M2M)
        }
    }

    test("journalpostvalidator oversetter valideringsfeil til feltfeil") {
        val journalpostValidator = mockk<JournalpostValidator>()
        val feil = listOf(
            JournalpostValidator.JournalpostValideringError.NotFound to "Fant ingen journalpost med id 12345",
            JournalpostValidator.JournalpostValideringError.SafError to "Klarte ikke å slå opp journalpost",
            JournalpostValidator.JournalpostValideringError.FeilFormat to "Feil format på Journalpost-ID: 12345",
            JournalpostValidator.JournalpostValideringError.TilhorerVirksomhet to "Journalposten tilhører en virksomhet",
            JournalpostValidator.JournalpostValideringError.TilhorerPerson to "Journalposten tilhører en person",
            JournalpostValidator.JournalpostValideringError.TilhorerAnnenPerson to
                "Journalposten tilhører en annen person enn deltakeren",
            JournalpostValidator.JournalpostValideringError.TilhorerAnnenVirksomhet to
                "Journalposten tilhører en annen virksomhet",
            JournalpostValidator.JournalpostValideringError.KunneIkkeVeksleAktoerId to
                "Kunne ikke slå opp person i PDL for å verifisere journalposten",
            JournalpostValidator.JournalpostValideringError.IngenTilknytning to
                "Journalposten var ikke tilknyttet en bruker eller virksomhet",
        )

        feil.forEach { (valideringsfeil, forventetDetalj) ->
            coEvery {
                journalpostValidator.validerJournalpost(any(), any(), any())
            } returns valideringsfeil.left()

            TilskuddBehandlingValidator.createJournalpostValidator(
                forventetBruker = forventetBruker(),
                journalpostValidator = journalpostValidator,
                valideringEnabled = true,
            )("12345", 4) shouldBeLeft listOf(
                FieldError("/tilskuddVedtak/4/soknadJournalpostId", forventetDetalj),
            )
        }
    }
})

private fun forventetBruker() = ForventetBruker.PersonEllerBedrift(
    norskIdent = NorskIdent("01010112345"),
    organisasjonsnummer = Organisasjonsnummer("889640782"),
)
