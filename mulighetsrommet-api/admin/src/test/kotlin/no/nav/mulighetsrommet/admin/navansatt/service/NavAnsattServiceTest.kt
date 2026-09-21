package no.nav.mulighetsrommet.admin.navansatt.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.kostnadssted.Kostnadssted
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.clients.msgraph.EntraNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MsGraphClient
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle.TILTAKADMINISTRASJON_GENERELL
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class NavAnsattServiceTest : FunSpec({
    val db = TestAdminDatabase()

    fun toEntraNavAnsatt(ansatt: NavAnsatt) = EntraNavAnsatt(
        entraObjectId = ansatt.entraObjectId,
        navIdent = ansatt.navIdent,
        fornavn = ansatt.fornavn,
        etternavn = ansatt.etternavn,
        hovedenhetKode = NavEnhetFixtures.Oslo.enhetsnummer,
        hovedenhetNavn = NavEnhetFixtures.Oslo.navn,
        mobilnummer = ansatt.mobilnummer,
        epost = ansatt.epost,
    )

    val ansatt1 = toEntraNavAnsatt(NavAnsattFixture.DonaldDuck)
    val ansatt2 = toEntraNavAnsatt(NavAnsattFixture.MikkeMus)

    val adGruppeGenerell = UUID.randomUUID()
    val rolleGenerell = NavAnsattRolle.generell(TILTAKADMINISTRASJON_GENERELL)
    val rolleMappingGenerell = EntraGroupNavAnsattRolleMapping(
        entraGroupId = adGruppeGenerell,
        rolle = rolleGenerell.rolle,
    )

    val adGruppeKontaktperson = UUID.randomUUID()
    val rolleKontaktperson = NavAnsattRolle.generell(KONTAKTPERSON)
    val rolleMappingKontaktperson = EntraGroupNavAnsattRolleMapping(
        entraGroupId = adGruppeKontaktperson,
        rolle = rolleKontaktperson.rolle,
    )

    val msGraph = mockk<MsGraphClient>()

    fun createNavAnsattService(
        roles: Set<EntraGroupNavAnsattRolleMapping>,
    ) = NavAnsattService(
        roles = roles,
        db = db,
        microsoftGraphClient = msGraph,
    )

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt by entraObjectId with roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val oid = ansatt1.entraObjectId

            coEvery { msGraph.getNavAnsatt(oid, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(adGruppeKontaktperson)

            service.getNavAnsattFromAzure(oid, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(setOf())
        }

        test("should get NavAnsatt by navIdent with roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val navIdent = ansatt1.navIdent
            val oid = ansatt1.entraObjectId

            coEvery { msGraph.getNavAnsattByNavIdent(navIdent, AccessType.M2M) } returns ansatt1
            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(adGruppeGenerell)

            service.getNavAnsattFromAzure(navIdent, AccessType.M2M) shouldBe ansatt1.toNavAnsatt(
                setOf(rolleGenerell),
            )
        }
    }

    context("getNavAnsattRoles") {
        test("should get NavAnsatt roles filtered by the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingGenerell))

            val oid = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(adGruppeGenerell, UUID.randomUUID())

            service.getNavAnsattRoles(oid, AccessType.M2M) shouldBe setOf(rolleGenerell)
        }

        test("should return empty set when the NavAnsatt does not have any of the configured roles") {
            val service = createNavAnsattService(setOf(rolleMappingKontaktperson))

            val oid = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(adGruppeGenerell)

            service.getNavAnsattRoles(oid, AccessType.M2M) shouldBe setOf()
        }

        test("should resolve Nav-enhet from the mapping") {
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = EntraGroupNavAnsattRolleMapping(
                entraGroupId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                kostnadssteder = setOf(NavEnhetNummer("0387")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterOslo))

            val oid = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(adGruppeBeslutterOslo)

            service.getNavAnsattRoles(oid, AccessType.M2M) shouldBe setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, enheter = setOf(NavEnhetNummer("0387"))),
            )
        }

        test("should resolve Nav-enhet with kostnadssteder Nav-enheter from multiple groups") {
            fun kostnadssted(enhetsnummer: String, region: String) = Kostnadssted(
                enhetsnummer = NavEnhetNummer(enhetsnummer),
                navn = enhetsnummer,
                region = Kostnadssted.Region(NavEnhetNummer(region), region),
            )

            every { db.queries.kostnadssted.getAll(listOf(NavEnhetNummer("0400"))) } returns listOf(
                kostnadssted("0501", "0400"),
                kostnadssted("0502", "0400"),
            )
            every { db.queries.kostnadssted.getAll(listOf(NavEnhetNummer("0300"))) } returns listOf(
                kostnadssted("0387", "0300"),
            )

            val adGruppeBeslutterInnlandet = UUID.randomUUID()
            val rolleBeslutterInnlandet = EntraGroupNavAnsattRolleMapping(
                entraGroupId = adGruppeBeslutterInnlandet,
                rolle = Rolle.BESLUTTER_TILSAGN,
                kostnadssteder = setOf(NavEnhetNummer("0400")),
            )
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = EntraGroupNavAnsattRolleMapping(
                entraGroupId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                kostnadssteder = setOf(NavEnhetNummer("0300")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterInnlandet, rolleBeslutterOslo))

            val oid = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(
                adGruppeBeslutterInnlandet,
                adGruppeBeslutterOslo,
            )

            service.getNavAnsattRoles(oid, AccessType.M2M) shouldBe setOf(
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
            val rolleBeslutterGenerell = EntraGroupNavAnsattRolleMapping(
                entraGroupId = adGruppeBeslutterGenerell,
                rolle = Rolle.BESLUTTER_TILSAGN,
            )
            val adGruppeBeslutterOslo = UUID.randomUUID()
            val rolleBeslutterOslo = EntraGroupNavAnsattRolleMapping(
                entraGroupId = adGruppeBeslutterOslo,
                rolle = Rolle.BESLUTTER_TILSAGN,
                kostnadssteder = setOf(NavEnhetNummer("0387")),
            )

            val service = createNavAnsattService(setOf(rolleBeslutterGenerell, rolleBeslutterOslo))

            val oid = UUID.randomUUID()

            coEvery { msGraph.getMemberGroups(oid, any()) } returns listOf(
                adGruppeBeslutterGenerell,
                adGruppeBeslutterOslo,
            )

            service.getNavAnsattRoles(oid, AccessType.M2M) shouldBe setOf(
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
                EntraGroupNavAnsattRolleMapping(supertilgang, TILTAKADMINISTRASJON_GENERELL),
                EntraGroupNavAnsattRolleMapping(supertilgang, KONTAKTPERSON),
            )

            coEvery { msGraph.getMemberGroups(ansatt1.entraObjectId, any()) } returns listOf(supertilgang)

            val service = createNavAnsattService(roles)

            service.getNavAnsattRoles(ansatt1.entraObjectId, AccessType.M2M) shouldBe setOf(
                rolleGenerell,
                rolleKontaktperson,
            )
        }
    }

    context("getNavAnsatteFromAzure") {
        coEvery { msGraph.getGroupMembers(rolleMappingGenerell.entraGroupId) } returns listOf(ansatt1, ansatt2)
        coEvery { msGraph.getGroupMembers(rolleMappingKontaktperson.entraGroupId) } returns listOf(ansatt2)

        coEvery { msGraph.getMemberGroups(ansatt1.entraObjectId, any()) } returns listOf(adGruppeGenerell)
        coEvery { msGraph.getMemberGroups(ansatt2.entraObjectId, any()) } returns listOf(
            adGruppeGenerell,
            adGruppeKontaktperson,
        )

        test("should resolve all roles from the specified groups").config(blockingTest = true, timeout = 10.seconds) {
            val cases = listOf(
                setOf(rolleMappingGenerell) to listOf(
                    ansatt1.toNavAnsatt(setOf(rolleGenerell)),
                    ansatt2.toNavAnsatt(setOf(rolleGenerell)),
                ),
                setOf(rolleMappingKontaktperson) to listOf(
                    ansatt2.toNavAnsatt(setOf(rolleKontaktperson)),
                ),
                setOf(rolleMappingGenerell, rolleMappingKontaktperson) to listOf(
                    ansatt1.toNavAnsatt(setOf(rolleGenerell)),
                    ansatt2.toNavAnsatt(setOf(rolleGenerell, rolleKontaktperson)),
                ),
            )

            cases.forEach { (groups, expectedAnsatte) ->
                val service = createNavAnsattService(groups)

                val resolvedAnsatte = service.getNavAnsatteForRoles(groups)

                resolvedAnsatte shouldContainExactlyInAnyOrder expectedAnsatte
            }
        }
    }
})
