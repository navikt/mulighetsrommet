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
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Periode
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
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
                kommentar = "",
                korreksjonBegrunnelse = "Begrunnelse som kun gjelder for korreksjoner",
            )

            UtbetalingValidator.validateUpsertUtbetaling(request) shouldBeRight UpsertUtbetaling.Anskaffelse(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
                journalpostId = JournalpostId("123"),
                kommentar = null,
                beregning = UtbetalingBeregningFri.from(ValutaBelop(150, Valuta.NOK)),
                kid = null,
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                vedlegg = listOf(),
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

            UtbetalingValidator.validateUpsertUtbetaling(request) shouldBeRight UpsertUtbetaling.Korreksjon(
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

        test("begrunnelse er påkrevd i stedet for journalpost-id for korreksjoner") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                korrigererUtbetaling = UUID.randomUUID(),
                korreksjonBegrunnelse = "Bla",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateUpsertUtbetaling(request)
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

            val result = UtbetalingValidator.validateUpsertUtbetaling(request)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være satt", UtbetalingRequest::periodeSlutt),
                FieldError.of("Beløp må være positivt", UtbetalingRequest::pris, ValutaBelopRequest::belop),
                FieldError.of("Kommentar må være minst 10 tegn", UtbetalingRequest::kommentar),
                FieldError.of("Journalpost-ID er på ugyldig format", UtbetalingRequest::journalpostId),
                FieldError.of("Ugyldig kid", UtbetalingRequest::kidNummer),
            )
        }

        test("Periodeslutt må være etter periodestart") {
            val request = UtbetalingRequest(
                id = UUID.randomUUID(),
                gjennomforingId = UUID.randomUUID(),
                periodeStart = periodeStart.plusDays(5),
                periodeSlutt = periodeSlutt,
                journalpostId = "123",
                pris = ValutaBelopRequest(150, Valuta.NOK),
            )

            val result = UtbetalingValidator.validateUpsertUtbetaling(request)
            result.shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                FieldError.of("Periodeslutt må være etter periodestart", UtbetalingRequest::periodeSlutt),
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

    context("opprett utbetalingLinjer") {
        test("skal ikke kunne opprette utbetalingslinje hvis utbetalingen allerede er godkjent") {
            UtbetalingValidator.validateOpprettUtbetalingLinjer(
                UtbetalingValidator.OpprettUtbetalingLinjerCtx(
                    utbetaling = UtbetalingFixtures.utbetalingDto1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET),
                    linjer = emptyList(),
                    begrunnelse = null,
                ),
            ).shouldBeLeft().shouldContainAll(
                FieldError("/", "Utbetaling kan ikke endres fordi den har status: FERDIG_BEHANDLET"),
            )
        }

        test("skal ikke kunne utbetale større enn innsendt beløp") {
            UtbetalingValidator.validateOpprettUtbetalingLinjer(
                UtbetalingValidator.OpprettUtbetalingLinjerCtx(
                    utbetaling = UtbetalingFixtures.utbetalingDto1,
                    linjer = listOf(
                        UtbetalingValidator.OpprettUtbetalingLinjerCtx.Linje(
                            request = UtbetalingLinjeRequest(
                                id = UUID.randomUUID(),
                                pris = 10000000.withValuta(Valuta.NOK).toRequest(),
                                gjorOppTilsagn = true,
                                tilsagnId = UUID.randomUUID(),
                            ),
                            tilsagn = UtbetalingValidator.OpprettUtbetalingLinjerCtx.Tilsagn(
                                status = TilsagnStatus.GODKJENT,
                                gjenstaendeBelop = 10000000.withValuta(Valuta.NOK),
                            ),
                        ),
                    ),
                    begrunnelse = null,
                ),
            ).shouldBeLeft().shouldContainAll(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("begrunnelseMindreBeløp er påkrevd hvis mindre beløp") {
            UtbetalingValidator.validateOpprettUtbetalingLinjer(
                UtbetalingValidator.OpprettUtbetalingLinjerCtx(
                    utbetaling = UtbetalingFixtures.utbetalingDto1,
                    linjer = listOf(
                        UtbetalingValidator.OpprettUtbetalingLinjerCtx.Linje(
                            request = UtbetalingLinjeRequest(
                                id = UUID.randomUUID(),
                                pris = 1.withValuta(Valuta.NOK).toRequest(),
                                gjorOppTilsagn = true,
                                tilsagnId = UUID.randomUUID(),
                            ),
                            tilsagn = UtbetalingValidator.OpprettUtbetalingLinjerCtx.Tilsagn(
                                status = TilsagnStatus.GODKJENT,
                                gjenstaendeBelop = 10.withValuta(Valuta.NOK),
                            ),
                        ),
                    ),
                    begrunnelse = null,
                ),
            ).shouldBeLeft().shouldContainAll(
                FieldError.root("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )
        }
    }
})
