package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDate
import java.util.*

class OkonomiBestillingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    lateinit var service: OkonomiBestillingService

    beforeContainer {
        service = OkonomiBestillingService(
            config = OkonomiBestillingService.Config(topic = "okonomi.bestilling.v1"),
            db = database.db,
        )
    }

    afterEach {
        database.truncateAll()
    }

    context("skedulering av oppgaver for økonomi") {
        val bestillingsnummer = "A-2025/1-1"

        val tilsagn = TilsagnFixtures.Tilsagn1.copy(
            bestillingsnummer = bestillingsnummer,
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1)),
            beregning = TilsagnBeregningFri(
                input = TilsagnBeregningFri.Input(1000),
                output = TilsagnBeregningFri.Output(1000),
            ),
        )

        val utbetaling1 = UtbetalingFixtures.utbetaling1.copy(
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1)),
            beregning = UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(500),
                output = UtbetalingBeregningFri.Output(500),
            ),
        )
        val utbetaling2 = UtbetalingFixtures.utbetaling2.copy(
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1)),
            beregning = UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(500),
                output = UtbetalingBeregningFri.Output(500),
            ),
        )
        val delutbetaling1 = UtbetalingFixtures.delutbetaling1
        val delutbetaling2 = UtbetalingFixtures.delutbetaling1
            .copy(
                id = UUID.randomUUID(),
                utbetalingId = utbetaling2.id,
                lopenummer = 2,
                fakturanummer = "$bestillingsnummer-2",
            )

        afterEach {
            database.truncateAll()
        }

        test("godkjent tilsagn blir omsider sendt som bestilling på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            database.run {
                service.behandleGodkjentTilsagn(queries.tilsagn.get(tilsagn.id)!!, this)
            }

            val records = database.run { queries.kafkaProducerRecord.getRecords(50) }
            records shouldHaveSize 1
            val record = records[0]
            record.topic shouldBe "okonomi.bestilling.v1"
            record.key shouldBe tilsagn.bestillingsnummer.toByteArray()
            val bestilling = record.value?.toString(Charsets.UTF_8)?.let {
                Json.decodeFromString<OkonomiBestillingMelding>(it)
            }
                .shouldBeTypeOf<OkonomiBestillingMelding.Bestilling>()
                .payload

            bestilling.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            bestilling.arrangor.hovedenhet shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
            bestilling.arrangor.underenhet shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
            bestilling.kostnadssted shouldBe NavEnhetNummer(NavEnhetFixtures.Innlandet.enhetsnummer)
            bestilling.bestillingsnummer shouldBe tilsagn.bestillingsnummer
            bestilling.belop shouldBe tilsagn.beregning.output.belop

            bestilling.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
            bestilling.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)
        }

        test("annullert tilsagn blir omsider sendt som annullering på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setTilsagnStatus(tilsagn, TilsagnStatus.ANNULLERT)
            }.initialize(database.db)

            database.run {
                service.behandleAnnullertTilsagn(queries.tilsagn.get(tilsagn.id)!!, this)
            }
            val record = database.run { queries.kafkaProducerRecord.getRecords(50) }[0]
            record.topic shouldBe "okonomi.bestilling.v1"

            record.key shouldBe bestillingsnummer.toByteArray()

            val annullering = Json.decodeFromString<OkonomiBestillingMelding>(record.value!!.toString(Charsets.UTF_8))
                .shouldBeTypeOf<OkonomiBestillingMelding.Annullering>()
                .payload

            annullering.bestillingsnummer shouldBe bestillingsnummer
            annullering.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
            annullering.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)
        }

        test("godkjent utbetaling blir omsider sendt som faktura på kafka") {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling1, utbetaling2),
                delutbetalinger = listOf(delutbetaling1, delutbetaling2),
            ) {
                setDelutbetalingStatus(delutbetaling1, DelutbetalingStatus.GODKJENT)
                setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
            }.initialize(database.db)

            database.run {
                service.behandleGodkjentUtbetaling(
                    queries.delutbetaling.getByUtbetalingId(utbetaling1.id),
                    this,
                )
            }
            val record = database.run { queries.kafkaProducerRecord.getRecords(50) }[0]
            record.topic shouldBe "okonomi.bestilling.v1"

            record.key shouldBe bestillingsnummer.toByteArray()

            val faktura = Json.decodeFromString<OkonomiBestillingMelding>(record.value!!.toString(Charsets.UTF_8))
                .shouldBeTypeOf<OkonomiBestillingMelding.Faktura>()
                .payload

            faktura.bestillingsnummer shouldBe tilsagn.bestillingsnummer
            faktura.fakturanummer shouldBe delutbetaling1.fakturanummer
            faktura.periode shouldBe delutbetaling1.periode
            faktura.belop shouldBe delutbetaling1.belop
            faktura.behandletAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt1.navIdent)
            faktura.besluttetAv shouldBe OkonomiPart.NavAnsatt(NavAnsattFixture.ansatt2.navIdent)
        }
    }
})
