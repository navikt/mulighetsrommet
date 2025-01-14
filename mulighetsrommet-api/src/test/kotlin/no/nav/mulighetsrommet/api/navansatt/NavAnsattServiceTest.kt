package no.nav.mulighetsrommet.api.navansatt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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

    val msGraph = mockk<MicrosoftGraphClient>()
    coEvery { msGraph.getGroupMembers(tiltaksadministrasjon.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getGroupMembers(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    fun createNavAnsattService(
        roles: List<AdGruppeNavAnsattRolleMapping>,
    ) = NavAnsattService(
        roles = roles,
        db = database.db,
        microsoftGraphClient = msGraph,
    )

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt with roles filtered by the configured roles") {
            val service = createNavAnsattService(listOf(tiltaksadministrasjon))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattFromAzure(azureId, AccessType.M2M) shouldBe NavAnsattDto.fromAzureAdNavAnsatt(
                ansatt1,
                setOf(TILTAKADMINISTRASJON_GENERELL),
            )
        }

        test("should fail when the requested NavAnsatt does not have any of the configured roles") {
            val service = createNavAnsattService(listOf(kontaktperson))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
            )

            shouldThrow<IllegalStateException> {
                service.getNavAnsattFromAzure(azureId, AccessType.M2M)
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
                        NavAnsattDto.fromAzureAdNavAnsatt(
                            ansatt2,
                            setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON),
                        ),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = createNavAnsattService(roles)

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
            coEvery { msGraph.getGroupMembers(id) } returns listOf(ansatt1, ansatt2)

            val service = createNavAnsattService(roles)

            val resolvedAnsatte = service.getNavAnsatteFromAzure()

            resolvedAnsatte shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL, KONTAKTPERSON)),
            )
        }
    }
})
