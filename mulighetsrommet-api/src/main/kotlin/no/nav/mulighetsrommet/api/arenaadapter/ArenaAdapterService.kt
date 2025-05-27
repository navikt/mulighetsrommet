package no.nav.mulighetsrommet.api.arenaadapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingEksternMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskoder
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterService(
    private val config: Config,
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val sisteTiltaksgjennomforingerV1Topic: String,
    )

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleDto = db.transaction {
        syncArrangorFromBrreg(Organisasjonsnummer(avtale.arrangorOrganisasjonsnummer))

        val previous = queries.avtale.get(avtale.id)
        if (previous?.toArenaAvtaleDbo() == avtale) {
            return@transaction previous
        }

        queries.avtale.upsertArenaAvtale(avtale)

        val next = requireNotNull(queries.avtale.get(avtale.id))

        logUpdateAvtale(next)

        next
    }

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaGjennomforingDbo): UUID? = db.session {
        val tiltakstype = queries.tiltakstype.get(arenaGjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${arenaGjennomforing.tiltakstypeId}")

        syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        if (Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
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
        arenaGjennomforing: ArenaGjennomforingDbo,
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

    private fun upsertGruppetiltak(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaGjennomforingDbo,
    ): Unit = db.transaction {
        require(Tiltakskoder.isGruppetiltak(tiltakstype.arenaKode)) {
            "Gjennomføringer er ikke støttet for tiltakstype ${tiltakstype.arenaKode}"
        }

        val previous = requireNotNull(queries.gjennomforing.get(arenaGjennomforing.id)) {
            "Alle gruppetiltak har blitt migrert. Forventet å finne gjennomføring i databasen."
        }

        if (!hasRelevantChanges(arenaGjennomforing, previous)) {
            logger.info("Gjennomføring hadde ingen endringer")
            return@transaction
        }

        queries.gjennomforing.updateArenaData(
            arenaGjennomforing.id,
            arenaGjennomforing.tiltaksnummer,
            arenaGjennomforing.arenaAnsvarligEnhet,
        )

        val next = requireNotNull(queries.gjennomforing.get(arenaGjennomforing.id)) {
            "Gjennomføring burde ikke være null siden den nettopp ble lagt til"
        }

        if (previous.tiltaksnummer == null) {
            logTiltaksnummerHentetFraArena(next)
        } else {
            logUpdateGjennomforing(next)
        }

        publishToKafka(next)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer) {
        arrangorService.getArrangorOrSyncFromBrreg(orgnr)
            .onLeft { error ->
                if (error == BrregError.NotFound) {
                    logger.warn("Virksomhet mer orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
                }

                throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
            }
    }

    private fun hasRelevantChanges(
        arenaGjennomforing: ArenaGjennomforingDbo,
        current: GjennomforingDto,
    ): Boolean {
        return arenaGjennomforing.tiltaksnummer != current.tiltaksnummer || arenaGjennomforing.arenaAnsvarligEnhet != current.arenaAnsvarligEnhet?.enhetsnummer
    }

    private fun QueryContext.publishToKafka(dto: GjennomforingDto) {
        val eksternDto = TiltaksgjennomforingEksternMapper.toTiltaksgjennomforingV1Dto(dto)

        val record = StoredProducerRecord(
            config.sisteTiltaksgjennomforingerV1Topic,
            eksternDto.id.toString().toByteArray(),
            Json.encodeToString(eksternDto).toByteArray(),
            null,
        )

        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.logUpdateAvtale(dto: AvtaleDto) {
        queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            "Endret i Arena",
            Arena,
            dto.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun QueryContext.logUpdateGjennomforing(dto: GjennomforingDto) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Endret i Arena",
            Arena,
            dto.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun QueryContext.logTiltaksnummerHentetFraArena(dto: GjennomforingDto) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Oppdatert med tiltaksnummer fra Arena",
            Tiltaksadministrasjon,
            dto.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(dto) }
    }
}
