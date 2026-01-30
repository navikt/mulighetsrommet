package no.nav.mulighetsrommet.api.arrangorflate.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.ApplicationConfigLocal
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

class ArrangorflateUtbetalingValidatorTest : FunSpec({

    context("opprett krav") {
        val today = LocalDate.now()
        val kontonummer = Kontonummer("12345678910")
        val okonomiConfig = mockk<OkonomiConfig>(relaxed = true)
        val gjennomforing = mockk<ArrangorflateTiltak>(relaxed = true)
        val vedlegg = mockk<List<Vedlegg>>(relaxed = true)

        beforeEach {
            clearMocks(okonomiConfig)
            clearMocks(gjennomforing)
            clearMocks(vedlegg)
        }

        test("investering - gyldig investeringsperiode") {
            val tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { vedlegg.size } returns 1
            every { gjennomforing.prismodell.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode

            val periodeStart = LocalDate.now().minusMonths(1).withDayOfMonth(1)

            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = today.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                request,
                gjennomforing,
                okonomiConfig,
                kontonummer,
            ).shouldBeRight()
        }

        test("investering - ugyldig investeringsperiode") {
            val tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
            every { vedlegg.size } returns 1

            val periodeStart = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            val periodeSlutt = today.plusDays(1)

            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                request,
                gjennomforing,
                okonomiConfig,
                kontonummer,
            ).shouldBeLeft()
        }

        test("gyldig annen avtalt pris") {
            val tiltakskode = Tiltakskode.AVKLARING
            val prismodell = PrismodellType.ANNEN_AVTALT_PRIS

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
            every { vedlegg.size } returns 1

            val periodeStart = LocalDate.now().minusDays(5)
            val periodeSlutt = periodeStart.plusDays(30)

            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                request,
                gjennomforing,
                okonomiConfig,
                kontonummer,
            ).shouldBeRight()
        }

        test("gyldig timespris") {
            val tiltakskode = Tiltakskode.OPPFOLGING
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
            every { vedlegg.size } returns 1

            val periodeSlutt = today.withDayOfMonth(1)
            val periodeStart = periodeSlutt.minusMonths(1)

            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result =
                ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                    request,
                    gjennomforing,
                    okonomiConfig,
                    kontonummer,
                )
            result.shouldBeRight()
        }

        test("ugyldig timespris") {
            val tiltakskode = Tiltakskode.OPPFOLGING
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
            every { vedlegg.size } returns 1

            val periodeSlutt = today.withDayOfMonth(1)
            val periodeStart = periodeSlutt.minusMonths(1)

            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.plusDays(1).toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result =
                ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                    request,
                    gjennomforing,
                    okonomiConfig,
                    kontonummer,
                )
            result.shouldBeLeft()
        }

        context("maks sluttdato for opprett krav utbetalings periode") {
            val localOkonomiConfig = ApplicationConfigLocal.okonomi
            val gjennomforing = mockk<ArrangorflateTiltak>(relaxed = true)
            val okonomiConfig = mockk<OkonomiConfig>(relaxed = true)

            beforeEach {
                clearMocks(gjennomforing)
                clearMocks(okonomiConfig)
            }

            test("skal tryne for prismodeller som ikke er støttet") {
                forAll(
                    row(PrismodellType.AVTALT_PRIS_PER_MANEDSVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK),
                ) {
                    every { okonomiConfig.gyldigTilsagnPeriode } returns localOkonomiConfig.gyldigTilsagnPeriode
                    every { okonomiConfig.opprettKravPrismodeller } returns localOkonomiConfig.opprettKravPrismodeller
                    every { gjennomforing.prismodell.type } returns it
                    every { gjennomforing.tiltakstype.tiltakskode } returns localOkonomiConfig.gyldigTilsagnPeriode.keys.first()
                    shouldThrow<IllegalArgumentException> {
                        ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                            gjennomforing,
                            okonomiConfig,
                            LocalDate.of(2025, 11, 1),
                        )
                    }
                }
            }

            context("investering") {
                val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
                val tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
                val sisteTilsagnsDato = LocalDate.of(2026, 1, 1)

                beforeEach {
                    every { gjennomforing.prismodell.type } returns prismodell
                    every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
                    every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
                    every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                        tiltakskode to Periode.of(
                            LocalDate.of(2025, 10, 1),
                            sisteTilsagnsDato,
                        )!!,
                    )
                }

                test("skal gi dagens dato som maks, hhvis innenfor opprett krav periode") {

                    val dato = LocalDate.of(2025, 11, 30)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe dato
                }

                test("skal gi siste dag i opprett krav konfigurasjonen, om dagens dato er etter den perioden") {
                    val dato = LocalDate.of(2026, 5, 17)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe sisteTilsagnsDato
                }
            }

            context("annen avtalt pris") {
                val prismodell = PrismodellType.ANNEN_AVTALT_PRIS
                val tiltakskode = Tiltakskode.AVKLARING
                val sisteTilsagnsDato = LocalDate.of(2026, 1, 1)

                beforeEach {
                    every { gjennomforing.prismodell.type } returns prismodell
                    every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
                    every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
                    every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                        tiltakskode to Periode.of(
                            LocalDate.of(2025, 10, 1),
                            sisteTilsagnsDato,
                        )!!,
                    )
                }

                test("skal kunne sende inn fremover i tid, opptil siste dag i opprett krav perioden") {
                    val dato = LocalDate.of(2026, 11, 1)
                    val result = ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    )
                    result shouldBe sisteTilsagnsDato
                }
            }

            context("timespris") {
                val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
                val tiltakskode = Tiltakskode.OPPFOLGING
                val sisteTilsagnsDato = LocalDate.of(2026, 1, 1)

                beforeEach {
                    every { gjennomforing.prismodell.type } returns prismodell
                    every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
                    every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
                    every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                        tiltakskode to Periode.of(
                            LocalDate.of(2025, 10, 1),
                            sisteTilsagnsDato,
                        )!!,
                    )
                }

                test("skal være 1. dag i samme måned som dagens dato") {
                    val dato = LocalDate.of(2025, 11, 7)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe dato.withDayOfMonth(1)
                }

                test("skal være siste dag i opprett krav perioden, hhvis dagens dato har forbigått datoen") {
                    val dato = LocalDate.of(2026, 2, 1)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe sisteTilsagnsDato
                }
            }
        }
    }
})
