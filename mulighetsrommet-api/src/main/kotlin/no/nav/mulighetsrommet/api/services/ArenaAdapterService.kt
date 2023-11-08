package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.Tiltakskoder.isEgenRegiTiltak
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.util.*

class ArenaAdapterService(
    private val db: Database,
    private val navAnsatte: NavAnsattRepository,
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltakshistorikk: TiltakshistorikkRepository,
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val tiltakstypeKafkaProducer: TiltakstypeKafkaProducer,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val navEnhetService: NavEnhetService,
    private val notificationService: NotificationService,
) {
    fun upsertTiltakstype(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> {
        return tiltakstyper.upsert(tiltakstype).onRight {
            tiltakstyper.get(tiltakstype.id)?.let {
                tiltakstypeKafkaProducer.publish(it)
            }
        }
    }

    fun removeTiltakstype(id: UUID): QueryResult<Int> {
        return tiltakstyper.delete(id).onRight { deletedRows ->
            if (deletedRows != 0) {
                tiltakstypeKafkaProducer.retract(id)
            }
        }
    }

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleAdminDto {
        virksomhetService.getOrSyncVirksomhet(avtale.leverandorOrganisasjonsnummer)
        avtaler.upsertArenaAvtale(avtale)

        val dbo = avtaler.get(avtale.id)!!
        if (dbo.isAktiv() && dbo.administratorer.isEmpty()) {
            maybeNotifyRelevantAdministrators(dbo)
        }
        return dbo
    }

    fun removeAvtale(id: UUID) {
        avtaler.delete(id)
    }

    suspend fun upsertTiltaksgjennomforing(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        val mulighetsrommetAvtaleId = lookForExistingAvtale(tiltaksgjennomforing)
        virksomhetService.getOrSyncVirksomhet(tiltaksgjennomforing.arrangorOrganisasjonsnummer)
        val tiltaksgjennomforingMedAvtale = tiltaksgjennomforing.copy(avtaleId = mulighetsrommetAvtaleId)

        val gjennomforing = db.transactionSuspend { tx ->
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingMedAvtale, tx)
            val gjennomforing = tiltaksgjennomforinger.get(tiltaksgjennomforing.id, tx)!!
            tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(gjennomforing))

            if (shouldBeManagedInSanity(gjennomforing)) {
                sanityTiltaksgjennomforingService.createOrPatchSanityTiltaksgjennomforing(gjennomforing, tx)
            } else if (gjennomforing.isAktiv() && gjennomforing.administratorer.isEmpty()) {
                maybeNotifyRelevantAdministrators(gjennomforing)
            }

            gjennomforing
        }

        return query { gjennomforing }
    }

    suspend fun removeTiltaksgjennomforing(id: UUID) {
        val gjennomforing = tiltaksgjennomforinger.get(id)
            ?: return
        val sanityId = gjennomforing.sanityId

        db.transaction { tx ->
            tiltaksgjennomforinger.delete(id, tx)
            tiltaksgjennomforingKafkaProducer.retract(id)
        }

        if (sanityId != null) {
            sanityTiltaksgjennomforingService.deleteSanityTiltaksgjennomforing(sanityId)
        }
    }

    fun upsertTiltakshistorikk(tiltakshistorikk: ArenaTiltakshistorikkDbo): QueryResult<ArenaTiltakshistorikkDbo> {
        return this.tiltakshistorikk.upsert(tiltakshistorikk)
    }

    fun removeTiltakshistorikk(id: UUID): QueryResult<Unit> {
        return tiltakshistorikk.delete(id)
    }

    fun upsertDeltaker(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> {
        return query { deltakere.upsert(deltaker) }
    }

    fun removeDeltaker(id: UUID): QueryResult<Unit> {
        return query { deltakere.delete(id) }
    }

    private fun lookForExistingAvtale(tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo): UUID? {
        val tiltakstype = tiltakstyper.get(tiltaksgjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${tiltaksgjennomforing.tiltakstypeId}")

        return if (Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode)) {
            tiltaksgjennomforinger.get(tiltaksgjennomforing.id)?.avtaleId ?: tiltaksgjennomforing.avtaleId
        } else {
            tiltaksgjennomforing.avtaleId
        }
    }

    private fun shouldBeManagedInSanity(gjennomforing: TiltaksgjennomforingAdminDto): Boolean {
        val sluttDato = gjennomforing.sluttDato
        return isEgenRegiTiltak(gjennomforing.tiltakstype.arenaKode) &&
            (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate))
    }

    private fun maybeNotifyRelevantAdministrators(avtale: AvtaleAdminDto) {
        val enhet = resolveRelevantNavEnhet(avtale.arenaAnsvarligEnhet) ?: return
        notifyRelevantAdministrators(enhet) { administrators ->
            ScheduledNotification(
                type = NotificationType.TASK,
                title = """Avtalen "${avtale.navn}" har endringer fra Arena, men mangler en ansvarlig administrator.""",
                description = "Du har blitt varslet fordi din NAV-hovedenhet og avtalens ansvarlige NAV-enhet begge er relatert til ${enhet.navn}. Gå til avtalen og sett deg som administrator hvis du eier avtalen.",
                metadata = NotificationMetadata(
                    linkText = "Gå til avtalen",
                    link = "/avtaler/${avtale.id}",
                ),
                targets = administrators,
                createdAt = Instant.now(),
            )
        }
    }

    private fun maybeNotifyRelevantAdministrators(gjennomforing: TiltaksgjennomforingAdminDto) {
        val enhet = resolveRelevantNavEnhet(gjennomforing.arenaAnsvarligEnhet) ?: return
        notifyRelevantAdministrators(enhet) { administrators ->
            ScheduledNotification(
                type = NotificationType.TASK,
                title = """Gjennomføringen "${gjennomforing.navn}" har endringer fra Arena, men mangler en ansvarlig administrator.""",
                description = "Du har blitt varslet fordi din NAV-hovedenhet og gjennomføringens ansvarlige NAV-enhet begge er relatert til ${enhet.navn}. Gå til gjennomføringen og sett deg som administrator hvis du eier gjennomføringen.",
                metadata = NotificationMetadata(
                    linkText = "Gå til gjennomføringen",
                    link = "/tiltaksgjennomforinger/${gjennomforing.id}",
                ),
                targets = administrators,
                createdAt = Instant.now(),
            )
        }
    }

    private fun resolveRelevantNavEnhet(arenaAnsvarligEnhet: String?): NavEnhetDbo? {
        if (arenaAnsvarligEnhet == null) {
            return null
        }

        return navEnhetService.hentOverordnetFylkesenhet(arenaAnsvarligEnhet)
    }

    private fun notifyRelevantAdministrators(
        overordnetEnhet: NavEnhetDbo,
        createNotification: (administrators: List<String>) -> ScheduledNotification,
    ) {
        val potentialAdministratorHovedenheter = navEnhetService
            .hentAlleEnheter(
                EnhetFilter(
                    statuser = listOf(NavEnhetStatus.AKTIV),
                    overordnetEnhet = overordnetEnhet.enhetsnummer,
                ),
            )
            .map { it.enhetsnummer }
            .plus(overordnetEnhet.enhetsnummer)

        val administrators = navAnsatte
            .getAll(
                roller = listOf(NavAnsattRolle.BETABRUKER),
                hovedenhetIn = potentialAdministratorHovedenheter,
            )
            .getOrThrow()
            .map { it.navIdent }

        val notification = createNotification(administrators)
        notificationService.scheduleNotification(notification)
    }
}
