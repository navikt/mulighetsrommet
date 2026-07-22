package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.navenhet.GetNavEnhet
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDtoQuery
import no.nav.mulighetsrommet.admin.navenhet.toDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinClient
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResult
import no.nav.mulighetsrommet.api.deltaker.client.AmtDeltakerClient
import no.nav.mulighetsrommet.api.deltaker.client.AmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
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
    val navEnhetDtoQuery = mockk<NavEnhetDtoQuery>()
    val tilgansmaskinClient = mockk<TilgangsmaskinClient>()

    fun createPersonaliaService(): PersonaliaService = PersonaliaService(
        hentPersonOgGeografiskTilknytningQuery,
        norg2Client,
        amtDeltakerClient,
        tilgansmaskinClient,
        navEnhetDtoQuery,
    )

    context("skjermet og adressebeskyttet") {
        coEvery { navEnhetDtoQuery.execute(GetNavEnhet(oppfolgingEnhet.enhetsnummer)) } returns oppfolgingEnhet.toDto()
        coEvery { navEnhetDtoQuery.execute(GetNavEnhet(oppfolgingEnhet.overordnetEnhet!!)) } returns oppfolgingEnhet.toDto()

        test("skjermet skjules") {
            val service = createPersonaliaService()

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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("azure"))) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe "Skjermet"
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
                it.geografiskEnhet() shouldBe oppfolgingEnhet.toDto()
            }
        }

        test("adressebeskyttet skjules") {
            val service = createPersonaliaService()

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

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_HABILITET
                it.navn() shouldBe "Adressebeskyttet"
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe "Adressebeskyttet"
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
        }

        test("adressebeskyttelse tar presedens over skjerming") {
            val service = createPersonaliaService()

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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
                it.navn() shouldBe "Adressebeskyttet"
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe "Adressebeskyttet"
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
        }

        test("når tilgangsmaskinen gir tilgang") {
            val service = createPersonaliaService()

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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
                it.navn() shouldBe "Adressebeskyttet"
                it.norskIdent() shouldBe null
                it.oppfolgingEnhet() shouldBe oppfolgingEnhet.toDto()
            }

            // AzureAd gir tilgang
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) shouldBe
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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it shouldNotBe null
                it.norskIdent() shouldBe null
                it.navn() shouldBe "Adressebeskyttet"
                it.geografiskEnhet() shouldBe null
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
        }

        test("tilgangsmaskinen kan forby tilgang selv for ugradert og uskjermet") {
            val service = createPersonaliaService()

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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) shouldBe
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
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_SKJERMING
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it shouldNotBe null
                it.norskIdent() shouldBe personalia.norskIdent
                it.navn() shouldBe personalia.navn
            }
        }

        test("geografisk avvisning fra tilgangsmaskin gir Gradering.GEOGRAFISK") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_GEOGRAFISK,
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
                    PdlPerson(navn = "Normann, Ola", gradering = PdlGradering.UGRADERT),
                    GeografiskTilknytning.GtKommune("0301"),
                ),
            ).right()

            // NavAnsatt uten geografisk tilgang: avvist, gradering GEOGRAFISK, data skjult
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_GEOGRAFISK
                it.gradering shouldBe Gradering.GEOGRAFISK
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
                it.geografiskEnhet() shouldBe null
                it.region() shouldBe null
                // oppfolgingEnhet vises selv uten tilgang
                it.oppfolgingEnhet() shouldBe oppfolgingEnhet.toDto()
            }

            // Arrangor: UGRADERT person → ikke avvist for arrangør
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it.avvistGrunn shouldBe null
                it.gradering shouldBe Gradering.UGRADERT
                it.navn() shouldBe personalia.navn
                it.norskIdent() shouldBe personalia.norskIdent
                it.geografiskEnhet() shouldBe oppfolgingEnhet.toDto()
            }

            // System: alltid full tilgang
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it.avvistGrunn shouldBe null
                it.gradering shouldBe Gradering.UGRADERT
                it.navn() shouldBe personalia.navn
                it.norskIdent() shouldBe personalia.norskIdent
            }
        }

        test("AVVIST_VERGE fra tilgangsmaskin gir Gradering.SKJERMING og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_VERGE,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_VERGE
                it.gradering shouldBe Gradering.SKJERMING
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("AVVIST_HABILITET fra tilgangsmaskin gir Gradering.SKJERMING og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
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

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_HABILITET
                it.gradering shouldBe Gradering.SKJERMING
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("FORTROLIG adressebeskyttelse avvises for arrangør") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(
                personalia.copy(adressebeskyttelse = PdlGradering.FORTROLIG),
            ).right()
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.Arrangor) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
                it.gradering shouldBe Gradering.FORTROLIG_ADRESSE
                it.navn() shouldBe "Adressebeskyttet"
                it.norskIdent() shouldBe null
            }
            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.System) should {
                it.avvistGrunn shouldBe null
                it.navn() shouldBe personalia.navn
                it.norskIdent() shouldBe personalia.norskIdent
            }
        }

        test("AVVIST_VERGEMAAL fra tilgangsmaskin gir Gradering.SKJERMING og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_VERGEMAAL,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_VERGEMAAL
                it.gradering shouldBe Gradering.SKJERMING
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("AVVIST_AVDOED fra tilgangsmaskin gir Gradering.SKJERMING og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_AVDOED,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_AVDOED
                it.gradering shouldBe Gradering.SKJERMING
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("AVVIST_PERSON_UTLAND fra tilgangsmaskin gir Gradering.GEOGRAFISK og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_PERSON_UTLAND,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_PERSON_UTLAND
                it.gradering shouldBe Gradering.GEOGRAFISK
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("AVVIST_UKJENT_BOSTED fra tilgangsmaskin gir Gradering.GEOGRAFISK og skjermet navn") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Avvist(
                        brukerId = personalia.norskIdent.value,
                        grunn = TilgangsmaskinResult.AvvistGrunn.AVVIST_UKJENT_BOSTED,
                    ),
                ),
            )
            coEvery {
                hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(any(), any())
            } returns emptyMap<PdlIdent, Pair<PdlPerson, GeografiskTilknytning?>>().right()

            service.getPersonalia(deltakerId, PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token"))) should {
                it.avvistGrunn shouldBe AvvistGrunn.AVVIST_UKJENT_BOSTED
                it.gradering shouldBe Gradering.GEOGRAFISK
                it.navn() shouldBe "Skjermet"
                it.norskIdent() shouldBe null
            }
        }

        test("ugradert og uskjermet person får full tilgang for alle") {
            val service = createPersonaliaService()

            coEvery { amtDeltakerClient.hentPersonalia(any()) } returns setOf(personalia).right()
            coEvery { tilgansmaskinClient.bulk(listOf(personalia.norskIdent), any()) } returns TilgangsmaskinResult(
                resultater = listOf(
                    TilgangsmaskinResult.Resultat.Innvilget(brukerId = personalia.norskIdent.value),
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
                    PdlPerson(navn = personalia.navn, gradering = PdlGradering.UGRADERT),
                    GeografiskTilknytning.GtKommune("0301"),
                ),
            ).right()

            listOf(
                PersonaliaService.OnBehalfOf.Arrangor,
                PersonaliaService.OnBehalfOf.NavAnsatt(AccessType.OBO.AzureAd("token")),
                PersonaliaService.OnBehalfOf.System,
            ).forEach { onBehalfOf ->
                service.getPersonalia(deltakerId, onBehalfOf) should {
                    it.avvistGrunn shouldBe null
                    it.gradering shouldBe Gradering.UGRADERT
                    it.navn() shouldBe personalia.navn
                    it.norskIdent() shouldBe personalia.norskIdent
                    it.geografiskEnhet() shouldBe oppfolgingEnhet.toDto()
                    it.region() shouldBe oppfolgingEnhet.toDto()
                }
            }
        }
    }
})
