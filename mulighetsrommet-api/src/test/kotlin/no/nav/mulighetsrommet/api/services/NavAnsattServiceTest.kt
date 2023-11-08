package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.util.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDate
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
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

    val betabruker = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = BETABRUKER)
    val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

    val msGraph = mockk<MicrosoftGraphService>()
    coEvery { msGraph.getNavAnsatteInGroup(betabruker.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getNavAnsatteInGroup(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    val sanityClient = SanityClient(
        engine = MockEngine { request ->
            if (request.method === HttpMethod.Post) {
                respondOk()
            } else if (request.method === HttpMethod.Delete) {
                respondOk()
            } else if (request.url.parameters.getOrFail<String>("query")
                    .contains("redaktor") && request.url.parameters.getOrFail<String>("query")
                    .contains("navKontaktperson")
            ) {
                respondJson(
                    content = sanityContentResult(listOf("123", "456")),
                )
            } else if (request.url.parameters.getOrFail<String>("query").contains("redaktor")) {
                respondJson(
                    content = sanityContentResult(
                        SanityRedaktor(
                            _id = "123",
                            _type = "navKontaktperson",
                            enhet = "",
                            epost = Slug(_type = "slug", current = "epost@epost.no"),
                            navn = "Navn Navnesen",
                        ),
                    ),
                )
            } else if (request.url.parameters.getOrFail("query").contains("navKontaktperson")) {
                respondJson(
                    content = sanityContentResult(
                        SanityNavKontaktperson(
                            _id = "123",
                            _type = "navKontaktperson",
                            enhet = "",
                            telefonnummer = null,
                            epost = "",
                            navn = "Navn Navnesen",
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
                microsoftGraphService = msGraph,
                ansatte = NavAnsattRepository(database.db),
                roles = listOf(betabruker),
                sanityClient = sanityClient,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId) } returns listOf(
                AdGruppe(id = betabruker.adGruppeId, navn = "Betabruker"),
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattFromAzure(azureId) shouldBe NavAnsattDto.fromAzureAdNavAnsatt(
                ansatt1,
                setOf(BETABRUKER),
            )
        }

        test("should fail when the requested NavAnsatt does not have any of the configured roles") {
            val service = NavAnsattService(
                microsoftGraphService = msGraph,
                ansatte = NavAnsattRepository(database.db),
                roles = listOf(kontaktperson),
                sanityClient = sanityClient,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId) } returns listOf(
                AdGruppe(id = betabruker.adGruppeId, navn = "Betabruker"),
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
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON))),
                ),
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        microsoftGraphService = msGraph,
                        ansatte = NavAnsattRepository(database.db),
                        roles = roles,
                        sanityClient = sanityClient,
                    )

                    val resolvedAnsatte = service.getNavAnsatteFromAzure()

                    resolvedAnsatte shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }
    }

    context("synchronizeNavAnsatteFromAzure") {
        val ansatte = NavAnsattRepository(database.db)

        test("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
            val today = LocalDate.now()
            val deletionDate = today.plusDays(1)

            forAll(
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER)),
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
                        microsoftGraphService = msGraph,
                        ansatte = ansatte,
                        roles = roles,
                        sanityClient = sanityClient,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }

        test("should delete nav_ansatt when their deletion date matches the provided deletion date") {
            val today = LocalDate.now()

            forAll(
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
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
                        microsoftGraphService = msGraph,
                        ansatte = ansatte,
                        roles = roles,
                        sanityClient = sanityClient,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate = today).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }
    }
})

inline fun <reified T> sanityContentResult(value: T): SanityResponse.Result {
    return SanityResponse.Result(ms = 100, query = "", result = Json.encodeToJsonElement(value))
}
