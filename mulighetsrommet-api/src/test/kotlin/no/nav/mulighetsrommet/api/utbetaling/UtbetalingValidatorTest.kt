package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest
import no.nav.mulighetsrommet.model.Kontonummer
import java.time.LocalDate
import java.util.*

class UtbetalingValidatorTest : FunSpec({
    test("Skal validere manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(UUID.randomUUID(), request)
        result.shouldBeRight()
    }

    test("Periodeslutt må være etter periodestart for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now().plusDays(5),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(
                    OpprettManuellUtbetalingRequest::periodeSlutt,
                    "Periodeslutt må være etter periodestart",
                ),
            ),
        )
    }

    test("Beløp må være større enn kroner 0 for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 0,
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(OpprettManuellUtbetalingRequest::belop, "Beløp må være positivt"),
            ),
        )
    }

    test("Beskrivelse må være mer enn 10 tegn for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periodeStart = LocalDate.now(),
            periodeSlutt = LocalDate.now().plusDays(1),
            beskrivelse = "Bla",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150,
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(UUID.randomUUID(), request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError.of(OpprettManuellUtbetalingRequest::beskrivelse, "Du må fylle ut beskrivelse"),
            ),
        )
    }
})
