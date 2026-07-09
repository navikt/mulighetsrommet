package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt.model.AmtArrangorMelding
import no.nav.amt.model.EndringAarsak
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Periode
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
                status = DeltakerStatusType.HAR_SLUTTET,
            )
            val deltaker2 = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                startDato = today.minusMonths(1),
                sluttDato = today.plusMonths(1),
                status = DeltakerStatusType.DELTAR,
            )
            val feilSluttDato = UtbetalingAdvarsler.deltakereMedFeilSluttDato(listOf(deltaker1, deltaker2), today)
            feilSluttDato shouldHaveSize 1
            feilSluttDato[0].deltakerId shouldBe deltaker1.id
        }
    }

    context("isForslagRelevantForPeriode") {
        test("Sluttaarsak") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Sluttarsak(EndringAarsak.Syk),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false
        }

        test("ForlengDeltakelse") {
            // Forlenger slutt midt i periode blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.ForlengDeltakelse(
                    sluttdato = LocalDate.of(2025, 3, 31),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 12)),
            ) shouldBe true

            // Forlenger en allerede full periode blokkerer ikke
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.ForlengDeltakelse(
                    sluttdato = LocalDate.of(2025, 3, 31),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            // Forleng mot formodning er forkorting blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.ForlengDeltakelse(
                    sluttdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }

        test("Sluttdato") {
            // Endrer slutt midt i periode blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Sluttdato(
                    sluttdato = LocalDate.of(2025, 3, 31),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 12)),
            ) shouldBe true

            // Forlenger en allerede full periode blokkerer ikke
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Sluttdato(
                    sluttdato = LocalDate.of(2025, 3, 31),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            // Endre sluttdato tilbake i tid blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Sluttdato(
                    sluttdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }

        test("Deltakelsesmengde") {
            // Endrer deltakelsesmengde etter deltakelse blokkerer ikke
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde(
                    deltakelsesprosent = 20,
                    gyldigFra = LocalDate.of(2025, 3, 31),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            // Endrer deltakelsesmengde inni deltakelse blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde(
                    deltakelsesprosent = 20,
                    gyldigFra = LocalDate.of(2025, 1, 7),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            // Endrer deltakelsesmengde før deltakelse blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde(
                    deltakelsesprosent = 20,
                    gyldigFra = LocalDate.of(2025, 1, 15),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
            ) shouldBe true

            // Endrer deltakelsesmengde uten dato blokkerer
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde(
                    deltakelsesprosent = 20,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 2, 1)),
            ) shouldBe true
        }

        test("AvsluttDeltakelse") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse(
                    harDeltatt = false,
                    aarsak = null,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse(
                    sluttdato = LocalDate.of(2025, 3, 31),
                    harDeltatt = false,
                    aarsak = null,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse(
                    sluttdato = LocalDate.of(2025, 3, 31),
                    harDeltatt = true,
                    aarsak = null,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse(
                    sluttdato = LocalDate.of(2025, 1, 1),
                    harDeltatt = true,
                    aarsak = null,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse(
                    sluttdato = LocalDate.of(2024, 1, 1),
                    aarsak = null,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }

        test("EndreAvslutning") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.EndreAvslutning(
                    harDeltatt = false,
                    aarsak = null,
                    harFullfort = true,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.EndreAvslutning(
                    sluttdato = LocalDate.of(2025, 3, 31),
                    harDeltatt = false,
                    aarsak = null,
                    harFullfort = true,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.EndreAvslutning(
                    sluttdato = LocalDate.of(2025, 3, 31),
                    harDeltatt = true,
                    aarsak = null,
                    harFullfort = false,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.EndreAvslutning(
                    sluttdato = LocalDate.of(2025, 1, 1),
                    harDeltatt = true,
                    aarsak = null,
                    harFullfort = true,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.EndreAvslutning(
                    sluttdato = LocalDate.of(2024, 1, 1),
                    aarsak = null,
                    harFullfort = false,
                    harDeltatt = true,
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }

        test("Startdato") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Startdato(
                    startdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Startdato(
                    startdato = LocalDate.of(2024, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe false

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Startdato(
                    startdato = LocalDate.of(2025, 1, 1),
                    sluttdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Startdato(
                    startdato = LocalDate.of(2025, 1, 1),
                    sluttdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3)),
            ) shouldBe true

            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.Startdato(
                    startdato = LocalDate.of(2025, 1, 1),
                    sluttdato = LocalDate.of(2025, 1, 12),
                ),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 29)),
            ) shouldBe true
        }

        test("IkkeAktuell") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.IkkeAktuell(EndringAarsak.Syk),
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }

        test("FjernOppstartsdato") {
            UtbetalingAdvarsler.isForslagRelevantForPeriode(
                endring = AmtArrangorMelding.Forslag.Endring.FjernOppstartsdato,
                utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                deltakelsePeriode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            ) shouldBe true
        }
    }
})
