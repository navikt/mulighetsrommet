package no.nav.mulighetsrommet.api.arrangorflate.service

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.arrangorflate.api.PeriodeType
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateOpprettUtbetaling
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

class ArrangorflateUtbetalingValidatorTest : FunSpec({

    context("opprett krav") {
        val vedlegg = listOf(
            Vedlegg(
                content = Content("text/plain", "test".toByteArray()),
                filename = "test.txt",
            ),
        )

        test("investering - gyldig investeringsperiode") {
            val ctx = createContext(
                Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
            )

            val request = OpprettKravUtbetalingRequest(
                periodeStart = "2025-01-01",
                periodeSlutt = "2025-01-31",
                periodeType = PeriodeType.Inklusiv,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(
                ctx,
                request,
            ) shouldBeRight ArrangorflateOpprettUtbetaling(
                gjennomforingId = ctx.gjennomforingId,
                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                kidNummer = null,
                pris = ValutaBelop(1234, Valuta.NOK),
                vedlegg = vedlegg,
            )
        }

        test("gyldig annen avtalt pris") {
            val ctx = createContext(Tiltakskode.AVKLARING, PrismodellType.ANNEN_AVTALT_PRIS)

            val request = OpprettKravUtbetalingRequest(
                periodeStart = "2025-06-01",
                periodeSlutt = "2025-06-30",
                periodeType = PeriodeType.Inklusiv,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(ctx, request).shouldBeRight()
        }

        test("gyldig timespris") {
            val ctx = createContext(
                Tiltakskode.OPPFOLGING,
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            )

            val request = OpprettKravUtbetalingRequest(
                periodeStart = "2025-06-01",
                periodeSlutt = "2025-07-01",
                periodeType = PeriodeType.Eksklusiv,
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            ArrangorflateUtbetalingValidator.validateOpprettKravArrangorflate(ctx, request).shouldBeRight()
        }

        context("maks sluttdato for opprett krav utbetalings periode") {
            test("skal tryne for prismodeller som ikke er støttet") {
                forAll(
                    row(PrismodellType.AVTALT_PRIS_PER_MANEDSVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_UKESVERK),
                    row(PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK),
                ) { prismodell ->
                    shouldThrow<IllegalArgumentException> {
                        ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                            prismodell,
                            Periode.forYear(2025),
                            LocalDate.of(2025, 11, 1),
                        )
                    }
                }
            }

            context("investering") {
                val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

                test("skal gi dagens dato som maks, hvis innenfor opprett krav periode") {
                    val dato = LocalDate.of(2025, 11, 30)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        prismodell,
                        Periode.forYear(2025),
                        dato,
                    ) shouldBe dato
                }

                test("skal gi siste dag i opprett krav konfigurasjonen, om dagens dato er etter den perioden") {
                    val dato = LocalDate.of(2026, 5, 17)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        prismodell,
                        Periode.forYear(2025),
                        dato,
                    ) shouldBe LocalDate.of(2026, 1, 1)
                }
            }

            context("annen avtalt pris") {
                val prismodell = PrismodellType.ANNEN_AVTALT_PRIS

                test("skal kunne sende inn fremover i tid, opptil siste dag i opprett krav perioden") {
                    val dato = LocalDate.of(2026, 11, 1)
                    val result = ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        prismodell,
                        Periode.forYear(2025),
                        dato,
                    )
                    result shouldBe LocalDate.of(2026, 1, 1)
                }
            }

            context("timespris") {
                val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER

                test("skal være 1. dag i samme måned som dagens dato") {
                    val dato = LocalDate.of(2025, 11, 7)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        prismodell,
                        Periode.forYear(2025),
                        dato,
                    ) shouldBe dato.withDayOfMonth(1)
                }

                test("skal være siste dag i opprett krav perioden, hvis dagens dato har forbigått datoen") {
                    val dato = LocalDate.of(2026, 2, 1)
                    ArrangorflateUtbetalingValidator.maksUtbetalingsPeriodeSluttDato(
                        prismodell,
                        Periode.forYear(2025),
                        dato,
                    ) shouldBe LocalDate.of(2026, 1, 1)
                }
            }
        }
    }
})

private fun createContext(
    tiltakskode: Tiltakskode,
    prismodell: PrismodellType,
) = ArrangorflateUtbetalingValidator.ValidateOpprettUtbetalingContext(
    gjennomforingId = UUID.randomUUID(),
    tiltakskode = tiltakskode,
    prismodell = prismodell,
    valuta = Valuta.NOK,
    gyldigTilsagnPeriode = mapOf(tiltakskode to Periode.forYear(2025)),
)
