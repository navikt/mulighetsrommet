package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class EnhetsregisterQueryTest : FunSpec({
    val db = TestAdminDatabase()

    fun arrangor(orgnr: String, navn: String, erUtenlandsk: Boolean, overordnetEnhet: String? = null) = Arrangor(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer(orgnr),
        organisasjonsform = "AS",
        navn = navn,
        overordnetEnhet = overordnetEnhet?.let { Organisasjonsnummer(it) },
        erUtenlandsk = erUtenlandsk,
    )

    test("sokHovedenheter validerer at sok ikke er blank") {
        val query = EnhetsregisterQuery(mockk(), db)

        query.sokHovedenheter(" ").shouldBeLeft(EnhetsregisterError.UgyldigSok())
    }

    test("sokHovedenheter kombinerer treff fra gateway med utenlandske arrangører") {
        val fraBrreg = Hovedenhet(organisasjonsnummer = Organisasjonsnummer("111111111"), navn = "Nord AS")
        val utenlandsk = arrangor("100000001", "Nord Utenlandsk AS", erUtenlandsk = true)

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
                organisasjonsform = "AS",
            ),
        )
    }

    test("hentUnderenheterForHovedenhet kortslutter til utenlandsk arrangør uten å spørre gateway") {
        val utenlandsk = arrangor("100000002", "Utenlandsk Tiger AS", erUtenlandsk = true)
        db.repository.arrangor.save(utenlandsk)

        val query = EnhetsregisterQuery(mockk(), db)
        val result = query.hentUnderenheterForHovedenhet(utenlandsk.organisasjonsnummer).shouldBeRight()

        result shouldBe listOf(
            Underenhet(
                organisasjonsnummer = utenlandsk.organisasjonsnummer,
                navn = utenlandsk.navn,
                overordnetEnhet = utenlandsk.organisasjonsnummer,
            ),
        )
    }

    test("hentUnderenheterForHovedenhet kombinerer treff fra gateway med slettede underenheter") {
        val hovedenhetOrgnr = Organisasjonsnummer("111111112")
        val fraBrreg = Underenhet(organisasjonsnummer = Organisasjonsnummer("222222223"), navn = "Avdeling")
        val slettet = arrangor(
            "333333334",
            "Slettet Avdeling",
            erUtenlandsk = false,
            overordnetEnhet = hovedenhetOrgnr.value,
        ).copy(slettetDato = LocalDate.of(2020, 1, 1))

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
                overordnetEnhet = hovedenhetOrgnr,
                slettetDato = slettet.slettetDato,
            ),
        )
    }
})

private fun Arrangor.toDto(): ArrangorDto = ArrangorDto(
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    organisasjonsform = organisasjonsform,
    overordnetEnhet = overordnetEnhet,
    slettetDato = slettetDato,
    id = id,
    underenheter = null,
    erUtenlandsk = erUtenlandsk,
)
