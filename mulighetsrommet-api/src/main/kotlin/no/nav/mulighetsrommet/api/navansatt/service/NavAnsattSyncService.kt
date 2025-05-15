package no.nav.mulighetsrommet.api.navansatt.service

import arrow.core.toNonEmptyListOrNull
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.EnhetFilter
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.sanity.*
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationTask
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

class NavAnsattSyncService(
    private val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
    private val sanityService: SanityService,
    private val navEnhetService: NavEnhetService,
    private val notificationTask: NotificationTask,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val ansattGroupsToSync: Set<UUID>,
    )

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate): Unit = db.session {
        val ansatteToUpsert = navAnsattService.getNavAnsatteInGroups(config.ansattGroupsToSync)

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            val currentAnsattDbo = queries.ansatt.getNavAnsattDbo(ansatt.navIdent)
            NavAnsattDbo.fromNavAnsatt(ansatt).takeIf { it != currentAnsattDbo }?.also {
                queries.ansatt.upsert(it)
            }
            queries.ansatt.setRoller(ansatt.navIdent, ansatt.roller)
        }
        upsertSanityAnsatte(ansatteToUpsert)

        val ansatteEntraObjectIds = ansatteToUpsert.map { it.entraObjectId }
        val ansatteToScheduleForDeletion = queries.ansatt.getAll().filter { ansatt ->
            ansatt.entraObjectId !in ansatteEntraObjectIds && ansatt.skalSlettesDato == null
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting oid=${ansatt.entraObjectId} dato=$deletionDate")
            val ansattToDelete = NavAnsattDbo.fromNavAnsatt(ansatt).copy(skalSlettesDato = deletionDate)
            queries.ansatt.upsert(ansattToDelete)
            queries.ansatt.setRoller(ansattToDelete.navIdent, setOf())
        }

        val ansatteToDelete = queries.ansatt.getAll(skalSlettesDatoLte = today)
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting oid=${ansatt.entraObjectId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private suspend fun deleteNavAnsatt(ansatt: NavAnsatt): Unit = db.transaction {
        val avtaleIds = queries.avtale.getAvtaleIdsByAdministrator(ansatt.navIdent)
        val gjennomforinger = sanityService.getTiltakByNavIdent(ansatt.navIdent)

        queries.ansatt.deleteByEntraObjectId(ansatt.entraObjectId)
        sanityService.removeNavIdentFromTiltaksgjennomforinger(ansatt.navIdent)
        sanityService.deleteNavIdent(ansatt.navIdent)

        gjennomforinger.forEach { gjennomforing ->
            notifyRelevantAdministratorsForSanityGjennomforing(
                gjennomforing,
                ansatt.hovedenhet,
            )
        }

        avtaleIds.forEach {
            val avtale = requireNotNull(queries.avtale.get(it))
            if (avtale.administratorer.isEmpty()) {
                notifyRelevantAdministrators(avtale, ansatt.hovedenhet)
            }
        }
    }

    private fun QueryContext.notifyRelevantAdministrators(
        avtale: AvtaleDto,
        hovedenhet: NavAnsatt.Hovedenhet,
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

        val administrators = queries.ansatt
            .getAll(
                rollerContainsAll = listOf(NavAnsattRolle.generell(Rolle.AVTALER_SKRIV)),
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
        notificationTask.scheduleNotification(notification)
    }

    private fun QueryContext.notifyRelevantAdministratorsForSanityGjennomforing(
        tiltak: SanityTiltaksgjennomforing,
        hovedenhet: NavAnsatt.Hovedenhet,
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

        val administrators = queries.ansatt
            .getAll(
                rollerContainsAll = listOf(NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)),
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
        notificationTask.scheduleNotification(notification)
    }

    private suspend fun upsertSanityAnsatte(ansatte: List<NavAnsatt>) {
        val existingNavKontaktpersonIds = sanityService.getNavKontaktpersoner()
            .associate { it.navIdent.current to it._id }
        val existingRedaktorIds = sanityService.getRedaktorer()
            .associate { it.navIdent.current to it._id }

        logger.info("Upserter ${ansatte.size} ansatte til Sanity")
        val navKontaktpersoner = mutableListOf<SanityNavKontaktperson>()
        val redaktorer = mutableListOf<SanityRedaktor>()
        ansatte.forEach { ansatt ->
            if (ansatt.hasGenerellRolle(Rolle.KONTAKTPERSON)) {
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
                        enhetsnummer = ansatt.hovedenhet.enhetsnummer,
                    ),
                )
            }

            if (ansatt.hasAnyGenerellRolle(Rolle.AVTALER_SKRIV, Rolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
                val id = existingRedaktorIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                redaktorer.add(
                    SanityRedaktor(
                        _id = id.toString(),
                        _type = "redaktor",
                        enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        epost = Slug(current = ansatt.epost),
                        enhetsnummer = ansatt.hovedenhet.enhetsnummer,
                    ),
                )
            }
        }
        sanityService.createNavKontaktpersoner(navKontaktpersoner)
        sanityService.createRedaktorer(redaktorer)
    }
}
