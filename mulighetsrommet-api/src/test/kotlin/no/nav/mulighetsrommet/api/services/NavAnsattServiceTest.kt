package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.util.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.AVTALER_SKRIV
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Lopenummer
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.LocalDate
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    fun toAzureAdNavAnsattDto(dbo: NavAnsattDbo) = AzureAdNavAnsatt(
        azureId = dbo.azureId,
        navIdent = dbo.navIdent,
        fornavn = dbo.fornavn,
        etternavn = dbo.etternavn,
        hovedenhetKode = domain.enheter[0].enhetsnummer,
        hovedenhetNavn = domain.enheter[0].navn,
        mobilnummer = dbo.mobilnummer,
        epost = dbo.epost,
    )

    val ansatt1 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt1)
    val ansatt2 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt2)

    val tiltaksadministrasjon = AdGruppeNavAnsattRolleMapping(
        adGruppeId = UUID.randomUUID(),
        rolle = TILTAKADMINISTRASJON_GENERELL,
    )
    val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)
    val avtalerSkriv = AdGruppeNavAnsattRolleMapping(
        adGruppeId = UUID.randomUUID(),
        rolle = AVTALER_SKRIV,
    )

    val msGraph = mockk<MicrosoftGraphService>()
    coEvery { msGraph.getNavAnsatteInGroup(tiltaksadministrasjon.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getNavAnsatteInGroup(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    val avtaleRepository: AvtaleRepository = mockk()
    val navEnhetService: NavEnhetService = mockk()
    val notificationService: NotificationService = mockk()

    val sanityClient = SanityClient(
        engine = MockEngine { request ->
            if (request.method === HttpMethod.Post) {
                return@MockEngine respondOk()
            }

            val query = request.url.parameters.getOrFail<String>("query")
            if (query.contains("redaktor") && query.contains("navKontaktperson")) {
                respondJson(
                    content = sanityContentResult(listOf("123", "456")),
                )
            } else if (query.contains("redaktor")) {
                respondJson(
                    content = sanityContentResult(
                        listOf(
                            SanityRedaktor(
                                _id = "123",
                                _type = "redaktor",
                                enhet = "",
                                epost = Slug(current = "epost@epost.no"),
                                navIdent = Slug(current = "N123"),
                                navn = "Navn Navnesen",
                            ),
                        ),
                    ),
                )
            } else if (query.contains("navKontaktperson")) {
                respondJson(
                    content = sanityContentResult(
                        listOf(
                            SanityNavKontaktperson(
                                _id = "123",
                                _type = "navKontaktperson",
                                enhet = "",
                                telefonnummer = null,
                                epost = "navn.navnesen@nav.no",
                                navn = "Navn Navnesen",
                                navIdent = Slug(current = "N123"),
                            ),
                        ),
                    ),
                )
            } else {
                respondError(status = HttpStatusCode.BadRequest)
            }
        },
        config = SanityClient.Config("", "", "", "", false),
    )

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt with roles filtered by the configured roles") {
            val service = NavAnsattService(
                roles = listOf(tiltaksadministrasjon),
                db = database.db,
                microsoftGraphService = msGraph,
                navAnsattRepository = NavAnsattRepository(database.db),
                sanityClient = sanityClient,
                avtaleRepository = avtaleRepository,
                navEnhetService = navEnhetService,
                notificationService = notificationService,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattFromAzure(azureId) shouldBe NavAnsattDto.fromAzureAdNavAnsatt(
                ansatt1,
                setOf(TILTAKADMINISTRASJON_GENERELL),
            )
        }

        test("should fail when the requested NavAnsatt does not have any of the configured roles") {
            val service = NavAnsattService(
                roles = listOf(kontaktperson),
                db = database.db,
                microsoftGraphService = msGraph,
                navAnsattRepository = NavAnsattRepository(database.db),
                sanityClient = sanityClient,
                avtaleRepository = avtaleRepository,
                navEnhetService = navEnhetService,
                notificationService = notificationService,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
            )

            shouldThrow<IllegalStateException> {
                service.getNavAnsattFromAzure(azureId)
            }
        }
    }

    context("getNavAnsatteFromAzure") {
        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    listOf(tiltaksadministrasjon),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON))),
                ),
                row(
                    listOf(tiltaksadministrasjon, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        roles = roles,
                        db = database.db,
                        microsoftGraphService = msGraph,
                        navAnsattRepository = NavAnsattRepository(database.db),
                        sanityClient = sanityClient,
                        avtaleRepository = avtaleRepository,
                        navEnhetService = navEnhetService,
                        notificationService = notificationService,
                    )

                    val resolvedAnsatte = service.getNavAnsatteFromAzure()

                    resolvedAnsatte shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }

        test("should support multiple roles from the same group") {
            val id = UUID.randomUUID()
            val roles = listOf(
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = TILTAKADMINISTRASJON_GENERELL),
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = KONTAKTPERSON),
            )
            coEvery { msGraph.getNavAnsatteInGroup(id) } returns listOf(ansatt1, ansatt2)

            val service = NavAnsattService(
                roles = roles,
                db = database.db,
                microsoftGraphService = msGraph,
                navAnsattRepository = NavAnsattRepository(database.db),
                sanityClient = sanityClient,
                avtaleRepository = avtaleRepository,
                navEnhetService = navEnhetService,
                notificationService = notificationService,
            )

            val resolvedAnsatte = service.getNavAnsatteFromAzure()

            resolvedAnsatte shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
            )
        }
    }

    context("synchronizeNavAnsatteFromAzure") {
        val ansatte = NavAnsattRepository(database.db)

        test("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
            val today = LocalDate.now()
            val deletionDate = today.plusDays(1)

            forAll(
                row(
                    listOf(tiltaksadministrasjon, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(tiltaksadministrasjon),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        roles = roles,
                        db = database.db,
                        microsoftGraphService = msGraph,
                        navAnsattRepository = ansatte,
                        sanityClient = sanityClient,
                        avtaleRepository = avtaleRepository,
                        navEnhetService = navEnhetService,
                        notificationService = notificationService,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate)

                    ansatte.getAll() shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }

        test("should delete nav_ansatt when their deletion date matches the provided deletion date") {
            every { avtaleRepository.getAvtaleIdsByAdministrator(any()) } returns emptyList()
            val today = LocalDate.now()

            forAll(
                row(
                    listOf(tiltaksadministrasjon, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        roles = roles,
                        db = database.db,
                        microsoftGraphService = msGraph,
                        navAnsattRepository = ansatte,
                        sanityClient = sanityClient,
                        avtaleRepository = avtaleRepository,
                        navEnhetService = navEnhetService,
                        notificationService = notificationService,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate = today)

                    ansatte.getAll() shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }

        test("varsler administratorer basert på hovedenhet når avtale ikke lengre har administrator") {
            every { avtaleRepository.getAvtaleIdsByAdministrator(ansatt1.navIdent) } returns listOf(AvtaleFixtures.AFT.id)
            every { avtaleRepository.get(AvtaleFixtures.AFT.id) } returns AvtaleAdminDto(
                id = AvtaleFixtures.AFT.id,
                navn = AvtaleFixtures.AFT.navn,
                avtalenummer = AvtaleFixtures.AFT.avtalenummer,
                tiltakstype = AvtaleAdminDto.Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "",
                    arenaKode = "",
                ),
                arrangor = AvtaleAdminDto.ArrangorHovedenhet(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = "123",
                    navn = "navn",
                    slettet = false,
                    underenheter = emptyList(),
                    kontaktpersoner = emptyList(),
                ),
                startDato = AvtaleFixtures.AFT.startDato,
                sluttDato = AvtaleFixtures.AFT.sluttDato,
                avtaletype = AvtaleFixtures.AFT.avtaletype,
                prisbetingelser = AvtaleFixtures.AFT.prisbetingelser,
                antallPlasser = AvtaleFixtures.AFT.antallPlasser,
                url = AvtaleFixtures.AFT.url,
                beskrivelse = AvtaleFixtures.AFT.beskrivelse,
                faneinnhold = AvtaleFixtures.AFT.faneinnhold,
                personopplysninger = AvtaleFixtures.AFT.personopplysninger,
                personvernBekreftet = AvtaleFixtures.AFT.personvernBekreftet,
                arenaAnsvarligEnhet = null,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                avtalestatus = Avtalestatus.AKTIV,
                lopenummer = Lopenummer(value = "2024/1"),
                administratorer = emptyList(), // eneste verdien som er relevant her
                kontorstruktur = emptyList(),
            )

            every { navEnhetService.hentOverordnetFylkesenhet(any()) } returns
                NavEnhetDbo(
                    navn = ansatt2.hovedenhetNavn,
                    enhetsnummer = ansatt2.hovedenhetKode,
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                )

            every { navEnhetService.hentAlleEnheter(any()) } returns listOf(
                NavEnhetDbo(
                    navn = ansatt2.hovedenhetNavn,
                    enhetsnummer = ansatt2.hovedenhetKode,
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            )

            every { notificationService.scheduleNotification(any(), any()) } returns Unit
            coEvery { msGraph.getNavAnsatteInGroup(avtalerSkriv.adGruppeId) } returns listOf(ansatt2)

            val today = LocalDate.now()
            val service = NavAnsattService(
                roles = listOf(avtalerSkriv), // Sletter ansatt1 siden den ikke returneres i mocken over
                db = database.db,
                microsoftGraphService = msGraph,
                navAnsattRepository = ansatte,
                sanityClient = sanityClient,
                avtaleRepository = avtaleRepository,
                navEnhetService = navEnhetService,
                notificationService = notificationService,
            )
            service.synchronizeNavAnsatte(today, deletionDate = today)

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(ansatt2.navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }
    }
})

inline fun <reified T> sanityContentResult(value: T): SanityResponse.Result {
    return SanityResponse.Result(ms = 100, query = "", result = Json.encodeToJsonElement(value))
}
