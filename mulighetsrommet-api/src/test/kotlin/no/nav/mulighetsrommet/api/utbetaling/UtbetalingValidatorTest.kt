package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.util.UUID

class UtbetalingValidatorTest : FunSpec({
    context("opprett utbetaling") {
        test("Skal validere forespørsel om oppretting av utbetaling") {
            val request = OpprettUtbetalingRequest(
                gjennomforingId = UUID.randomUUID(),
                periodeStart = LocalDate.now(),
                periodeSlutt = LocalDate.now().plusDays(1),
                beskrivelse = "Bla bla bla beskrivelse",
                kidNummer = null,
                pris = 150.withValuta(Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeRight()
        }

        test("valider opprett utbetaling akumulerer feil") {
            val request = OpprettUtbetalingRequest(
                gjennomforingId = UUID.randomUUID(),
                periodeStart = LocalDate.now(),
                periodeSlutt = null,
                beskrivelse = "Bla bla bla beskrivelse",
                kidNummer = "asdf",
                pris = (-5).withValuta(Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeLeft().shouldContainAll(
                FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt),
                FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::pris),
                FieldError.of("Ugyldig kid", OpprettUtbetalingRequest::kidNummer),
            )
        }

        test("Periodeslutt må være etter periodestart") {
            val request = OpprettUtbetalingRequest(
                gjennomforingId = UUID.randomUUID(),
                periodeStart = LocalDate.now().plusDays(5),
                periodeSlutt = LocalDate.now().plusDays(1),
                beskrivelse = "Bla bla bla beskrivelse",
                kidNummer = null,
                pris = 150.withValuta(Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeLeft().shouldContainAll(
                listOf(
                    FieldError.of(
                        "Periodeslutt må være etter periodestart",
                        OpprettUtbetalingRequest::periodeSlutt,
                    ),
                ),
            )
        }

        test("Beløp må være større enn kroner 0") {
            val request = OpprettUtbetalingRequest(
                gjennomforingId = UUID.randomUUID(),
                periodeStart = LocalDate.now(),
                periodeSlutt = LocalDate.now().plusDays(1),
                beskrivelse = "Bla bla bla beskrivelse",
                kidNummer = null,
                pris = 0.withValuta(Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeLeft().shouldContainAll(
                listOf(
                    FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::pris),
                ),
            )
        }

        test("Beskrivelse må være mer enn 10 tegn") {
            val request = OpprettUtbetalingRequest(
                gjennomforingId = UUID.randomUUID(),
                periodeStart = LocalDate.now(),
                periodeSlutt = LocalDate.now().plusDays(1),
                beskrivelse = "Bla",
                kidNummer = null,
                pris = 150.withValuta(Valuta.NOK),
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeLeft().shouldContainAll(
                listOf(
                    FieldError.of("Du må fylle ut beskrivelse", OpprettUtbetalingRequest::beskrivelse),
                ),
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
