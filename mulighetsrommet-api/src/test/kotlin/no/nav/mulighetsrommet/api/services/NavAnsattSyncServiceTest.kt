package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.*
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.AvtaleDto
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.LocalDate
import java.util.*

class NavAnsattSyncServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
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

    val avtaleRepository: AvtaleRepository = mockk()
    val navEnhetService: NavEnhetService = mockk()
    val notificationService: NotificationService = mockk()
    val sanityService: SanityService = mockk(relaxed = true)
    val navAnsattService: NavAnsattService = mockk()

    context("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
        val ansatte = NavAnsattRepository(database.db)
        val today = LocalDate.now()
        val deletionDate = today.plusDays(1)

        val service = NavAnsattSyncService(
            navAnsattService = navAnsattService,
            db = database.db,
            navAnsattRepository = ansatte,
            sanityService = sanityService,
            avtaleRepository = avtaleRepository,
            navEnhetService = navEnhetService,
            notificationService = notificationService,
        )

        test("begge finnes i azure => ingen skal slettes") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
            )
            service.synchronizeNavAnsatte(today, deletionDate)

            ansatte.getAll() shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(TILTAKADMINISTRASJON_GENERELL)),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(TILTAKADMINISTRASJON_GENERELL)),
            )
        }

        test("kontaktperson fra azure => den andre satt til sletting") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )
            service.synchronizeNavAnsatte(today, deletionDate)

            ansatte.getAll() shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                    skalSlettesDato = deletionDate,
                ),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )
        }

        test("ingen fra azure => begge satt til sletting") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns emptyList()
            service.synchronizeNavAnsatte(today, deletionDate)

            ansatte.getAll() shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                    skalSlettesDato = deletionDate,
                ),
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf()).copy(
                    skalSlettesDato = deletionDate,
                ),
            )
        }
    }

    context("should delete nav_ansatt when their deletion date matches the provided deletion date") {
        val ansatte = NavAnsattRepository(database.db)

        val service = NavAnsattSyncService(
            navAnsattService = navAnsattService,
            db = database.db,
            navAnsattRepository = ansatte,
            sanityService = sanityService,
            avtaleRepository = avtaleRepository,
            navEnhetService = navEnhetService,
            notificationService = notificationService,
        )
        every { avtaleRepository.getAvtaleIdsByAdministrator(any()) } returns emptyList()
        every { navEnhetService.hentOverordnetFylkesenhet(any()) } returns null

        val today = LocalDate.now()

        test("kontaktperson fra azure => da skal den ikke slettes") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )
            service.synchronizeNavAnsatte(today, deletionDate = today)

            ansatte.getAll() shouldContainExactlyInAnyOrder listOf(
                NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
            )
        }

        test("ingen fra azure => begge har blitt slettet") {
            coEvery { navAnsattService.getNavAnsatteFromAzure() } returns emptyList()
            service.synchronizeNavAnsatte(today, deletionDate = today)

            ansatte.getAll().shouldBeEmpty()
        }
    }

    test("varsler administratorer basert på hovedenhet når avtale ikke lengre har administrator") {
        val ansatte = NavAnsattRepository(database.db)
        every { avtaleRepository.getAvtaleIdsByAdministrator(ansatt1.navIdent) } returns listOf(AvtaleFixtures.AFT.id)
        every { avtaleRepository.get(AvtaleFixtures.AFT.id) } returns AvtaleDto(
            id = AvtaleFixtures.AFT.id,
            navn = AvtaleFixtures.AFT.navn,
            avtalenummer = AvtaleFixtures.AFT.avtalenummer,
            tiltakstype = TiltakstypeFixtures.AFT.run {
                AvtaleDto.Tiltakstype(
                    id = id,
                    navn = navn,
                    tiltakskode = tiltakskode!!,
                )
            },
            arrangor = AvtaleDto.ArrangorHovedenhet(
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
            websaknummer = AvtaleFixtures.AFT.websaknummer,
            beskrivelse = AvtaleFixtures.AFT.beskrivelse,
            faneinnhold = AvtaleFixtures.AFT.faneinnhold,
            personopplysninger = AvtaleFixtures.AFT.personopplysninger,
            personvernBekreftet = AvtaleFixtures.AFT.personvernBekreftet,
            arenaAnsvarligEnhet = null,
            opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            status = AvtaleStatus.AKTIV,
            administratorer = emptyList(),
            kontorstruktur = emptyList(),
            amoKategorisering = null,
            opsjonerRegistrert = emptyList(),
            utdanningslop = null,
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
        coEvery { navAnsattService.getNavAnsatteFromAzure() } returns listOf(
            NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(AVTALER_SKRIV)),
        )

        val today = LocalDate.now()
        val service = NavAnsattSyncService(
            navAnsattService = navAnsattService,
            db = database.db,
            navAnsattRepository = ansatte,
            sanityService = sanityService,
            avtaleRepository = avtaleRepository,
            navEnhetService = navEnhetService,
            notificationService = notificationService,
        )
        service.synchronizeNavAnsatte(today, deletionDate = today)

        verify(exactly = 1) {
            val expectedNotification: ScheduledNotification = match {
                it.type == NotificationType.TASK && it.targets.containsAll(listOf(ansatt2.navIdent))
            }
            notificationService.scheduleNotification(expectedNotification, any())
        }
    }
})
