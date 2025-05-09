package no.nav.mulighetsrommet.api.navansatt.service

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
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.navansatt.model.Rolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetFixtures.Oslo,
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Gjovik,
        ),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
    )

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

    val adGruppeGenerell = UUID.randomUUID()
    val rolleGenerell = NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)
    val rolleMappingGenerell = AdGruppeNavAnsattRolleMapping(
        adGruppeId = adGruppeGenerell,
        rolle = rolleGenerell.rolle,
    )

    val adGruppeKontaktperson = UUID.randomUUID()
    val rolleKontaktperson = NavAnsattRolle.generell(KONTAKTPERSON)
    val rolleMappingKontaktperson = AdGruppeNavAnsattRolleMapping(
        adGruppeId = adGruppeKontaktperson,
        rolle = rolleKontaktperson.rolle,
    )

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
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val azureId = ansatt1.azureId

            coEvery { msGraph.getNavAnsatt(azureId, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(adGruppeKontaktperson)

            service.getNavAnsattFromAzure(azureId, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(setOf())
        }

        test("should get NavAnsatt by navIdent with roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val navIdent = ansatt1.navIdent
            val azureId = ansatt1.azureId

            coEvery { msGraph.getNavAnsattByNavIdent(navIdent, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(adGruppeGenerell)

            service.getNavAnsattFromAzure(navIdent, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(
                setOf(rolleGenerell),
            )
        }
    }

    context("getNavAnsattRoles") {
        test("should get NavAnsatt roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(adGruppeGenerell, UUID.randomUUID())

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(rolleGenerell)
        }

        test("should return empty set when the NavAnsatt does not have any of the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingKontaktperson))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(adGruppeGenerell)

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf()
        }

        test("should resolve Nav-enhet from the mapping") {
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0387")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterOslo))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(adGruppeBeslutterOslo)

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, enheter = setOf(NavEnhetNummer("0387"))),
            )
        }

        test("should resolve Nav-enhet with underliggende Nav-enheter from multiple groups") {
            MulighetsrommetTestDomain(
                navEnheter = listOf(
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Lillehammer,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
            ).initialize(database.db)

            val adGruppeBeslutterInnlandet = UUID.randomUUID()
            val rolleBeslutterInnlandet = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterInnlandet,
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0400")),
            )
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0300")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterInnlandet, rolleBeslutterOslo))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(
                adGruppeBeslutterInnlandet,
                adGruppeBeslutterOslo,
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(
                NavAnsattRolle.kontorspesifikk(
                    Rolle.BESLUTTER_TILSAGN,
                    enheter = setOf(
                        NavEnhetNummer("0400"),
                        NavEnhetNummer("0501"),
                        NavEnhetNummer("0502"),
                        NavEnhetNummer("0300"),
                        NavEnhetNummer("0387"),
                    ),
                ),
            )
        }

        test("should support both generell rolle and rolle for Nav-enhet") {
            val adGruppeBeslutterGenerell = UUID.randomUUID()
            val rolleBeslutterGenerell = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterGenerell,
                rolle = Rolle.BESLUTTER_TILSAGN,
            )
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = AdGruppeNavAnsattRolleMapping(
                adGruppeId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0387")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterGenerell, rolleBeslutterOslo))

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(azureId, any()) } returns listOf(
                adGruppeBeslutterGenerell,
                adGruppeBeslutterOslo,
            )

            service.getNavAnsattRoles(azureId, AccessType.M2M) shouldBe setOf(
                NavAnsattRolle(
                    rolle = Rolle.BESLUTTER_TILSAGN,
                    generell = true,
                    enheter = setOf(NavEnhetNummer("0387")),
                ),
            )
        }

        test("should support multiple roles from the same group") {
            val supertilgang = UUID.randomUUID()
            val roles = setOf(
                AdGruppeNavAnsattRolleMapping(supertilgang, TILTAKADMINISTRASJON_GENERELL),
                AdGruppeNavAnsattRolleMapping(supertilgang, KONTAKTPERSON),
            )

            coEvery { msGraph.getMemberGroups(ansatt1.azureId, any()) } returns listOf(supertilgang)

            val service = createNavAnsattService(roles)

            service.getNavAnsattRoles(ansatt1.azureId, AccessType.M2M) shouldBe setOf(
                rolleGenerell,
                rolleKontaktperson,
            )
        }
    }

    context("getNavAnsatteFromAzure") {
        coEvery { msGraph.getGroupMembers(rolleMappingGenerell.adGruppeId) } returns listOf(ansatt1, ansatt2)
        coEvery { msGraph.getGroupMembers(rolleMappingKontaktperson.adGruppeId) } returns listOf(ansatt2)

        coEvery { msGraph.getMemberGroups(ansatt1.azureId, any()) } returns listOf(adGruppeGenerell)
        coEvery { msGraph.getMemberGroups(ansatt2.azureId, any()) } returns listOf(
            adGruppeGenerell,
            adGruppeKontaktperson,
        )

        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    setOf(rolleMappingGenerell),
                    listOf(
                        ansatt1.toNavAnsatt(setOf(rolleGenerell)),
                        ansatt2.toNavAnsatt(setOf(rolleGenerell)),
                    ),
                ),
                row(
                    setOf(rolleMappingKontaktperson),
                    listOf(ansatt2.toNavAnsatt(setOf(rolleKontaktperson))),
                ),
                row(
                    setOf(rolleMappingGenerell, rolleMappingKontaktperson),
                    listOf(
                        ansatt1.toNavAnsatt(setOf(rolleGenerell)),
                        ansatt2.toNavAnsatt(setOf(rolleGenerell, rolleKontaktperson)),
                    ),
                ),
            ) { groups, expectedAnsatte ->
                runBlocking {
                    val service = createNavAnsattService(groups)

                    val groupIds = groups.map { it.adGruppeId }.toSet()
                    val resolvedAnsatte = service.getNavAnsatteInGroups(groupIds)

                    resolvedAnsatte shouldContainExactlyInAnyOrder expectedAnsatte
                }
            }
        }
    }
})
