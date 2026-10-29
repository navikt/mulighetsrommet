package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

class UtbetalingValidatorTest : FunSpec({
    val ctx = UtbetalingValidator.Ctx(
        arrangor = ArrangorFixtures.underenhet1,
        korrigertUtbetalingFinnes = false,
        journalpostValidator = { journalpostId ->
            JournalpostId.parse(journalpostId)?.right()
                ?: FieldError(pointer = "/journalpostId", detail = "Feil format").nel().left()
        },
        today = LocalDate.now(),
    )

    context("opprett utbetaling") {
        val periodeStart = LocalDate.now()
        val periodeSlutt = periodeStart.plusDays(1)

        test("validere forespørsel om oppretting av utbetaling") {
            val utbetalingsDato = LocalDate.now()
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                utbetalingsDato = utbetalingsDato,
                pris = ValutaBelopRequest(150, Valuta.NOK),
                kommentar = "",
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
            )

            UtbetalingValidator.validateUpsertUtbetaling(request, ctx) shouldBeRight UpsertUtbetaling.Anskaffelse(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
                journalpostId = JournalpostId("123"),
                kommentar = null,
                beregning = UtbetalingBeregningFri.from(ValutaBelop(150, Valuta.NOK)),
                kid = null,
                utbetalingsDato = utbetalingsDato,
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            )
        }

        test("validere forespørsel om oppretting av korreksjon") {
            val korrigererUtbetaling = UUID.randomUUID()
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                korrigererUtbetaling = korrigererUtbetaling,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
                kommentar = "En lang kommentar",
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
            )

            UtbetalingValidator.validateUpsertUtbetaling(request, ctx.copy(korrigertUtbetalingFinnes = true)) shouldBeRight UpsertUtbetaling.Korreksjon(
                id = request.id,
                periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
                kommentar = "En lang kommentar",
                korreksjonGjelderUtbetalingId = korrigererUtbetaling,
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
                beregning = UtbetalingBeregningFri.from(ValutaBelop(150, Valuta.NOK)),
                kid = null,
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            )
        }

        test("begrunnelse er påkrevd for korreksjoner") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                korrigererUtbetaling = UUID.randomUUID(),
                korreksjonBegrunnelse = "Bla",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateUpsertUtbetaling(request, ctx.copy(korrigertUtbetalingFinnes = true))
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of(
                    "Begrunnelse for korreksjon må være minst 10 tegn",
                    UtbetalingRequest::korreksjonBegrunnelse,
                ),
            )
        }

        test("valider opprett utbetaling akkumulerer feil") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                journalpostId = "abc",
                kidNummer = "asdf",
                kommentar = "1",
                pris = ValutaBelopRequest(0, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateUpsertUtbetaling(request, ctx)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være satt", UtbetalingRequest::periodeSlutt),
                FieldError.of("Beløp må være positivt", UtbetalingRequest::pris, ValutaBelopRequest::belop),
                FieldError.of("Kommentar må være minst 10 tegn", UtbetalingRequest::kommentar),
                FieldError(pointer = "/journalpostId", detail = "Feil format"),
                FieldError.of("Ugyldig kid", UtbetalingRequest::kidNummer),
                FieldError(pointer = "/utbetalingsDato", detail = "Utbetalingsdato er påkrevd"),
            )
        }

        test("Periodeslutt må være etter periodestart") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart.plusDays(5),
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                utbetalingsDato = LocalDate.now(),
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateUpsertUtbetaling(request, ctx)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være etter periodestart", UtbetalingRequest::periodeSlutt),
            )
        }

        test("Journalpostid må være på gyldig format") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = "foo",
                utbetalingsDato = LocalDate.now(),
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            UtbetalingValidator.validateUpsertUtbetaling(request, ctx) shouldBeLeft listOf(
                FieldError(pointer = "/journalpostId", detail = "Feil format"),
            )
        }

        test("utbetalingsdato må være i dag eller innen 3 måneder for anskaffelse") {
            val today = LocalDate.of(2026, 10, 2)

            val nullDato = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val tidligereEnnIdag = nullDato.copy(utbetalingsDato = today.minusDays(1))
            UtbetalingValidator.validateUpsertUtbetaling(tidligereEnnIdag, ctx.copy(today = today)) shouldBeLeft listOf(
                FieldError.of(
                    "Utbetalingsdato må være mellom i dag og 3 måneder frem i tid",
                    UtbetalingRequest::utbetalingsDato,
                ),
            )

            UtbetalingValidator.validateUpsertUtbetaling(
                nullDato.copy(utbetalingsDato = today),
                ctx.copy(today = today),
            ).shouldBeRight()
            UtbetalingValidator.validateUpsertUtbetaling(
                nullDato.copy(utbetalingsDato = today.plusMonths(3)),
                ctx.copy(today = today),
            ).shouldBeRight()

            val etterTreMåneder = nullDato.copy(utbetalingsDato = today.plusMonths(3).plusDays(1))
            UtbetalingValidator.validateUpsertUtbetaling(etterTreMåneder, ctx.copy(today = today)) shouldBeLeft listOf(
                FieldError.of(
                    "Utbetalingsdato må være mellom i dag og 3 måneder frem i tid",
                    UtbetalingRequest::utbetalingsDato,
                ),
            )
        }
    }
})
