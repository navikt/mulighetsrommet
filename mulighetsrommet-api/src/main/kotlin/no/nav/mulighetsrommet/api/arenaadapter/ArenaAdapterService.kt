package no.nav.mulighetsrommet.api.arenaadapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.services.TILTAKSADMINISTRASJON_SYSTEM_BRUKER
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArenaAdapterService(
    private val db: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
    private val endringshistorikk: EndringshistorikkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleDto {
        syncArrangorFromBrreg(Organisasjonsnummer(avtale.arrangorOrganisasjonsnummer))

        return db.transaction { tx ->
            val previous = avtaler.get(avtale.id)
            if (previous?.toArenaAvtaleDbo() == avtale) {
                return@transaction previous
            }

            avtaler.upsertArenaAvtale(tx, avtale)

            val next = avtaler.get(avtale.id, tx)!!

            logUpdateAvtale(tx, next)

            next
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

    suspend fun removeSanityTiltaksgjennomforing(sanityId: UUID) {
        sanityService.deleteSanityTiltaksgjennomforing(sanityId)
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
        require(Tiltakskoder.isGruppetiltak(tiltakstype.arenaKode)) {
            "Gjennomføringer er ikke støttet for tiltakstype ${tiltakstype.arenaKode}"
        }

        val previous = requireNotNull(tiltaksgjennomforinger.get(arenaGjennomforing.id)) {
            "Alle gruppetiltak har blitt migrert. Forventet å finne gjennomføring i databasen."
        }

        if (!hasRelevantChanges(arenaGjennomforing, previous)) {
            logger.info("Gjennomføring hadde ingen endringer")
            return
        }

        db.transactionSuspend { tx ->
            tiltaksgjennomforinger.updateArenaData(
                arenaGjennomforing.id,
                arenaGjennomforing.tiltaksnummer,
                arenaGjennomforing.arenaAnsvarligEnhet,
                tx,
            )

            val next = requireNotNull(tiltaksgjennomforinger.get(arenaGjennomforing.id, tx)) {
                "Gjennomføring burde ikke være null siden den nettopp ble lagt til"
            }

            if (previous.tiltaksnummer == null) {
                logTiltaksnummerHentetFraArena(tx, next)
            } else {
                logUpdateGjennomforing(tx, next)
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

    private fun hasRelevantChanges(
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingDto,
    ): Boolean {
        return arenaGjennomforing.tiltaksnummer != current.tiltaksnummer || arenaGjennomforing.arenaAnsvarligEnhet != current.arenaAnsvarligEnhet?.enhetsnummer
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
}
