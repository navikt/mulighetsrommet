package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.Tilsagn1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.OpprettDelutbetaling
import no.nav.mulighetsrommet.api.utbetaling.api.DelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kontonummer
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
        test("opprett krav - investering - gyldig investeringsperiode") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
            every { vedlegg.size } returns 1

            val periodeStart = LocalDate.now().minusMonths(1).withDayOfMonth(1)
            val periodeSlutt = today
            val kontonummer = Kontonummer("12345678910")
            val request = OpprettKravUtbetalingRequest(
                tilsagnId = UUID.randomUUID(),
                periodeStart = periodeStart.toString(),
                periodeSlutt = periodeSlutt.toString(),
                kidNummer = null,
                belop = 1234,
                vedlegg = vedlegg,
            )
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, kontonummer)
            result.shouldBeRight()
        }
        test("opprett krav - investering - ugyldig investeringsperiode") {
            val today = LocalDate.now()
            val kontonummer = Kontonummer("12345678910")
            val prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, kontonummer)
            result.shouldBeLeft()
        }

        test("opprett krav - gyldig annen avtalt pris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.ANNEN_AVTALT_PRIS
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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, kontonummer)
            result.shouldBeRight()
        }

        test("opprett krav - gyldig timespris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, kontonummer)
            result.shouldBeRight()
        }

        test("opprett krav - ugyldig timespris") {
            val today = LocalDate.now()
            val vedlegg = mockk<List<Vedlegg>>(relaxed = true)
            val prismodell = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
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
            val result = UtbetalingValidator.validateOpprettKravArrangorflate(request, prismodell, kontonummer)
            result.shouldBeLeft()
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
