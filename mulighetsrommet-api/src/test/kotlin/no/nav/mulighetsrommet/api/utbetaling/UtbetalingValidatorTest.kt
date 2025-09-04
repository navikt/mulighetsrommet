package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.model.Kontonummer
import java.time.LocalDate
import java.util.*

class UtbetalingValidatorTest : FunSpec({
    test("Skal validere forespørsel om oppretting av utbetaling") {
        val request = OpprettUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
        result.shouldBeRight()
    }

    test("valider opprett utbetaling akumulerer feil") {
        val request = OpprettUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().minusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = "asdf",
            belop = -5,
        )

        val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            FieldError.of(OpprettUtbetalingRequest::periodeSlutt, "Periodeslutt må være etter periodestart"),
            FieldError.of(OpprettUtbetalingRequest::belop, "Beløp må være positivt"),
            FieldError.of(OpprettUtbetalingRequest::kidNummer, "Ugyldig kid"),
        )
    }

    test("Periodeslutt må være etter periodestart") {
        val request = OpprettUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now().plusDays(5),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(
                    OpprettUtbetalingRequest::periodeSlutt,
                    "Periodeslutt må være etter periodestart",
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
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 0,
        )

        val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(OpprettUtbetalingRequest::belop, "Beløp må være positivt"),
            ),
        )
    }

    test("Beskrivelse må være mer enn 10 tegn") {
        val request = OpprettUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(OpprettUtbetalingRequest::beskrivelse, "Du må fylle ut beskrivelse"),
            ),
        )
    }

    test("Kan ikke godkjenne før periode er passert") {
        val request = GodkjennUtbetaling(
            digest = "asdf",
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
})
