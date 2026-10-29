package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.BeregningTestHelpers
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
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

    context("send til godkjenning") {
        val utbetaling = UtbetalingFixtures.utbetalingDto1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)
        val gjenstaendeBelopFeil = "Beløp overstiger gjenstående beløp på tilsagn. " +
            "For å utbetale hele beløpet må dere først opprette og godkjenne et ekstratilsagn"

        fun createTilsagn(
            belop: ValutaBelop = 1000.NOK,
            status: TilsagnStatus = TilsagnStatus.GODKJENT,
            gjennomforingId: UUID = utbetaling.gjennomforing.id,
        ): Tilsagn {
            val gjennomforing = BeregningTestHelpers.createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(
                periode = utbetaling.periode,
            )
            val tilsagn = BeregningTestHelpers.createTilsagn(gjennomforing, utbetaling.periode, belop)
            return tilsagn.copy(status = status, gjennomforing = tilsagn.gjennomforing.copy(id = gjennomforingId))
        }

        fun validerSendTilGodkjenning(
            linjer: List<Pair<OpprettUtbetalingLinje, Tilsagn>>,
            utbetaling: Utbetaling = UtbetalingFixtures.utbetalingDto1.copy(status = UtbetalingStatusType.TIL_BEHANDLING),
            begrunnelseMindreBetalt: String? = null,
            eksisterendeUtbetalingIdByLinjeId: Map<UUID, UUID> = emptyMap(),
        ): Validated<Unit> = UtbetalingValidator.validerSendTilGodkjenning(
            OpprettUtbetalingLinjer(utbetaling.id, linjer.map { it.first }, begrunnelseMindreBetalt),
            UtbetalingValidator.SendTilGodkjenningCtx(
                utbetaling = utbetaling,
                tilsagnByLinjeId = linjer.associate { (linje, tilsagn) -> linje.id to tilsagn },
                eksisterendeUtbetalingIdByLinjeId = eksisterendeUtbetalingIdByLinjeId,
            ),
        )

        fun linje(tilsagn: Tilsagn, pris: ValutaBelop = 1000.NOK) = OpprettUtbetalingLinje(
            id = UUID.randomUUID(),
            tilsagnId = tilsagn.id,
            pris = pris,
            gjorOppTilsagn = false,
        )

        test("gyldig linje godtas") {
            val tilsagn = createTilsagn()

            validerSendTilGodkjenning(listOf(linje(tilsagn) to tilsagn)).shouldBeRight()
        }

        test("utbetaling må være til behandling") {
            val tilsagn = createTilsagn()

            validerSendTilGodkjenning(
                listOf(linje(tilsagn) to tilsagn),
                utbetaling = utbetaling.copy(status = UtbetalingStatusType.GENERERT),
            ) shouldBeLeft listOf(
                FieldError.of("Utbetaling kan bare endres når den er til behandling"),
            )
        }

        test("totalt beløp må være større enn 0") {
            val tilsagn = createTilsagn()

            validerSendTilGodkjenning(listOf(linje(tilsagn, 0.NOK) to tilsagn)) shouldBeLeft listOf(
                FieldError.of("Totalt beløp må være større enn 0"),
                FieldError.of("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )
        }

        test("linje kan ikke overta en linje fra en annen utbetaling") {
            val tilsagn = createTilsagn()
            val linje = linje(tilsagn)

            validerSendTilGodkjenning(
                listOf(linje to tilsagn),
                eksisterendeUtbetalingIdByLinjeId = mapOf(linje.id to UUID.randomUUID()),
            ) shouldBeLeft listOf(
                FieldError("/utbetalingLinjer/0/id", "Utbetalingslinjen tilhører en annen utbetaling"),
            )
        }

        test("eksisterende linje på samme utbetaling godtas") {
            val tilsagn = createTilsagn()
            val linje = linje(tilsagn)

            validerSendTilGodkjenning(
                listOf(linje to tilsagn),
                eksisterendeUtbetalingIdByLinjeId = mapOf(linje.id to utbetaling.id),
            ).shouldBeRight()
        }

        test("kan ikke benytte tilsagn fra en annen gjennomføring") {
            val tilsagn = createTilsagn(gjennomforingId = UUID.randomUUID())

            validerSendTilGodkjenning(listOf(linje(tilsagn) to tilsagn)) shouldBeLeft listOf(
                FieldError("/utbetalingLinjer/0/tilsagnId", "Tilsagnet tilhører en annen gjennomføring"),
            )
        }

        test("totalt beløp kan ikke overstige innsendt beløp") {
            val tilsagn = createTilsagn(belop = 2000.NOK)

            validerSendTilGodkjenning(listOf(linje(tilsagn, 1001.NOK) to tilsagn)) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("begrunnelse er påkrevd når totalt beløp er mindre enn innsendt beløp") {
            val tilsagn = createTilsagn()

            validerSendTilGodkjenning(listOf(linje(tilsagn, 50.NOK) to tilsagn)) shouldBeLeft listOf(
                FieldError.of("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )

            validerSendTilGodkjenning(
                listOf(linje(tilsagn, 50.NOK) to tilsagn),
                begrunnelseMindreBetalt = "Arrangør har fakturert for mye",
            ).shouldBeRight()
        }

        test("beløp kan ikke overstige gjenstående beløp på tilsagn") {
            val tilsagn = createTilsagn()

            validerSendTilGodkjenning(listOf(linje(tilsagn, 1001.NOK) to tilsagn)) shouldBeLeft listOf(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
                FieldError("/utbetalingLinjer/0/tilsagnId", gjenstaendeBelopFeil),
            )
        }

        test("tilsagn som ikke har status GODKJENT kan ikke benyttes") {
            val tilsagn = createTilsagn(status = TilsagnStatus.OPPGJORT)

            validerSendTilGodkjenning(listOf(linje(tilsagn) to tilsagn)) shouldBeLeft listOf(
                FieldError("/utbetalingLinjer/0/tilsagnId", gjenstaendeBelopFeil),
                FieldError(
                    "/utbetalingLinjer/0/tilsagnId",
                    "Tilsagnet har status Oppgjort og kan ikke benyttes, linjen må fjernes",
                ),
            )
        }
    }
})
