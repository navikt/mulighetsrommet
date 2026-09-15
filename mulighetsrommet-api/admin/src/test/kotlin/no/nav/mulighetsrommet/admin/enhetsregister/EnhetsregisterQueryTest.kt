package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.arrangor.ArrangorType
import no.nav.mulighetsrommet.admin.arrangor.toDto
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class EnhetsregisterQueryTest : FunSpec({
    val db = TestAdminDatabase()

    test("sokHovedenheter validerer at sok ikke er blank") {
        val query = EnhetsregisterQuery(mockk(), db)

        query.sokHovedenheter(" ").shouldBeLeft(EnhetsregisterError.UgyldigSok())
    }

    test("sokHovedenheter kombinerer treff fra gateway med lokalt registrete arrangører") {
        val fraBrreg = Virksomhet.Hovedenhet(
            organisasjonsnummer = Organisasjonsnummer("111111111"),
            navn = "Nord AS",
            organisasjonsform = "AS",
        )
        val utenlandsk = Arrangor.Utenlandsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("100000001"),
            navn = "Nord Utenlandsk AS",
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { sokHovedenheter("nord") } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(
                sok = "nord",
                typer = setOf(ArrangorType.NORSK_HOVEDENHET, ArrangorType.UTENLANDSK),
                slettet = false,
            )
        } returns PaginatedResult(
            totalCount = 1,
            items = listOf(utenlandsk.toDto()),
        )

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.sokHovedenheter("nord").shouldBeRight()

        result shouldBe listOf(
            fraBrreg,
            Virksomhet.Hovedenhet(
                organisasjonsnummer = utenlandsk.organisasjonsnummer,
                navn = utenlandsk.navn,
                organisasjonsform = null,
            ),
        )
    }

    test("sokHovedenheter dedupliserer lokalt lagrede hovedenheter som allerede finnes hos brreg") {
        val fraBrreg = Virksomhet.Hovedenhet(
            organisasjonsnummer = Organisasjonsnummer("111111111"),
            navn = "Nord AS",
            organisasjonsform = "AS",
        )
        val lokal = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = fraBrreg.organisasjonsnummer,
            organisasjonsform = "AS",
            navn = "Nord AS (lokal)",
            overordnetEnhet = null,
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { sokHovedenheter("nord") } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(
                sok = "nord",
                typer = setOf(ArrangorType.NORSK_HOVEDENHET, ArrangorType.UTENLANDSK),
                slettet = false,
            )
        } returns PaginatedResult(totalCount = 1, items = listOf(lokal.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.sokHovedenheter("nord").shouldBeRight()

        result shouldBe listOf(fraBrreg)
    }

    test("sokUnderenheter validerer at sok ikke er blank") {
        val query = EnhetsregisterQuery(mockk(), db)

        query.sokUnderenheter(" ").shouldBeLeft(EnhetsregisterError.UgyldigSok())
    }

    test("sokUnderenheter kombinerer treff fra gateway med lokalt lagrede underenheter") {
        val fraBrreg = Virksomhet.Underenhet(
            organisasjonsnummer = Organisasjonsnummer("111111111"),
            navn = "Nord Avdeling",
            overordnetEnhet = Organisasjonsnummer("222222222"),
            organisasjonsform = "BEDR",
        )
        val tt02Org = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("300000002"),
            organisasjonsform = "BEDR",
            navn = "Nord TT02 Avdeling",
            overordnetEnhet = Organisasjonsnummer("300000001"),
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { sokUnderenheter("nord") } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(
                sok = "nord",
                typer = setOf(ArrangorType.NORSK_UNDERENHET, ArrangorType.UTENLANDSK),
                slettet = false,
            )
        } returns PaginatedResult(totalCount = 1, items = listOf(tt02Org.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.sokUnderenheter("nord").shouldBeRight()

        result shouldBe listOf(
            fraBrreg,
            Virksomhet.Underenhet(
                organisasjonsnummer = tt02Org.organisasjonsnummer,
                navn = tt02Org.navn,
                overordnetEnhet = null,
                organisasjonsform = null,
            ),
        )
    }

    test("sokUnderenheter dedupliserer lokalt lagrede underenheter som allerede finnes hos brreg") {
        val fraBrreg = Virksomhet.Underenhet(
            organisasjonsnummer = Organisasjonsnummer("111111111"),
            navn = "Nord Avdeling",
            overordnetEnhet = Organisasjonsnummer("222222222"),
            organisasjonsform = "BEDR",
        )
        val lokal = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = fraBrreg.organisasjonsnummer,
            organisasjonsform = "BEDR",
            navn = "Nord Avdeling (lokal)",
            overordnetEnhet = Organisasjonsnummer("222222222"),
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { sokUnderenheter("nord") } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(
                sok = "nord",
                typer = setOf(ArrangorType.NORSK_UNDERENHET, ArrangorType.UTENLANDSK),
                slettet = false,
            )
        } returns PaginatedResult(totalCount = 1, items = listOf(lokal.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.sokUnderenheter("nord").shouldBeRight()

        result shouldBe listOf(fraBrreg)
    }

    test("hentUnderenheterForHovedenhet kortslutter til utenlandsk arrangør uten å spørre gateway") {
        val utenlandsk = ArrangorFixtures.Utenlandsk.hovedenhet
        db.repository.arrangor.save(utenlandsk)

        val query = EnhetsregisterQuery(mockk(), db)
        val result = query.hentUnderenheterForHovedenhet(utenlandsk.organisasjonsnummer).shouldBeRight()

        result shouldBe listOf(
            Virksomhet.Underenhet(
                organisasjonsnummer = utenlandsk.organisasjonsnummer,
                navn = utenlandsk.navn,
                overordnetEnhet = null,
                organisasjonsform = null,
            ),
        )
    }

    test("hentUnderenheterForHovedenhet kombinerer treff fra gateway med både slettede og ikke-slettede lokale underenheter") {
        val hovedenhetOrgnr = Organisasjonsnummer("111111112")
        val fraBrreg = Virksomhet.Underenhet(
            organisasjonsnummer = Organisasjonsnummer("222222223"),
            navn = "Avdeling",
            organisasjonsform = "BEDR",
        )
        val slettet = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("333333334"),
            organisasjonsform = "AS",
            navn = "Slettet Avdeling",
            overordnetEnhet = hovedenhetOrgnr,
            slettetDato = LocalDate.of(2020, 1, 1),
        )
        val ikkeSlettet = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("444444445"),
            organisasjonsform = "BEDR",
            navn = "TT02 Avdeling",
            overordnetEnhet = hovedenhetOrgnr,
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { hentUnderenheterForHovedenhet(hovedenhetOrgnr) } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(overordnetEnhetOrgnr = hovedenhetOrgnr)
        } returns PaginatedResult(totalCount = 2, items = listOf(slettet.toDto(), ikkeSlettet.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.hentUnderenheterForHovedenhet(hovedenhetOrgnr).shouldBeRight()

        result shouldBe listOf(
            fraBrreg,
            Virksomhet.Underenhet(
                organisasjonsnummer = slettet.organisasjonsnummer,
                navn = slettet.navn,
                overordnetEnhet = null,
                slettetDato = slettet.slettetDato,
                organisasjonsform = null,
            ),
            Virksomhet.Underenhet(
                organisasjonsnummer = ikkeSlettet.organisasjonsnummer,
                navn = ikkeSlettet.navn,
                overordnetEnhet = null,
                slettetDato = null,
                organisasjonsform = null,
            ),
        )
    }

    test("hentUnderenheterForHovedenhet dedupliserer lokalt lagrede underenheter som allerede finnes hos brreg") {
        val hovedenhetOrgnr = Organisasjonsnummer("111111112")
        val fraBrreg = Virksomhet.Underenhet(
            organisasjonsnummer = Organisasjonsnummer("222222223"),
            navn = "Avdeling",
            overordnetEnhet = hovedenhetOrgnr,
            organisasjonsform = "BEDR",
        )
        val lokal = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = fraBrreg.organisasjonsnummer,
            organisasjonsform = "BEDR",
            navn = "Avdeling (lokal)",
            overordnetEnhet = hovedenhetOrgnr,
        )

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { hentUnderenheterForHovedenhet(hovedenhetOrgnr) } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(overordnetEnhetOrgnr = hovedenhetOrgnr)
        } returns PaginatedResult(totalCount = 1, items = listOf(lokal.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.hentUnderenheterForHovedenhet(hovedenhetOrgnr).shouldBeRight()

        result shouldBe listOf(fraBrreg)
    }
})
