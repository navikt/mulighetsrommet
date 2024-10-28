package no.nav.mulighetsrommet.api.services

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.AvtaleDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.kafka.producers.SisteTiltaksgjennomforingerV1KafkaProducer
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
    private val deltakere: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
    private val navEnhetService: NavEnhetService,
    private val notificationService: NotificationService,
    private val endringshistorikk: EndringshistorikkService,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleDto {
        syncArrangorFromBrreg(Organisasjonsnummer(avtale.arrangorOrganisasjonsnummer))

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

        if (dto.status == AvtaleStatus.AKTIV && dto.administratorer.isEmpty()) {
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

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaTiltaksgjennomforingDbo): UUID? {
        val tiltakstype = tiltakstyper.get(arenaGjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${arenaGjennomforing.tiltakstypeId}")

        syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        return if (Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
            upsertEgenRegiTiltak(tiltakstype, arenaGjennomforing)
        } else {
            upsertGruppetiltak(tiltakstype, arenaGjennomforing)
            null
        }
    }

    private suspend fun upsertEgenRegiTiltak(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
    ): UUID? {
        require(Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
            "Gjennomføring for tiltakstype ${tiltakstype.arenaKode} skal ikke skrives til Sanity"
        }

        val sluttDato = arenaGjennomforing.sluttDato
        return if (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate)) {
            sanityService.createOrPatchSanityTiltaksgjennomforing(arenaGjennomforing, tiltakstype.sanityId)
        } else {
            null
        }
    }

    private suspend fun upsertGruppetiltak(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
    ) {
        require(Tiltakskoder.isAmtTiltak(tiltakstype.arenaKode)) {
            "Gjennomføringer er ikke støttet for tiltakstype ${tiltakstype.arenaKode}"
        }

        val previous = tiltaksgjennomforinger.get(arenaGjennomforing.id)

        val mergedArenaGjennomforing = if (previous != null) {
            mergeWithCurrentGjennomforing(arenaGjennomforing, previous, tiltakstype)
        } else {
            arenaGjennomforing
        }

        if (previous != null && hasNoRelevantChanges(mergedArenaGjennomforing, previous)) {
            logger.info("Gjennomføring hadde ingen endringer")
            return
        }

        db.transactionSuspend { tx ->
            if (previous != null && tiltakstypeService.isEnabled(tiltakstype.tiltakskode)) {
                tiltaksgjennomforinger.updateArenaData(
                    previous.id,
                    mergedArenaGjennomforing.tiltaksnummer,
                    mergedArenaGjennomforing.arenaAnsvarligEnhet,
                    tx,
                )
            } else {
                tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(mergedArenaGjennomforing, tx)
            }

            val next = requireNotNull(tiltaksgjennomforinger.get(mergedArenaGjennomforing.id, tx)) {
                "Gjennomføring burde ikke være null siden den nettopp ble lagt til"
            }

            if (next.isAktiv() && next.administratorer.isEmpty()) {
                maybeNotifyRelevantAdministrators(next)
            }

            if (previous != null && previous.tiltaksnummer == null) {
                logTiltaksnummerHentetFraArena(tx, next)
            } else {
                logUpdateGjennomforing(tx, next)
            }

            next.avtaleId?.let { avtaleId ->
                avtaler.setArrangorUnderenhet(tx, avtaleId, next.arrangor.id)
            }

            tiltaksgjennomforingKafkaProducer.publish(next.toTiltaksgjennomforingV1Dto())
        }
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer) {
        arrangorService.getOrSyncArrangorFromBrreg(orgnr).onLeft { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet mer orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }

            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
    }

    suspend fun removeTiltaksgjennomforing(id: UUID) {
        db.transactionSuspend { tx ->
            val numDeleted = tiltaksgjennomforinger.delete(id, tx)
            if (numDeleted > 0) {
                logDelete(tx, DocumentClass.TILTAKSGJENNOMFORING, id)
                tiltaksgjennomforingKafkaProducer.retract(id)
            }
        }
    }

    suspend fun removeSanityTiltaksgjennomforing(sanityId: UUID) {
        sanityService.deleteSanityTiltaksgjennomforing(sanityId)
    }

    private fun mergeWithCurrentGjennomforing(
        tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingDto,
        tiltakstype: TiltakstypeDto,
    ): ArenaTiltaksgjennomforingDbo = if (tiltakstypeService.isEnabled(tiltakstype.tiltakskode)) {
        ArenaTiltaksgjennomforingDbo(
            // Behold felter som settes i Arena
            tiltaksnummer = tiltaksgjennomforing.tiltaksnummer,
            sanityId = tiltaksgjennomforing.sanityId,
            arenaAnsvarligEnhet = tiltaksgjennomforing.arenaAnsvarligEnhet,

            // Resten av feltene skal ikke overskrives med data fra Arena
            id = current.id,
            avtaleId = current.avtaleId ?: tiltaksgjennomforing.avtaleId,
            navn = current.navn,
            tiltakstypeId = current.tiltakstype.id,
            arrangorOrganisasjonsnummer = current.arrangor.organisasjonsnummer.value,
            startDato = current.startDato,
            sluttDato = current.sluttDato,
            apentForInnsok = current.apentForInnsok,
            antallPlasser = current.antallPlasser,
            deltidsprosent = current.deltidsprosent,
            avslutningsstatus = current.status.toAvslutningsstatus(),
        )
    } else {
        // Pass på at man ikke mister referansen til Avtalen
        val avtaleId = if (
            current.opphav == ArenaMigrering.Opphav.MR_ADMIN_FLATE ||
            Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.tiltakskode)
        ) {
            current.avtaleId ?: tiltaksgjennomforing.avtaleId
        } else {
            tiltaksgjennomforing.avtaleId
        }
        tiltaksgjennomforing.copy(avtaleId = avtaleId)
    }

    private fun hasNoRelevantChanges(
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingDto,
    ): Boolean {
        val currentAsArenaGjennomforing = ArenaTiltaksgjennomforingDbo(
            id = current.id,
            sanityId = null,
            navn = current.navn,
            tiltakstypeId = current.tiltakstype.id,
            tiltaksnummer = current.tiltaksnummer ?: "",
            arrangorOrganisasjonsnummer = current.arrangor.organisasjonsnummer.value,
            startDato = current.startDato,
            sluttDato = current.sluttDato,
            arenaAnsvarligEnhet = current.arenaAnsvarligEnhet?.enhetsnummer,
            avslutningsstatus = current.status.toAvslutningsstatus(),
            apentForInnsok = current.apentForInnsok,
            antallPlasser = current.antallPlasser,
            avtaleId = current.avtaleId,
            deltidsprosent = current.deltidsprosent,
        )
        return currentAsArenaGjennomforing == arenaGjennomforing
    }

    private fun maybeNotifyRelevantAdministrators(avtale: AvtaleDto) {
        val enhet = resolveRelevantNavEnhet(avtale.arenaAnsvarligEnhet?.enhetsnummer) ?: return
        notifyRelevantAdministrators(enhet, NavAnsattRolle.AVTALER_SKRIV) { administrators ->
            ScheduledNotification(
                type = NotificationType.TASK,
                title = """Avtalen "${avtale.navn}" har endringer fra Arena, men mangler administrator.""",
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

    private fun maybeNotifyRelevantAdministrators(gjennomforing: TiltaksgjennomforingDto) {
        val enhet = resolveRelevantNavEnhet(gjennomforing.arenaAnsvarligEnhet?.enhetsnummer) ?: return
        notifyRelevantAdministrators(enhet, NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV) { administrators ->
            ScheduledNotification(
                type = NotificationType.TASK,
                title = """Gjennomføringen "${gjennomforing.navn}" har endringer fra Arena, men mangler administrator.""",
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

    private fun logUpdateAvtale(tx: TransactionalSession, dto: AvtaleDto) {
        endringshistorikk.logEndring(
            tx,
            DocumentClass.AVTALE,
            "Endret i Arena",
            "Arena",
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun logUpdateGjennomforing(tx: TransactionalSession, dto: TiltaksgjennomforingDto) {
        endringshistorikk.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            "Endret i Arena",
            "Arena",
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun logTiltaksnummerHentetFraArena(tx: TransactionalSession, dto: TiltaksgjennomforingDto) {
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
