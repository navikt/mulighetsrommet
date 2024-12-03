package no.nav.mulighetsrommet.api.navansatt

import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.dto.Slug
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRepository
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.api.navenhet.EnhetFilter
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

class NavAnsattSyncService(
    private val navAnsattService: NavAnsattService,
    private val db: Database,
    private val navAnsattRepository: NavAnsattRepository,
    private val sanityService: SanityService,
    private val avtaleRepository: AvtaleRepository,
    private val navEnhetService: NavEnhetService,
    private val notificationRepository: NotificationRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate) {
        val ansatteToUpsert = navAnsattService.getNavAnsatteFromAzure()

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))
        }
        upsertSanityAnsatte(ansatteToUpsert)

        val ansatteAzureIds = ansatteToUpsert.map { it.azureId }
        val ansatteToScheduleForDeletion = navAnsattRepository.getAll().filter { ansatt ->
            ansatt.azureId !in ansatteAzureIds && ansatt.skalSlettesDato == null
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting azureId=${ansatt.azureId} dato=$deletionDate")
            val ansattToDelete = ansatt.copy(roller = emptySet(), skalSlettesDato = deletionDate)
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansattToDelete))
        }

        val ansatteToDelete = navAnsattRepository.getAll(skalSlettesDatoLte = today)
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting azureId=${ansatt.azureId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private suspend fun deleteNavAnsatt(ansatt: NavAnsattDto) {
        val avtaleIds = avtaleRepository.getAvtaleIdsByAdministrator(ansatt.navIdent)
        val gjennomforinger = sanityService.getTiltakByNavIdent(ansatt.navIdent)

        db.transactionSuspend { tx ->
            navAnsattRepository.deleteByAzureId(ansatt.azureId, tx)
            sanityService.removeNavIdentFromTiltaksgjennomforinger(ansatt.navIdent)
            sanityService.deleteNavIdent(ansatt.navIdent)
        }

        gjennomforinger
            .forEach { gjennomforing ->
                notifyRelevantAdministratorsForSanityGjennomforing(
                    gjennomforing,
                    ansatt.hovedenhet,
                )
            }

        avtaleIds
            .forEach {
                val avtale = requireNotNull(avtaleRepository.get(it))
                if (avtale.administratorer.isEmpty()) {
                    notifyRelevantAdministrators(avtale, ansatt.hovedenhet)
                }
            }
    }

    private fun notifyRelevantAdministrators(
        avtale: AvtaleDto,
        hovedenhet: NavAnsattDto.Hovedenhet,
    ) {
        val region = navEnhetService.hentOverordnetFylkesenhet(hovedenhet.enhetsnummer)
            ?: return

        val potentialAdministratorHovedenheter = navEnhetService.hentAlleEnheter(
            EnhetFilter(
                statuser = listOf(NavEnhetStatus.AKTIV),
                overordnetEnhet = region.enhetsnummer,
            ),
        )
            .map { it.enhetsnummer }
            .plus(region.enhetsnummer)

        val administrators = navAnsattRepository
            .getAll(
                roller = listOf(NavAnsattRolle.AVTALER_SKRIV),
                hovedenhetIn = potentialAdministratorHovedenheter,
            )
            .map { it.navIdent }
            .toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            type = NotificationType.TASK,
            title = """Avtalen "${avtale.navn}" mangler administrator.""",
            description = "Du har blitt varslet fordi din Nav-hovedenhet er i samme fylke som forrige administrators Nav-hovedenhet. Gå til avtalen og sett deg som administrator hvis du eier avtalen.",
            metadata = NotificationMetadata(
                linkText = "Gå til avtalen",
                link = "/avtaler/${avtale.id}",
            ),
            targets = administrators,
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification)
    }

    private fun notifyRelevantAdministratorsForSanityGjennomforing(
        tiltak: SanityTiltaksgjennomforing,
        hovedenhet: NavAnsattDto.Hovedenhet,
    ) {
        val region = navEnhetService.hentOverordnetFylkesenhet(hovedenhet.enhetsnummer)
            ?: return

        val potentialAdministratorHovedenheter = navEnhetService.hentAlleEnheter(
            EnhetFilter(
                statuser = listOf(NavEnhetStatus.AKTIV),
                overordnetEnhet = region.enhetsnummer,
            ),
        )
            .map { it.enhetsnummer }
            .plus(region.enhetsnummer)

        val administrators = navAnsattRepository
            .getAll(
                roller = listOf(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV),
                hovedenhetIn = potentialAdministratorHovedenheter,
            )
            .map { it.navIdent }
            .toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            type = NotificationType.TASK,
            title = """Kontaktperson eller redaktør for tiltak: "${tiltak.tiltaksgjennomforingNavn}" ble fjernet i Sanity""",
            description = "Du har blitt varslet fordi din Nav-hovedenhet er i samme fylke som den slettede kontaktpersons/redaktørs Nav-hovedenhet. Gå til tiltaksgjennomføringen i Sanity og sjekk at kontaktpersonene og redaktørene for tiltaket er korrekt.",
            metadata = NotificationMetadata(
                linkText = "Gå til gjennomføringen i Sanity",
                link = "https://mulighetsrommet-sanity-studio.intern.nav.no/prod/structure/tiltaksgjennomforinger;alleTiltaksgjennomforinger;${tiltak._id}",
            ),
            targets = administrators,
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification)
    }

    private suspend fun upsertSanityAnsatte(ansatte: List<NavAnsattDto>) {
        val existingNavKontaktpersonIds = sanityService.getNavKontaktpersoner()
            .associate { it.navIdent.current to it._id }
        val existingRedaktorIds = sanityService.getRedaktorer()
            .associate { it.navIdent.current to it._id }

        logger.info("Upserter ${ansatte.size} ansatte til Sanity")
        val navKontaktpersoner = mutableListOf<SanityNavKontaktperson>()
        val redaktorer = mutableListOf<SanityRedaktor>()
        ansatte.forEach { ansatt ->
            if (ansatt.roller.contains(NavAnsattRolle.KONTAKTPERSON)) {
                val id = existingNavKontaktpersonIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                navKontaktpersoner.add(
                    SanityNavKontaktperson(
                        _id = id.toString(),
                        _type = "navKontaktperson",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
                        telefonnummer = ansatt.mobilnummer,
                        epost = ansatt.epost,
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                    ),
                )
            }

            if (ansatt.roller.contains(NavAnsattRolle.AVTALER_SKRIV) || ansatt.roller.contains(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
                val id = existingRedaktorIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                redaktorer.add(
                    SanityRedaktor(
                        _id = id.toString(),
                        _type = "redaktor",
                        enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        epost = Slug(current = ansatt.epost),
                    ),
                )
            }
        }
        sanityService.createNavKontaktpersoner(navKontaktpersoner)
        sanityService.createRedaktorer(redaktorer)
    }
}

@Serializable
data class SanityNavKontaktperson(
    val _id: String,
    val _type: String,
    val navIdent: Slug,
    val enhet: String,
    val telefonnummer: String? = null,
    val epost: String,
    val navn: String,
)

@Serializable
data class SanityRedaktor(
    val _id: String,
    val _type: String,
    val navIdent: Slug,
    val enhet: String,
    val epost: Slug,
    val navn: String,
)
