package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.arrangor.toDto
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class EnhetsregisterQueryTest : FunSpec({
    val db = TestAdminDatabase()

    test("sokHovedenheter validerer at sok ikke er blank") {
        val query = EnhetsregisterQuery(mockk(), db)

        query.sokHovedenheter(" ").shouldBeLeft(EnhetsregisterError.UgyldigSok())
    }

    test("sokHovedenheter kombinerer treff fra gateway med utenlandske arrangører") {
        val fraBrreg = Hovedenhet(
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
                utenlandsk = true,
            )
        } returns PaginatedResult(
            totalCount = 1,
            items = listOf(utenlandsk.toDto()),
        )

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.sokHovedenheter("nord").shouldBeRight()

        result shouldBe listOf(
            fraBrreg,
            Hovedenhet(
                organisasjonsnummer = utenlandsk.organisasjonsnummer,
                navn = utenlandsk.navn,
                organisasjonsform = null,
            ),
        )
    }

    test("hentUnderenheterForHovedenhet kortslutter til utenlandsk arrangør uten å spørre gateway") {
        val utenlandsk = ArrangorFixtures.Utenlandsk.hovedenhet
        db.repository.arrangor.save(utenlandsk)

        val query = EnhetsregisterQuery(mockk(), db)
        val result = query.hentUnderenheterForHovedenhet(utenlandsk.organisasjonsnummer).shouldBeRight()

        result shouldBe listOf(
            Underenhet(
                organisasjonsnummer = utenlandsk.organisasjonsnummer,
                navn = utenlandsk.navn,
                overordnetEnhet = null,
                organisasjonsform = null,
            ),
        )
    }

    test("hentUnderenheterForHovedenhet kombinerer treff fra gateway med slettede underenheter") {
        val hovedenhetOrgnr = Organisasjonsnummer("111111112")
        val fraBrreg = Underenhet(
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

        val gateway: EnhetsregisterGateway = mockk {
            coEvery { hentUnderenheterForHovedenhet(hovedenhetOrgnr) } returns listOf(fraBrreg).right()
        }
        every {
            db.queries.arrangor.getAll(
                overordnetEnhetOrgnr = hovedenhetOrgnr,
                slettet = true,
            )
        } returns PaginatedResult(totalCount = 1, items = listOf(slettet.toDto()))

        val query = EnhetsregisterQuery(gateway, db)
        val result = query.hentUnderenheterForHovedenhet(hovedenhetOrgnr).shouldBeRight()

        result shouldBe listOf(
            fraBrreg,
            Underenhet(
                organisasjonsnummer = slettet.organisasjonsnummer,
                navn = slettet.navn,
                overordnetEnhet = null,
                slettetDato = slettet.slettetDato,
                organisasjonsform = null,
            ),
        )
    }
})
