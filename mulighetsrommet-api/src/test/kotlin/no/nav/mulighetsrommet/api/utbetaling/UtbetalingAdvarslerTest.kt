package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.util.UUID

class UtbetalingAdvarslerTest : FunSpec({
    context("advarsler") {
        test("feil slutt dato") {
            val today = LocalDate.of(2025, 1, 1)
            forAll(
                row(DeltakerStatusType.AVBRUTT_UTKAST, false),
                row(DeltakerStatusType.DELTAR, false),
                row(DeltakerStatusType.FEILREGISTRERT, false),
                row(DeltakerStatusType.IKKE_AKTUELL, false),
                row(DeltakerStatusType.KLADD, false),
                row(DeltakerStatusType.PABEGYNT_REGISTRERING, false),
                row(DeltakerStatusType.SOKT_INN, false),
                row(DeltakerStatusType.UTKAST_TIL_PAMELDING, false),
                row(DeltakerStatusType.VENTELISTE, false),
                row(DeltakerStatusType.VENTER_PA_OPPSTART, false),
                row(DeltakerStatusType.VURDERES, false),

                row(DeltakerStatusType.AVBRUTT, true),
                row(DeltakerStatusType.FULLFORT, true),
                row(DeltakerStatusType.HAR_SLUTTET, true),
            ) { status, expectedResult ->
                UtbetalingAdvarsler.harFeilSluttDato(status, today.plusDays(1), today = today) shouldBe expectedResult
            }

            // I dag gir false
            UtbetalingAdvarsler.harFeilSluttDato(DeltakerStatusType.HAR_SLUTTET, today, today) shouldBe false
            // I går gir false
            UtbetalingAdvarsler.harFeilSluttDato(DeltakerStatusType.HAR_SLUTTET, today.minusDays(1), today) shouldBe false
            // Om et år gir true
            UtbetalingAdvarsler.harFeilSluttDato(DeltakerStatusType.AVBRUTT, today.plusYears(1), today) shouldBe true
        }

        test("getFeilSluttDato") {
            val today = LocalDate.of(2025, 1, 1)
            val deltaker1 = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                startDato = today.minusMonths(1),
                sluttDato = today.plusMonths(1),
                statusType = DeltakerStatusType.HAR_SLUTTET,
            )
            val deltaker2 = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                startDato = today.minusMonths(1),
                sluttDato = today.plusMonths(1),
                statusType = DeltakerStatusType.DELTAR,
            )
            val feilSluttDato = UtbetalingAdvarsler.deltakereMedFeilSluttDato(listOf(deltaker1, deltaker2), today)
            feilSluttDato shouldHaveSize 1
            feilSluttDato[0].deltakerId shouldBe deltaker1.id
        }
    }
})
