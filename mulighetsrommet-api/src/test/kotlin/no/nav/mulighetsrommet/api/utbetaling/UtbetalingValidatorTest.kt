package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.OpprettManuellUtbetalingRequest.Periode
import no.nav.mulighetsrommet.model.Kontonummer
import java.time.LocalDate
import java.util.*

class UtbetalingValidatorTest : FunSpec({
    test("Skal validere manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periode = Periode(
                start = LocalDate.now(),
                slutt = LocalDate.now().plusDays(1),
            ),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(request)
        result.shouldBeRight()
    }

    test("Periodeslutt må være etter periodestart for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periode = Periode(
                start = LocalDate.now().plusDays(5),
                slutt = LocalDate.now().plusDays(1),
            ),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError(
                    "/arrangorinfo/periode/slutt",
                    "Periodeslutt må være etter periodestart"
                )
            )
        )
    }

    test("Beløp må være større enn kroner 0 for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periode = Periode(
                start = LocalDate.now(),
                slutt = LocalDate.now().plusDays(1),
            ),
            beskrivelse = "Bla bla bla beskrivelse",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 0
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError(
                    "/arrangorinfo/belop",
                    "Beløp må være positivt"
                )
            )
        )
    }

    test("Beskrivelse må være mer enn 10 tegn for manuell utbetaling") {
        val request = OpprettManuellUtbetalingRequest(
            gjennomforingId = UUID.randomUUID(),
            periode = Periode(
                start = LocalDate.now(),
                slutt = LocalDate.now().plusDays(1),
            ),
            beskrivelse = "Bla",
            kontonummer = Kontonummer(value = "12345678910"),
            kidNummer = null,
            belop = 150
        )

        val result = UtbetalingValidator.validateManuellUtbetalingskrav(request)
        result.shouldBeLeft().shouldContainAll(
            listOf(
                FieldError(
                    "/arrangorinfo/beskrivelse",
                    "Du må beskrive utbetalingen"
                )
            )
        )
    }
})
