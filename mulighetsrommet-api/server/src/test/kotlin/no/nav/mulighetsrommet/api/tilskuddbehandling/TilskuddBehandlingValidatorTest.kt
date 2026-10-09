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
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingValidatorTest : FunSpec({
    val periodeStart = LocalDate.of(2025, 1, 1)
    val periodeSlutt = LocalDate.of(2025, 7, 1)
    val gjennomforingSluttDato = LocalDate.of(2025, 12, 31)
    val behandlendeEnhet = NavEnhetNummer("0502")
    val arrangorBetalingsinformasjon = mockk<Betalingsinformasjon>(relaxed = true)

    fun createGjennomforing(sluttDato: LocalDate? = gjennomforingSluttDato): Gjennomforing = mockk<Gjennomforing>().also { gjennomforing ->
        every { gjennomforing.sluttDato } returns sluttDato
    }

    fun validTilskuddRequest() = TilskuddBehandlingRequest.TilskuddRequest(
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
        belop = 2000,
        periodeStart = periodeStart.toString(),
        periodeSlutt = periodeSlutt.toString(),
        kostnadssted = behandlendeEnhet,
        kommentarIntern = "Intern kommentar",
    )

    fun createRequest(vararg tilskudd: TilskuddBehandlingRequest.TilskuddRequest) = TilskuddBehandlingRequest(
        id = UUID.randomUUID(),
        gjennomforingId = UUID.randomUUID(),
        tilskudd = tilskudd.toList(),
    )

    test("validerer og mapper en gyldig tilskuddsbehandling") {
        val request = createRequest(validTilskuddRequest())

        TilskuddBehandlingValidator.validate(
            request = request,
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
            behandlendeEnhet = behandlendeEnhet,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() } shouldBeRight TilskuddBehandling(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            tilskudd = listOf(
                TilskuddVedtak(
                    id = request.tilskudd.single().id,
                    tilskuddId = request.tilskudd.single().tilskuddId,
                    tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                    soknadJournalpostId = JournalpostId("12345"),
                    soknadDato = LocalDate.of(2024, 1, 15),
                    soknadBelop = ValutaBelop(1200, Valuta.NOK),
                    periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
                    kostnadssted = behandlendeEnhet,
                    vedtakResultat = VedtakResultat.INNVILGELSE,
                    utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                    kid = Kid.parse("116"),
                    utbetalingBelop = ValutaBelop(2000, Valuta.NOK),
                    kommentarIntern = "Intern kommentar",
                    kommentarVedtaksbrev = "Vedtakskommentar",
                ),
            ),
            status = TilskuddBehandlingStatus.TIL_ATTESTERING,
            type = TilskuddBehandlingType.REGISTRERING,
            behandlendeEnhet = behandlendeEnhet,
        )
    }

    test("avslag trenger ikke beløp til utbetaling") {
        val request = validTilskuddRequest().copy(
            vedtakResultat = VedtakResultat.AVSLAG,
            belop = null,
        )

        val result = TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() }.shouldBeRight()

        result.vedtakResultat shouldBe VedtakResultat.AVSLAG
        result.utbetalingBelop shouldBe null
    }

    test("samler feltfeil for ugyldig tilskuddsrequest") {
        val request = validTilskuddRequest().copy(
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
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = null,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() }.shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskudd/2/kostnadssted", "Kostnadssted er påkrevd"),
                FieldError("/tilskudd/2/periodeStart", "Periodestart er påkrevd"),
                FieldError("/tilskudd/2/periodeSlutt", "Periodeslutt er påkrevd"),
                FieldError("/tilskudd/2/soknadDato", "Søknadsdato er påkrevd"),
                FieldError("/tilskudd/2/soknadJournalpostId", "JournalpostId er påkrevd"),
                FieldError("/tilskudd/2/tilskuddOpplaeringType", "Du må velge en tilskuddstype"),
                FieldError("/tilskudd/2/utbetalingMottaker", "Du må velge en mottaker"),
                FieldError(
                    "/tilskudd/2/kommentarVedtaksbrev",
                    "Kommentar kan ikke inneholde mer enn 500 tegn",
                ),
                FieldError("/tilskudd/2/kidNummer", "Ugyldig kid"),
                FieldError("/tilskudd/2/soknadBelop/belop", "Beløp fra faktura må være positivt"),
                FieldError("/tilskudd/2/belop", "Beløp til utbetaling må være positivt"),
            )
    }

    test("krever vedtaksresultat") {
        val request = validTilskuddRequest().copy(vedtakResultat = null)

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() } shouldBeLeft listOf(
            FieldError("/tilskudd/0/vedtakResultat", "Du må velge et resultat"),
        )
    }

    test("krever betalingsinformasjon når utbetaling går til arrangør") {
        val request = validTilskuddRequest()

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 0,
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = null,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() } shouldBeLeft listOf(
            FieldError("/tilskudd/0/utbetalingMottaker", "Betalingsinformasjon for arrangøren må være registrert"),
        )
    }

    test("validerer periodens rekkefølge og grensen for gjennomføringen") {
        val request = validTilskuddRequest().copy(
            periodeStart = "2025-08-01",
            periodeSlutt = "2025-07-01",
        )

        TilskuddBehandlingValidator.validateTilskuddRequest(
            req = request,
            index = 1,
            gjennomforing = createGjennomforing(LocalDate.of(2025, 6, 30)),
            arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() }.shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError("/tilskudd/1/periodeStart", "Periodestart må være før sluttdato"),
                FieldError(
                    "/tilskudd/1/periodeSlutt",
                    "Periodeslutt kan ikke være etter gjennomføringsperioden",
                ),
            )
    }

    test("feil fra et tilskudd bruker indeksen til elementet i behandlingen") {
        val request = createRequest(
            validTilskuddRequest(),
            validTilskuddRequest().copy(kostnadssted = null),
        )

        TilskuddBehandlingValidator.validate(
            request = request,
            gjennomforing = createGjennomforing(),
            arrangorBetalingsinformasjon = arrangorBetalingsinformasjon,
            behandlendeEnhet = behandlendeEnhet,
        ) { journalpostId, _ -> JournalpostId(journalpostId).right() } shouldBeLeft listOf(
            FieldError("/tilskudd/1/kostnadssted", "Kostnadssted er påkrevd"),
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
                "/tilskudd/3/soknadJournalpostId",
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
                FieldError("/tilskudd/4/soknadJournalpostId", forventetDetalj),
            )
        }
    }
})

private fun forventetBruker() = ForventetBruker.PersonEllerBedrift(
    norskIdent = NorskIdent("01010112345"),
    organisasjonsnummer = Organisasjonsnummer("889640782"),
)
