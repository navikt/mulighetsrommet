package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.*

class PersonaliaServiceTest : FunSpec({
    val deltakelseId = UUID.randomUUID()
    val personalia = DeltakerPersonalia(
        deltakerId = deltakelseId,
        norskIdent = NorskIdent("01010199999"),
        navn = "Normann, Ola",
        erSkjermet = false,
        adressebeskyttelse = PdlGradering.UGRADERT,
        oppfolgingEnhet = NavEnhetFixtures.Sel.enhetsnummer,
    )

    val hentPersonOgGeografiskTilknytningQuery = mockk<HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery>()
    val norg2Client = mockk<Norg2Client>()
    val amtDeltakerClient = mockk<AmtDeltakerClient>()
    val navEnhetService = mockk<NavEnhetService>()

    context("skjermet og adressebeskyttet") {
        test("skjermet skjules") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns listOf(
                personalia.copy(erSkjermet = true),
            ).right()
            coEvery { hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any()) } returns
                emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonaliaMedGeografiskEnhet(emptyList())[deltakelseId] shouldBe DeltakerPersonaliaMedGeografiskEnhet(
                deltakerId = deltakelseId,
                norskIdent = null,
                navn = "Skjermet",
                oppfolgingEnhet = null,
                geografiskEnhet = null,
                region = null,
            )
        }

        test("adressebeskyttet skjules") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns listOf(
                personalia.copy(adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG_UTLAND),
            ).right()
            coEvery { hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any()) } returns
                emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonaliaMedGeografiskEnhet(emptyList())[deltakelseId] shouldBe DeltakerPersonaliaMedGeografiskEnhet(
                deltakerId = deltakelseId,
                norskIdent = null,
                navn = "Adressebeskyttet",
                oppfolgingEnhet = null,
                geografiskEnhet = null,
                region = null,
            )
        }

        test("adressebeskyttelse tar presedens over skjerming") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns listOf(
                personalia.copy(
                    erSkjermet = true,
                    adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG_UTLAND,
                ),
            ).right()
            coEvery { hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any()) } returns
                emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonaliaMedGeografiskEnhet(emptyList())[deltakelseId] shouldBe DeltakerPersonaliaMedGeografiskEnhet(
                deltakerId = deltakelseId,
                norskIdent = null,
                navn = "Adressebeskyttet",
                oppfolgingEnhet = null,
                geografiskEnhet = null,
                region = null,
            )
        }
    }
})
