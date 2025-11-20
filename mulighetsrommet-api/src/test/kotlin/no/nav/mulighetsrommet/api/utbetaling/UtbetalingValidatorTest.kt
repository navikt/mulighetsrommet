package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
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

        test("investering - gyldig investeringsperiode") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
            val opprettKravPeriode = mapOf(
                prismodell to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { vedlegg.size } returns 1
            val periodeStart = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            val kontonummer = Kontonummer("12345678910")
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
                prismodell,
                opprettKravPeriode,
                kontonummer,
            )
            result.shouldBeRight()
        }
        test("investering - ugyldig investeringsperiode") {
            val today = LocalDate.now()
            val kontonummer = Kontonummer("12345678910")
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
            val opprettKravPeriode = mapOf(
                prismodell to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, opprettKravPeriode, kontonummer)
            result.shouldBeLeft()
        }

        test("gyldig annen avtalt pris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.ANNEN_AVTALT_PRIS
            val opprettKravPeriode = mapOf(
                prismodell to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { vedlegg.size } returns 0

            val periodeStart = LocalDate.now().minusDays(5)
            val periodeSlutt = periodeStart.plusDays(30)
            val kontonummer = Kontonummer("12345678910")
            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, opprettKravPeriode, kontonummer)
            result.shouldBeRight()
        }

        test("gyldig timespris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
            val opprettKravPeriode = mapOf(
                prismodell to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )
            every { vedlegg.size } returns 1

            val periodeSlutt = today.withDayOfMonth(1)
            val periodeStart = periodeSlutt.minusMonths(1)

            val kontonummer = Kontonummer("12345678910")
            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, opprettKravPeriode, kontonummer)
            result.shouldBeRight()
        }

        test("ugyldig timespris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
            val opprettKravPeriode = mapOf(
                prismodell to Periode.of(today.minusMonths(1).withDayOfMonth(1), today.plusMonths(2))!!,
            )

            every { vedlegg.size } returns 1

            val periodeSlutt = today.withDayOfMonth(1)
            val periodeStart = periodeSlutt.minusMonths(1)

            val kontonummer = Kontonummer("12345678910")
            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.plusDays(1).toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, opprettKravPeriode, kontonummer)
            result.shouldBeLeft()
        }

        context("maks utbetalings periode sluttdato") {
            val opprettKravPeriode = mapOf(
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK to Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                PrismodellType.ANNEN_AVTALT_PRIS to Periode(
                    LocalDate.of(2025, 9, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER to Periode(
                    LocalDate.of(2025, 9, 1),
                    LocalDate.of(2026, 1, 1),
                ),
            )

            test("skal tryne for prismodeller som ikke er støttet") {
                forAll(
                    row(PrismodellType.AVTALT_PRIS_PER_MANEDSVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                ) {
                    shouldThrow<IllegalArgumentException> {
                        UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(it, opprettKravPeriode = opprettKravPeriode)
                    }
                }
            }

            context("investering") {
                test("skal gi dagens dato som maks, hhvis innenfor opprett krav periode") {
                    val dato = LocalDate.of(2025, 11, 30)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK, opprettKravPeriode, dato) shouldBe dato
                }
                test("skal gi siste dag i opprett krav konfigurasjonen, om dagens dato er etter den perioden") {
                    val dato = LocalDate.of(2026, 5, 17)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK, opprettKravPeriode, dato) shouldBe opprettKravPeriode[PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK]!!.slutt
                }
            }

            context("annen avtalt pris") {
                test("skal kunne sende inn fremover i tid, opptil siste dag i opprett krav perioden") {
                    val dato = LocalDate.of(2026, 11, 1)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(PrismodellType.ANNEN_AVTALT_PRIS, opprettKravPeriode, dato) shouldBe opprettKravPeriode[PrismodellType.ANNEN_AVTALT_PRIS]!!.slutt
                }
            }

            context("timespris") {
                test("skal være 1. dag i samme måned som dagens dato") {
                    val dato = LocalDate.of(2025, 11, 7)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER, opprettKravPeriode, dato) shouldBe dato.withDayOfMonth(1)
                }

                test("skal være siste dag i opprett krav perioden, hhvis dagens dato har forbigått datoen") {
                    val dato = LocalDate.of(2026, 1, 1)
                    UtbetalingValidator.maksUtbetalingsPeriodeSluttDato(PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER, opprettKravPeriode, dato) shouldBe opprettKravPeriode[PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER]!!.slutt
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
