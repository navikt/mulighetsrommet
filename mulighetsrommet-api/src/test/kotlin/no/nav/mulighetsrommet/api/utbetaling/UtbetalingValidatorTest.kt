package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.ApplicationConfigLocal
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

class UtbetalingValidatorTest : FunSpec({
    context("opprett utbetaling") {
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
                periodeSlutt = null,
                beskrivelse = "Bla bla bla beskrivelse",
                kontonummer = Kontonummer(value = "12345678910"),
                kidNummer = "asdf",
                belop = -5,
            )

            val result = UtbetalingValidator.validateOpprettUtbetalingRequest(UUID.randomUUID(), request)
            result.shouldBeLeft().shouldContainAll(
                FieldError.of("Periodeslutt må være satt", OpprettUtbetalingRequest::periodeSlutt),
                FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::belop),
                FieldError.of("Ugyldig kid", OpprettUtbetalingRequest::kidNummer),
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
                        "Periodeslutt må være etter periodestart",
                        OpprettUtbetalingRequest::periodeSlutt,
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
                    FieldError.of("Beløp må være positivt", OpprettUtbetalingRequest::belop),
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
                    FieldError.of("Du må fylle ut beskrivelse", OpprettUtbetalingRequest::beskrivelse),
                ),
            )
        }
    }

    context("godkjenn utbetaling av arrangør") {
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
    }

    context("opprett krav") {
        val today = LocalDate.now()
        val kontonummer = Kontonummer("12345678910")
        val okonomiConfig = mockk<OkonomiConfig>(relaxed = true)
        val gjennomforing = mockk<GjennomforingGruppetiltak>(relaxed = true)
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
            every { gjennomforing.prismodell?.type } returns prismodell
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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(
                request,
                gjennomforing,
                okonomiConfig,
                kontonummer,
            )
            result.shouldBeRight()
        }
        test("investering - ugyldig investeringsperiode") {
            val tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell?.type } returns prismodell
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
            val result =
                UtbetalingValidator.validateOpprettKravArrangorflate(request, gjennomforing, okonomiConfig, kontonummer)
            result.shouldBeLeft()
        }

        test("gyldig annen avtalt pris") {
            val tiltakskode = Tiltakskode.AVKLARING
            val prismodell = PrismodellType.ANNEN_AVTALT_PRIS

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell?.type } returns prismodell
            every { gjennomforing.tiltakstype.tiltakskode } returns tiltakskode
            every { vedlegg.size } returns 0

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
            val result =
                UtbetalingValidator.validateOpprettKravArrangorflate(request, gjennomforing, okonomiConfig, kontonummer)
            result.shouldBeRight()
        }

        test("gyldig timespris") {
            val tiltakskode = Tiltakskode.OPPFOLGING
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell?.type } returns prismodell
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
                UtbetalingValidator.validateOpprettKravArrangorflate(request, gjennomforing, okonomiConfig, kontonummer)
            result.shouldBeRight()
        }

        test("ugyldig timespris") {
            val tiltakskode = Tiltakskode.OPPFOLGING
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER

            every { okonomiConfig.gyldigTilsagnPeriode } returns mapOf(
                tiltakskode to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { okonomiConfig.opprettKravPrismodeller } returns listOf(prismodell)
            every { gjennomforing.prismodell?.type } returns prismodell
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
                UtbetalingValidator.validateOpprettKravArrangorflate(request, gjennomforing, okonomiConfig, kontonummer)
            result.shouldBeLeft()
        }

        context("maks sluttdato for opprett krav utbetalings periode") {
            val localOkonomiConfig = ApplicationConfigLocal.okonomi
            val gjennomforing = mockk<GjennomforingGruppetiltak>(relaxed = true)
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
                    every { gjennomforing.prismodell?.type } returns it
                    every { gjennomforing.tiltakstype.tiltakskode } returns localOkonomiConfig.gyldigTilsagnPeriode.keys.first()
                    shouldThrow<IllegalArgumentException> {
                        UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
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
                    every { gjennomforing.prismodell?.type } returns prismodell
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
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe dato
                }
                test("skal gi siste dag i opprett krav konfigurasjonen, om dagens dato er etter den perioden") {
                    val dato = LocalDate.of(2026, 5, 17)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
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
                    every { gjennomforing.prismodell?.type } returns prismodell
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
                    val result = UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
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
                    every { gjennomforing.prismodell?.type } returns prismodell
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
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe dato.withDayOfMonth(1)
                }

                test("skal være siste dag i opprett krav perioden, hhvis dagens dato har forbigått datoen") {
                    val dato = LocalDate.of(2026, 2, 1)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        gjennomforing,
                        okonomiConfig,
                        dato,
                    ) shouldBe sisteTilsagnsDato
                }
            }
        }
    }

    context("opprett delutbetalinger") {
        test("skal ikke kunne opprette delutbetaling hvis utbetalingen allerede er godkjent") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET),
                opprettDelutbetalinger = emptyList(),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError("/", "Utbetaling kan ikke endres fordi den har status: FERDIG_BEHANDLET"),
            )
        }

        test("skal ikke kunne utbetale større enn innsendt beløp") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1,
                opprettDelutbetalinger = listOf(
                    OpprettDelutbetaling(
                        id = UUID.randomUUID(),
                        belop = 10000000,
                        gjorOppTilsagn = true,
                        tilsagn = OpprettDelutbetaling.Tilsagn(
                            status = TilsagnStatus.GODKJENT,
                            gjenstaendeBelop = 10000000,
                        ),
                    ),
                ),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError.of("Kan ikke utbetale mer enn innsendt beløp"),
            )
        }

        test("begrunnelseMindreBeløp er påkrevd hvis mindre beløp") {
            UtbetalingValidator.validateOpprettDelutbetalinger(
                utbetaling = UtbetalingFixtures.utbetalingDto1,
                opprettDelutbetalinger = listOf(
                    OpprettDelutbetaling(
                        id = UUID.randomUUID(),
                        belop = 1,
                        gjorOppTilsagn = true,
                        tilsagn = OpprettDelutbetaling.Tilsagn(
                            status = TilsagnStatus.GODKJENT,
                            gjenstaendeBelop = 10,
                        ),
                    ),
                ),
                begrunnelse = null,
            ).shouldBeLeft().shouldContainAll(
                FieldError.root("Begrunnelse er påkrevd ved utbetaling av mindre enn innsendt beløp"),
            )
        }
    }
})
