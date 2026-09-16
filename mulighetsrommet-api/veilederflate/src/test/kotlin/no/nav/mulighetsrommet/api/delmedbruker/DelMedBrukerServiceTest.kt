package no.nav.mulighetsrommet.api.delmedbruker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.veilederflate.NavEnhetDto
import no.nav.mulighetsrommet.api.veilederflate.NavEnhetService
import no.nav.mulighetsrommet.api.veilederflate.testing.TestVeilederflateDatabase
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.UUID

class DelMedBrukerServiceTest : FunSpec({
    val navEnhetService = mockk<NavEnhetService>()
    val db = TestVeilederflateDatabase()
    val service = DelMedBrukerService(db, navEnhetService)

    val dbo = DelMedBrukerDbo(
        norskIdent = NorskIdent("12345678910"),
        navIdent = NavIdent("B123456"),
        dialogId = "1",
        tiltakstypeId = UUID.randomUUID(),
        tiltakDokumentId = UUID.randomUUID(),
        gjennomforingId = null,
        deltFraEnhet = NavEnhetNummer("0502"),
    )

    test("insertDelMedBruker slår opp overordnet fylkesenhet og lagrer delingen med fylkesnummer") {
        val fylke = NavEnhetDto(
            navn = "Innlandet",
            enhetsnummer = NavEnhetNummer("0400"),
            type = NavEnhetType.FYLKE,
            overordnetEnhet = null,
        )
        every { navEnhetService.hentOverordnetFylkesenhet(dbo.deltFraEnhet) } returns fylke

        service.insertDelMedBruker(dbo)

        verify(exactly = 1) { db.queries.delMedBruker.insert(dbo, fylke.enhetsnummer) }
    }

    test("insertDelMedBruker lagrer uten fylke når enhet ikke har overordnet fylkesenhet") {
        every { navEnhetService.hentOverordnetFylkesenhet(dbo.deltFraEnhet) } returns null

        service.insertDelMedBruker(dbo)

        verify(exactly = 1) { db.queries.delMedBruker.insert(dbo, null) }
    }

    test("getLast delegerer til queries") {
        val id = UUID.randomUUID()
        val expected = mockk<DelMedBrukerDto>()
        every { db.queries.delMedBruker.getLast(dbo.norskIdent, id) } returns expected

        service.getLast(dbo.norskIdent, id) shouldBe expected
    }

    test("getAll delegerer til queries") {
        val expected = listOf(mockk<DelMedBrukerDto>())
        every { db.queries.delMedBruker.getAll(dbo.norskIdent) } returns expected

        service.getAll(dbo.norskIdent) shouldBe expected
    }
})
