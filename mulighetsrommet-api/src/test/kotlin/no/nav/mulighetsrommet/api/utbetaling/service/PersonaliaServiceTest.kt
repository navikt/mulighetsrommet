package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinClient
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResponse
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class PersonaliaServiceTest : FunSpec({
    val deltakelseId = UUID.randomUUID()
    val oppfolgingEnhet = NavEnhetFixtures.Sel
    val personalia = AmtDeltakerPersonalia(
        deltakerId = deltakelseId,
        norskIdent = NorskIdent("01010199999"),
        navn = "Normann, Ola",
        erSkjermet = false,
        adressebeskyttelse = PdlGradering.UGRADERT,
        oppfolgingEnhet = oppfolgingEnhet.enhetsnummer,
    )

    val hentPersonOgGeografiskTilknytningQuery = mockk<HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery>()
    val norg2Client = mockk<Norg2Client>()
    val amtDeltakerClient = mockk<AmtDeltakerClient>()
    val navEnhetService = mockk<NavEnhetService>()
    val tilgansmaskinClient = mockk<TilgangsmaskinClient>()

    context("skjermet og adressebeskyttet") {
        test("skjermet skjules") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                tilgansmaskinClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(
                personalia.copy(erSkjermet = true),
            ).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResponse(
                resultater = listOf(
                    TilgangsmaskinResponse.Resultat(
                        brukerId = personalia.norskIdent.value,
                        status = 403,
                    ),
                ),
            )

            coEvery { norg2Client.hentEnhetByGeografiskOmraade(any()) } returns Norg2EnhetDto(
                enhetId = 1,
                navn = "navn",
                enhetNr = oppfolgingEnhet.enhetsnummer,
                status = Norg2EnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
            ).right()
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns mapOf(
                PdlIdent(personalia.norskIdent.value) to Pair(
                    PdlPerson(
                        navn = "hans",
                        gradering = PdlGradering.UGRADERT,
                    ),
                    GeografiskTilknytning.GtKommune("asdf"),
                ),
            ).right()
            coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.enhetsnummer) } returns oppfolgingEnhet.toDto()
            coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.overordnetEnhet!!) } returns oppfolgingEnhet.toDto()
            service.getPersonalia(listOf(), AccessType.OBO.TokenX("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
            service.getPersonalia(listOf(), AccessType.OBO.AzureAd("azure"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe null
                it.navn shouldBe "Skjermet"
            }
            service.getPersonalia(listOf(), AccessType.M2M)[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
            service.getPersonaliaMedGeografiskEnhet(listOf(), AccessType.OBO.TokenX("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
                it.geografiskEnhet shouldBe oppfolgingEnhet.toDto()
            }
        }

        test("adressebeskyttet skjules") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                tilgansmaskinClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(
                personalia.copy(adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG_UTLAND),
            ).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResponse(
                resultater = listOf(
                    TilgangsmaskinResponse.Resultat(
                        brukerId = personalia.norskIdent.value,
                        status = 403,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()
            coEvery { navEnhetService.hentEnhet(NavEnhetNummer("0517")) } returns oppfolgingEnhet.toDto()

            service.getPersonalia(emptyList(), AccessType.OBO.TokenX("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = null,
                        navn = "Adressebeskyttet",
                        oppfolgingEnhet = null,
                        erSkjermet = false,
                        adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG_UTLAND,
                    ),
            )
            service.getPersonaliaMedGeografiskEnhet(listOf(), AccessType.OBO.TokenX("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe null
                it.navn shouldBe "Adressebeskyttet"
            }
            service.getPersonalia(listOf(), AccessType.M2M)[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
        }

        test("adressebeskyttelse tar presedens over skjerming") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                tilgansmaskinClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(
                personalia.copy(
                    erSkjermet = true,
                    adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG,
                ),
            ).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResponse(
                resultater = listOf(
                    TilgangsmaskinResponse.Resultat(
                        brukerId = personalia.norskIdent.value,
                        status = 403,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()
            coEvery { navEnhetService.hentEnhet(NavEnhetNummer("0517")) } returns NavEnhetDto(
                navn = "Nav Innlandet",
                enhetsnummer = NavEnhetNummer("0400"),
                type = NavEnhetType.FYLKE,
                overordnetEnhet = null,
            )
            service.getPersonalia(listOf(), AccessType.OBO.TokenX("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = null,
                        navn = "Adressebeskyttet",
                        oppfolgingEnhet = null,
                        erSkjermet = true,
                        adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG,
                    ),
            )
            service.getPersonaliaMedGeografiskEnhet(listOf(), AccessType.OBO.TokenX("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe null
                it.navn shouldBe "Adressebeskyttet"
            }
            service.getPersonalia(listOf(), AccessType.M2M)[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
        }

        test("når tilgangsmaskinen gir tilgang") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                tilgansmaskinClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(
                personalia.copy(
                    erSkjermet = true,
                    adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG,
                ),
            ).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResponse(
                resultater = listOf(
                    TilgangsmaskinResponse.Resultat(
                        brukerId = personalia.norskIdent.value,
                        status = 204,
                    ),
                ),
            )
            coEvery { norg2Client.hentEnhetByGeografiskOmraade(any()) } returns Norg2EnhetDto(
                enhetId = 1,
                navn = "navn",
                enhetNr = oppfolgingEnhet.enhetsnummer,
                status = Norg2EnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
            ).right()
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns mapOf(
                PdlIdent(personalia.norskIdent.value) to Pair(
                    PdlPerson(
                        navn = "hans",
                        gradering = PdlGradering.UGRADERT,
                    ),
                    GeografiskTilknytning.GtKommune("asdf"),
                ),
            ).right()
            coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.enhetsnummer) } returns oppfolgingEnhet.toDto()
            coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.overordnetEnhet!!) } returns oppfolgingEnhet.toDto()

            // tokenX gir ikke tilgang
            service.getPersonalia(listOf(), AccessType.OBO.TokenX("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = null,
                        navn = "Adressebeskyttet",
                        oppfolgingEnhet = null,
                        erSkjermet = true,
                        adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG,
                    ),
            )

            // AzureAd gir tilgang
            service.getPersonalia(listOf(), AccessType.OBO.AzureAd("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = personalia.norskIdent,
                        navn = personalia.navn,
                        oppfolgingEnhet = oppfolgingEnhet.toDto(),
                        erSkjermet = true,
                        adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG,
                    ),
            )
            service.getPersonaliaMedGeografiskEnhet(listOf(), AccessType.OBO.AzureAd("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
                it.geografiskEnhet shouldBe oppfolgingEnhet.toDto()
            }
            service.getPersonaliaMedGeografiskEnhet(listOf(), AccessType.OBO.TokenX("token"))[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe null
                it.navn shouldBe "Adressebeskyttet"
                it.geografiskEnhet shouldBe null
            }
            service.getPersonalia(listOf(), AccessType.M2M)[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
        }

        test("tilgangsmaskinen kan forby tilgang selv for ugradert og uskjermet") {
            val service = PersonaliaService(
                hentPersonOgGeografiskTilknytningQuery,
                norg2Client,
                amtDeltakerClient,
                tilgansmaskinClient,
                navEnhetService,
            )
            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResponse(
                resultater = listOf(
                    TilgangsmaskinResponse.Resultat(
                        brukerId = personalia.norskIdent.value,
                        status = 403,
                    ),
                ),
            )
            coEvery { navEnhetService.hentEnhet(NavEnhetNummer("0517")) } returns oppfolgingEnhet.toDto()

            // tokenX gir tilgang
            service.getPersonalia(listOf(), AccessType.OBO.TokenX("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = personalia.norskIdent,
                        navn = personalia.navn,
                        oppfolgingEnhet = oppfolgingEnhet.toDto(),
                        erSkjermet = personalia.erSkjermet,
                        adressebeskyttelse = personalia.adressebeskyttelse,
                    ),
            )

            // AzureAd gir ikke tilgang
            service.getPersonalia(listOf(), AccessType.OBO.AzureAd("token")) shouldBe mapOf(
                deltakelseId to
                    Personalia(
                        norskIdent = null,
                        navn = "Skjermet",
                        oppfolgingEnhet = null,
                        erSkjermet = personalia.erSkjermet,
                        adressebeskyttelse = personalia.adressebeskyttelse,
                    ),
            )
            service.getPersonalia(listOf(), AccessType.M2M)[deltakelseId] should {
                it shouldNotBe null
                it!!.norskIdent shouldBe personalia.norskIdent
                it.navn shouldBe personalia.navn
            }
        }
    }
})
