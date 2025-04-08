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

    val rolleGenerell = AdGruppeNavAnsattRolleMapping(
        adGruppeId = UUID.randomUUID(),
        rolle = TILTAKADMINISTRASJON_GENERELL,
    )
    val adGruppeGenerell = AdGruppe(id = rolleGenerell.adGruppeId, navn = "Generell")

    val rolleKontaktperson = AdGruppeNavAnsattRolleMapping(
        adGruppeId = UUID.randomUUID(),
        rolle = KONTAKTPERSON,
    )
    val adGruppeKontaktperson = AdGruppe(id = rolleKontaktperson.adGruppeId, navn = "Kontaktperson")

    val msGraph = mockk<MicrosoftGraphClient>()

    fun createNavAnsattService(
        roles: Set<AdGruppeNavAnsattRolleMapping>,
    ) = NavAnsattService(
        roles = roles,
        db = database.db,
        microsoftGraphClient = msGraph,
    )

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt by azureId with roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleGenerell))

            val azureId = ansatt1.azureId

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(adGruppeKontaktperson)

            service.getNavAnsattFromAzure(azureId, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(setOf())
        }

        test("should get NavAnsatt by navIdent with roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleGenerell))

            val navIdent = ansatt1.navIdent
            val azureId = ansatt1.azureId

            coEvery { msGraph.getNavAnsattByNavIdent(navIdent, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(adGruppeGenerell)

            service.getNavAnsattFromAzure(navIdent, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(
                setOf(Rolle.TiltakadministrasjonGenerell),
            )
        }
    }

    context("getNavAnsattRoles") {
        test("should get NavAnsatt roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleGenerell))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                adGruppeGenerell,
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(Rolle.TiltakadministrasjonGenerell)
        }

        test("should return empty set when the NavAnsatt does not have any of the configured roles") {
            val service = createNavAnsattService(setOf(rolleKontaktperson))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(adGruppeGenerell)

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf()
        }

        test("should resolve Nav-enheter from the group name") {
            val adGruppeBeslutterInnlandet = AdGruppe(id = UUID.randomUUID(), navn = "0400-CA-TILTAK-beslutter_tilsagn")
            val adGruppeBeslutterOslo = AdGruppe(id = UUID.randomUUID(), navn = "0300-CA-TILTAK-beslutter_tilsagn")

            val rolleBeslutterInnlandet = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterInnlandet.id,
                rolle = NavAnsattRolle.BESLUTTER_TILSAGN,
            )
            val rolleBeslutterOslo = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterOslo.id,
                rolle = NavAnsattRolle.BESLUTTER_TILSAGN,
            )

            val service = createNavAnsattService(setOf(rolleBeslutterInnlandet, rolleBeslutterOslo))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, AccessType.M2M) } returns listOf(
                adGruppeBeslutterInnlandet,
                adGruppeBeslutterOslo,
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(
                Rolle.BeslutterTilsagn(enheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0300"))),
            )
        }

        test("should support multiple roles from the same group") {
            val id = UUID.randomUUID()

            val supertilgang = AdGruppe(id = id, navn = "Supertilgang")
            val roles = setOf(
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = TILTAKADMINISTRASJON_GENERELL),
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = KONTAKTPERSON),
            )

            coEvery { msGraph.getMemberGroups(ansatt1.azureId, AccessType.M2M) } returns listOf(supertilgang)

            val service = createNavAnsattService(roles)

            service.getNavAnsattRoles(ansatt1.azureId, AccessType.M2M) shouldBe setOf(
                Rolle.TiltakadministrasjonGenerell,
                Rolle.Kontaktperson,
            )
        }
    }

    context("getNavAnsatteFromAzure") {
        coEvery { msGraph.getGroupMembers(rolleGenerell.adGruppeId) } returns listOf(ansatt1, ansatt2)
        coEvery { msGraph.getGroupMembers(rolleKontaktperson.adGruppeId) } returns listOf(ansatt2)

        coEvery { msGraph.getMemberGroups(ansatt1.azureId, AccessType.M2M) } returns listOf(adGruppeGenerell)
        coEvery { msGraph.getMemberGroups(ansatt2.azureId, AccessType.M2M) } returns listOf(
            adGruppeGenerell,
            adGruppeKontaktperson,
        )

        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    setOf(rolleGenerell),
                    listOf(
                        ansatt1.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell)),
                        ansatt2.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell)),
                    ),
                ),
                row(
                    setOf(rolleKontaktperson),
                    listOf(ansatt2.toNavAnsatt(setOf(Rolle.Kontaktperson))),
                ),
                row(
                    setOf(rolleGenerell, rolleKontaktperson),
                    listOf(
                        ansatt1.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell)),
                        ansatt2.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell, Rolle.Kontaktperson)),
                    ),
                ),
            ) { groups, expectedAnsatte ->
                runBlocking {
                    val service = createNavAnsattService(groups)

                    val resolvedAnsatte = service.getNavAnsatteInGroups(groups.toSet())

                    resolvedAnsatte shouldContainExactlyInAnyOrder expectedAnsatte
                }
            }
        }

        test("should support multiple roles from the same group") {
            val id = UUID.randomUUID()

            val supertilgang = AdGruppe(id = id, navn = "Supertilgang")
            val roles = setOf(
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = TILTAKADMINISTRASJON_GENERELL),
                AdGruppeNavAnsattRolleMapping(adGruppeId = id, rolle = KONTAKTPERSON),
            )

            coEvery { msGraph.getGroupMembers(id) } returns listOf(ansatt1, ansatt2)

            coEvery { msGraph.getMemberGroups(ansatt1.azureId, AccessType.M2M) } returns listOf(supertilgang)
            coEvery { msGraph.getMemberGroups(ansatt2.azureId, AccessType.M2M) } returns listOf(supertilgang)

            val service = createNavAnsattService(roles)

            val resolvedAnsatte = service.getNavAnsatteInGroups(roles.toSet())

            resolvedAnsatte shouldContainExactlyInAnyOrder listOf(
                ansatt1.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell, Rolle.Kontaktperson)),
                ansatt2.toNavAnsatt(setOf(Rolle.TiltakadministrasjonGenerell, Rolle.Kontaktperson)),
            )
        }
    }
})
