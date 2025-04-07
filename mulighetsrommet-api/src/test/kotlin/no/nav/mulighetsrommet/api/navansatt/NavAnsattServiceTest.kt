package no.nav.mulighetsrommet.api.navansatt

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.msgraph.AdGruppe
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
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
        hovedenhetKode = domain.navEnheter[0].enhetsnummer,
        hovedenhetNavn = domain.navEnheter[0].navn,
        mobilnummer = dbo.mobilnummer,
        epost = dbo.epost,
    )

    val ansatt1 = toAzureAdNavAnsattDto(NavAnsattFixture.DonaldDuck)
    val ansatt2 = toAzureAdNavAnsattDto(NavAnsattFixture.MikkeMus)

    val tiltaksadministrasjon = AdGruppeNavAnsattRolleMapping(
        adGruppeId = UUID.randomUUID(),
        rolle = TILTAKADMINISTRASJON_GENERELL,
    )
    val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

    val msGraph = mockk<MicrosoftGraphClient>()
    coEvery { msGraph.getGroupMembers(tiltaksadministrasjon.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getGroupMembers(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    fun createNavAnsattService(
        roles: Set<AdGruppeNavAnsattRolleMapping>,
    ) = NavAnsattService(
        roles = roles,
        db = database.db,
        microsoftGraphClient = msGraph,
    )

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt by azure id") {
            val service = createNavAnsattService(setOf(tiltaksadministrasjon))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1

            service.getNavAnsattFromAzure(azureId, AccessType.M2M) shouldBe NavAnsatt.fromAzureAdNavAnsatt(ansatt1)
        }
    }

    context("getNavAnsattRoles") {
        test("should get NavAnsatt roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(tiltaksadministrasjon))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(Rolle.TiltakadministrasjonGenerell)
        }

        test("should return empty set when the NavAnsatt does not have any of the configured roles") {
            val service = createNavAnsattService(setOf(kontaktperson))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = tiltaksadministrasjon.adGruppeId, navn = "Tiltaksadministrasjon generell"),
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf()
        }

        test("should resolve Nav-enheter from the group name") {
            val beslutter = AdGruppeNavAnsattRolleMapping(
                adGruppeId = UUID.randomUUID(),
                rolle = NavAnsattRolle.BESLUTTER_TILSAGN,
            )

            val service = createNavAnsattService(setOf(beslutter))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                AdGruppe(id = beslutter.adGruppeId, navn = "0400-CA-TILTAK-beslutter_tilsagn"),
                AdGruppe(id = beslutter.adGruppeId, navn = "0300-CA-TILTAK-beslutter_tilsagn"),
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(
                Rolle.BeslutterTilsagn(enheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0300"))),
            )
        }
    }

    context("getNavAnsatteFromAzure") {
        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    setOf(tiltaksadministrasjon),
                    listOf(
                        NavAnsatt.fromAzureAdNavAnsatt(ansatt1),
                        NavAnsatt.fromAzureAdNavAnsatt(ansatt2),
                    ),
                ),
                row(
                    setOf(kontaktperson),
                    listOf(
                        NavAnsatt.fromAzureAdNavAnsatt(ansatt2),
                    ),
                ),
                row(
                    setOf(tiltaksadministrasjon, kontaktperson),
                    listOf(
                        NavAnsatt.fromAzureAdNavAnsatt(ansatt1),
                        NavAnsatt.fromAzureAdNavAnsatt(ansatt2),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = createNavAnsattService(roles)

                    val resolvedAnsatte = service.getNavAnsatteInGroups(roles.toSet())

                    resolvedAnsatte shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }

        test("should support multiple roles from the same group") {
            val id = UUID.randomUUID()
            val roles = setOf(
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = TILTAKADMINISTRASJON_GENERELL),
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = KONTAKTPERSON),
            )
            coEvery { msGraph.getGroupMembers(id) } returns listOf(ansatt1, ansatt2)

            val service = createNavAnsattService(roles)

            val resolvedAnsatte = service.getNavAnsatteInGroups(roles.toSet())

            resolvedAnsatte shouldContainExactlyInAnyOrder listOf(
                NavAnsatt.fromAzureAdNavAnsatt(ansatt1),
                NavAnsatt.fromAzureAdNavAnsatt(ansatt2),
            )
        }
    }
})
