package no.nav.mulighetsrommet.api.utbetaling.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingAnnenAvtaltPris
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

class UtbetalingValidatorTest : FunSpec({
    context("opprett utbetaling") {
        val periodeStart = LocalDate.now()
        val periodeSlutt = periodeStart.plusDays(1)

        test("validere forespørsel om oppretting av utbetaling") {
            val request = OpprettUtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
                kommentar = "",
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
            )

            UtbetalingValidator.validateOpprettUtbetalingRequest(request) shouldBeRight OpprettUtbetalingAnnenAvtaltPris(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = JournalpostId("123"),
                kommentar = null,
                korreksjonGjelderUtbetalingId = null,
                korreksjonBegrunnelse = null,
                kid = null,
                pris = ValutaBelop(150, Valuta.NOK),
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                vedlegg = listOf(),
            )
        }

        test("validere forespørsel om oppretting av korreksjon") {
            val request = OpprettUtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                korrigererUtbetaling = UUID.randomUUID(),
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
                kommentar = "En lang kommentar",
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
            )

            UtbetalingValidator.validateOpprettUtbetalingRequest(request) shouldBeRight OpprettUtbetalingAnnenAvtaltPris(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = null,
                kommentar = "En lang kommentar",
                korreksjonGjelderUtbetalingId = request.korrigererUtbetaling,
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
                kid = null,
                pris = ValutaBelop(150, Valuta.NOK),
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                vedlegg = listOf(),
            )
        }

        test("begrunnelse er påkrevd i stedet for journalpost-id for korreksjoner") {
            val request = OpprettUtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                korrigererUtbetaling = UUID.randomUUID(),
                korreksjonBegrunnelse = "Bla",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(request)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of(
                    "Begrunnelse for korreksjon må være minst 10 tegn",
                    OpprettUtbetalingRequest::korreksjonBegrunnelse,
                ),
            )
        }

        test("valider opprett utbetaling akkumulerer feil") {
            val request = OpprettUtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                journalpostId = "abc",
                kidNummer = "asdf",
                kommentar = "1",
                pris = ValutaBelopRequest(0, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(request)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt),
                FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::pris, ValutaBelopRequest::belop),
                FieldError.of("Kommentar må være minst 10 tegn", OpprettUtbetalingRequest::kommentar),
                FieldError.of("Journalpost-ID er på ugyldig format", OpprettUtbetalingRequest::journalpostId),
                FieldError.of("Ugyldig kid", OpprettUtbetalingRequest::kidNummer),
            )
        }

        test("Periodeslutt må være etter periodestart") {
            val request = OpprettUtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart.plusDays(5),
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(request)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være etter periodestart", OpprettUtbetalingRequest::periodeSlutt),
            )
        }
    }

    context("godkjenn utbetaling av arrangør") {
        test("Kan ikke godkjenne før periode er passert") {
            val request = GodkjennUtbetaling(
                updatedAt = "asdf",
                kid = null,
            )

            val result = UtbetalingValidator.validerGodkjennUtbetaling(
                request = request,
                utbetaling = UtbetalingFixtures.utbetalingDto1,
                advarsler = emptyList(),
                today = UtbetalingFixtures.utbetalingDto1.periode.start,
            )
            result.shouldBeLeft().shouldContainAll(
                listOf(
                    FieldError.root("Utbetalingen kan ikke godkjennes før perioden er passert"),
                ),
            )
        }
    }

    context("opprett delutbetalinger") {
        test("skal ikke kunne opprette delutbetaling hvis utbetalingen allerede er godkjent") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET),
                opprettDelutbetalinger = emptyList(),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError("/", "Utbetaling kan ikke endres fordi den har status: FERDIG_BEHANDLET"),
            )
        }

        test("skal ikke kunne utbetale større enn innsendt beløp") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1,
                opprettDelutbetalinger = listOf(
                    OpprettDelutbetaling(
                        id = UUID.randomUUID(),
                        pris = 10000000.withValuta(Valuta.NOK),
                        gjorOppTilsagn = true,
                        tilsagn = OpprettDelutbetaling.Tilsagn(
                            status = TilsagnStatus.GODKJENT,
                            gjenstaendeBelop = 10000000.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("begrunnelseMindreBeløp er påkrevd hvis mindre beløp") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1,
                opprettDelutbetalinger = listOf(
                    OpprettDelutbetaling(
                        id = UUID.randomUUID(),
                        pris = 1.withValuta(Valuta.NOK),
                        gjorOppTilsagn = true,
                        tilsagn = OpprettDelutbetaling.Tilsagn(
                            status = TilsagnStatus.GODKJENT,
                            gjenstaendeBelop = 10.withValuta(Valuta.NOK),
                        ),
                    ),
                ),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError.root("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )
        }
    }
})
