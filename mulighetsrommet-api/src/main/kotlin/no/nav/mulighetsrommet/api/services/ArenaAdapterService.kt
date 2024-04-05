package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
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
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.Tiltakskoder.isEgenRegiTiltak
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
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
    private val arrangorService: ArrangorService,
    private val navEnhetService: NavEnhetService,
    private val notificationService: NotificationService,
    private val endringshistorikk: EndringshistorikkService,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsertTiltakstype(tiltakstype: TiltakstypeDbo): TiltakstypeDbo {
        return tiltakstyper.upsert(tiltakstype)
            .also {
                val eksternDto = tiltakstyper.getEksternTiltakstype(tiltakstype.id)
                if (eksternDto != null) {
                    tiltakstypeKafkaProducer.publish(eksternDto)
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
        syncArrangorFromBrreg(avtale.arrangorOrganisasjonsnummer)

        val dto = db.transaction { tx ->
            val previous = avtaler.get(avtale.id)
            if (previous?.toArenaAvtaleDbo() == avtale) {
                return@transaction previous
            }

            avtaler.upsertArenaAvtale(tx, avtale)

            val next = avtaler.get(avtale.id, tx)!!

            logUpdateAvtale(tx, next)

            next
        }

        if (dto.avtalestatus == Avtalestatus.AKTIV && dto.administratorer.isEmpty()) {
            maybeNotifyRelevantAdministrators(dto)
        }

        return dto
    }

    fun removeAvtale(id: UUID) {
        db.transaction { tx ->
            avtaler.delete(tx, id)
            logDelete(tx, DocumentClass.AVTALE, id)
        }
    }

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaTiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingAdminDto> {
        syncArrangorFromBrreg(arenaGjennomforing.arrangorOrganisasjonsnummer)

        val previous = tiltaksgjennomforinger.get(arenaGjennomforing.id)

        val mergedArenaGjennomforing = if (previous != null) {
            mergeWithCurrentGjennomforing(arenaGjennomforing, previous)
        } else {
            arenaGjennomforing
        }

        if (previous != null && hasNoRelevantChanges(mergedArenaGjennomforing, previous)) {
            return query { previous }
        }

        val gjennomforing = db.transactionSuspend { tx ->
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(mergedArenaGjennomforing, tx)

            val next = tiltaksgjennomforinger.get(mergedArenaGjennomforing.id, tx)!!

            if (previous?.tiltaksnummer == null) {
                logTiltaksnummerHentetFraArena(tx, next)
            } else {
                logUpdateGjennomforing(tx, next)
            }

            next.avtaleId?.let { avtaleId ->
                avtaler.setArrangorUnderenhet(tx, avtaleId, next.arrangor.id)
            }

            if (shouldBeManagedInSanity(next)) {
                sanityTiltaksgjennomforingService.createOrPatchSanityTiltaksgjennomforing(next, tx)
            } else if (next.isAktiv() && next.administratorer.isEmpty()) {
                maybeNotifyRelevantAdministrators(next)
            }

            tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(next))

            next
        }

        return query { gjennomforing }
    }

    private suspend fun syncArrangorFromBrreg(orgnr: String) {
        arrangorService.getOrSyncArrangorFromBrreg(orgnr).onLeft { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet mer orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }

            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
    }

    suspend fun removeTiltaksgjennomforing(id: UUID) {
        val gjennomforing = tiltaksgjennomforinger.get(id)
            ?: return
        val sanityId = gjennomforing.sanityId

        db.transaction { tx ->
            tiltaksgjennomforinger.delete(id, tx)
            logDelete(tx, DocumentClass.TILTAKSGJENNOMFORING, id)
            tiltaksgjennomforingKafkaProducer.retract(id)
        }

        if (sanityId != null) {
            sanityTiltaksgjennomforingService.deleteSanityTiltaksgjennomforing(sanityId)
        }
    }

    suspend fun upsertTiltakshistorikk(tiltakshistorikk: ArenaTiltakshistorikkDbo): Either<ErUnderOppfolgingError, Boolean> {
        return veilarboppfolgingClient.erBrukerUnderOppfolging(tiltakshistorikk.norskIdent, AccessType.M2M)
            .onRight {
                if (it) {
                    this.tiltakshistorikk.upsert(tiltakshistorikk)
                }
            }
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

    private fun mergeWithCurrentGjennomforing(
        tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingAdminDto,
    ): ArenaTiltaksgjennomforingDbo {
        val tiltakstype = tiltakstyper.get(tiltaksgjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${tiltaksgjennomforing.tiltakstypeId}")

        return if (tiltakstypeService.isEnabled(Tiltakskode.fromArenaKode(tiltakstype.arenaKode))) {
            ArenaTiltaksgjennomforingDbo(
                // Behold felter som settes i Arena
                tiltaksnummer = tiltaksgjennomforing.tiltaksnummer,
                arenaAnsvarligEnhet = tiltaksgjennomforing.arenaAnsvarligEnhet,

                // Resten av feltene skal ikke overskrives med data fra Arena
                id = current.id,
                avtaleId = current.avtaleId,
                navn = current.navn,
                tiltakstypeId = current.tiltakstype.id,
                arrangorOrganisasjonsnummer = current.arrangor.organisasjonsnummer,
                startDato = current.startDato,
                sluttDato = current.sluttDato,
                apentForInnsok = current.apentForInnsok,
                antallPlasser = current.antallPlasser,
                oppstart = current.oppstart,
                deltidsprosent = current.deltidsprosent,
                avslutningsstatus = current.status.toAvslutningsstatus(),
            )
        } else {
            // Pass på at man ikke mister referansen til Avtalen
            val avtaleId = if (
                current.opphav == ArenaMigrering.Opphav.MR_ADMIN_FLATE ||
                Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode)
            ) {
                current.avtaleId ?: tiltaksgjennomforing.avtaleId
            } else {
                tiltaksgjennomforing.avtaleId
            }
            tiltaksgjennomforing.copy(avtaleId = avtaleId)
        }
    }

    private fun hasNoRelevantChanges(
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingAdminDto,
    ): Boolean {
        val currentAsArenaGjennomforing = ArenaTiltaksgjennomforingDbo(
            id = current.id,
            navn = current.navn,
            tiltakstypeId = current.tiltakstype.id,
            tiltaksnummer = current.tiltaksnummer ?: "",
            arrangorOrganisasjonsnummer = current.arrangor.organisasjonsnummer,
            startDato = current.startDato,
            sluttDato = current.sluttDato,
            arenaAnsvarligEnhet = current.arenaAnsvarligEnhet?.enhetsnummer,
            avslutningsstatus = current.status.toAvslutningsstatus(),
            apentForInnsok = current.apentForInnsok,
            antallPlasser = current.antallPlasser,
            avtaleId = current.avtaleId,
            oppstart = current.oppstart,
            deltidsprosent = current.deltidsprosent,
        )
        return currentAsArenaGjennomforing == arenaGjennomforing
    }

    private fun shouldBeManagedInSanity(gjennomforing: TiltaksgjennomforingAdminDto): Boolean {
        val sluttDato = gjennomforing.sluttDato
        return isEgenRegiTiltak(gjennomforing.tiltakstype.arenaKode) &&
            (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate))
    }

    private fun maybeNotifyRelevantAdministrators(avtale: AvtaleAdminDto) {
        val enhet = resolveRelevantNavEnhet(avtale.arenaAnsvarligEnhet?.enhetsnummer) ?: return
        notifyRelevantAdministrators(enhet, NavAnsattRolle.AVTALER_SKRIV) { administrators ->
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
        val enhet = resolveRelevantNavEnhet(gjennomforing.arenaAnsvarligEnhet?.enhetsnummer) ?: return
        notifyRelevantAdministrators(enhet, NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV) { administrators ->
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
        navAnsattRolle: NavAnsattRolle,
        createNotification: (administrators: NonEmptyList<NavIdent>) -> ScheduledNotification,
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
                roller = listOf(navAnsattRolle),
                hovedenhetIn = potentialAdministratorHovedenheter,
            )
            .map { it.navIdent }
            .toNonEmptyListOrNull() ?: return

        val notification = createNotification(administrators)
        notificationService.scheduleNotification(notification)
    }

    private fun logUpdateAvtale(tx: TransactionalSession, dto: AvtaleAdminDto) {
        endringshistorikk.logEndring(
            tx,
            DocumentClass.AVTALE,
            "Endret i Arena",
            "Arena",
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun logUpdateGjennomforing(tx: TransactionalSession, dto: TiltaksgjennomforingAdminDto) {
        endringshistorikk.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            "Endret i Arena",
            "Arena",
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun logTiltaksnummerHentetFraArena(tx: TransactionalSession, dto: TiltaksgjennomforingAdminDto) {
        endringshistorikk.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            "Oppdatert med tiltaksnummer fra Arena",
            TILTAKSADMINISTRASJON_SYSTEM_BRUKER,
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun logDelete(tx: TransactionalSession, documentClass: DocumentClass, id: UUID) {
        endringshistorikk.logEndring(
            tx,
            documentClass,
            "Slettet i Arena",
            "Arena",
            id,
        ) { JsonNull }
    }
}
