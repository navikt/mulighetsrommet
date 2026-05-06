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
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResult
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class PersonaliaServiceTest : FunSpec({
    val deltakerId = UUID.randomUUID()
    val oppfolgingEnhet = NavEnhetFixtures.Sel
    val personalia = AmtDeltakerPersonalia(
        deltakerId = deltakerId,
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
        coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.enhetsnummer) } returns oppfolgingEnhet.toDto()
        coEvery { navEnhetService.hentEnhet(oppfolgingEnhet.overordnetEnhet!!) } returns oppfolgingEnhet.toDto()

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
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE,
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
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
            service.getPersonalia(deltakerId, AccessType.OBO.AzureAd("azure")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.M2M) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
                it.geografiskEnhet() shouldBe oppfolgingEnhet.toDto()
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
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_HABILITET,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, AccessType.OBO.AzureAd("token")) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_HABILITET
                it.navn() shouldBe null
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.M2M) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
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
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
                it.navn() shouldBe null
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.M2M) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
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
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Innvilget(
                        brukerId = personalia.norskIdent.value,
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

            // tokenX gir ikke tilgang
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
                it.navn() shouldBe null
                it.norskIdent() shouldBe null
                it.oppfolgingEnhet() shouldBe null
            }

            // AzureAd gir tilgang
            service.getPersonalia(deltakerId, AccessType.OBO.AzureAd("token")) shouldBe
                Personalia(
                    deltakerId,
                    norskIdent = personalia.norskIdent,
                    navn = personalia.navn,
                    oppfolgingEnhet = oppfolgingEnhet.toDto(),
                    gradering = Gradering.STRENGT_FORTROLIG_ADRESSE,
                    geografiskEnhet = oppfolgingEnhet.toDto(),
                    region = oppfolgingEnhet.toDto(),
                    avvistGrunn = null,
                )
            service.getPersonalia(deltakerId, AccessType.OBO.AzureAd("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
                it.geografiskEnhet() shouldBe oppfolgingEnhet.toDto()
            }
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe null
                it.geografiskEnhet() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.M2M) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
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
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_SKJERMING,
                    ),
                ),
            )

            // tokenX gir tilgang
            service.getPersonalia(deltakerId, AccessType.OBO.TokenX("token")) shouldBe
                Personalia(
                    deltakerId,
                    norskIdent = personalia.norskIdent,
                    navn = personalia.navn,
                    oppfolgingEnhet = oppfolgingEnhet.toDto(),
                    gradering = Gradering.UGRADERT,
                    geografiskEnhet = oppfolgingEnhet.toDto(),
                    region = oppfolgingEnhet.toDto(),
                    avvistGrunn = null,
                )

            // AzureAd gir ikke tilgang
            service.getPersonalia(deltakerId, AccessType.OBO.AzureAd("token")) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_SKJERMING
                it.navn() shouldBe null
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, AccessType.M2M) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
        }
    }
})
