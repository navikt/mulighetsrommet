package no.nav.mulighetsrommet.api.arenaadapter

import arrow.core.getOrElse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.mapper.prisbetingelser
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.*
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
        val gjennomforingV1Topic: String,
        val gjennomforingV2Topic: String,
    )

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): Avtale = db.transaction {
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

        val arrangor = syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        if (Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
            return upsertEgenRegiTiltak(tiltakstype, arenaGjennomforing)
        }

        if (Tiltakskoder.isEnkeltplassTiltak(tiltakstype.arenaKode)) {
            upsertEnkeltplass(tiltakstype, arenaGjennomforing, arrangor)
        } else {
            upsertGruppetiltak(tiltakstype, arenaGjennomforing)
        }

        return null
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

        val next = queries.gjennomforing.getOrError(arenaGjennomforing.id)
        if (previous.tiltaksnummer == null) {
            logTiltaksnummerHentetFraArena(next)
        } else {
            logUpdateGjennomforing(next)
        }

        publishTiltaksgjennomforingV1ToKafka(TiltaksgjennomforingV1Mapper.fromGjennomforing(next))
    }

    private fun upsertEnkeltplass(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaGjennomforingDbo,
        arrangor: ArrangorDto,
    ): Unit = db.transaction {
        require(Tiltakskoder.isEnkeltplassTiltak(tiltakstype.arenaKode)) {
            "Enkeltplasser er ikke støttet for tiltakstype ${tiltakstype.arenaKode}"
        }

        val previous = queries.enkeltplass.get(arenaGjennomforing.id)
        if (
            previous == null ||
            arenaGjennomforing.tiltakstypeId != previous.tiltakstype.id ||
            arenaGjennomforing.arrangorOrganisasjonsnummer != previous.arrangor.organisasjonsnummer.value
        ) {
            queries.enkeltplass.upsert(
                EnkeltplassDbo(
                    id = arenaGjennomforing.id,
                    tiltakstypeId = arenaGjennomforing.tiltakstypeId,
                    arrangorId = arrangor.id,
                ),
            )
        }

        val arenadata = EnkeltplassArenaDataDbo(
            id = arenaGjennomforing.id,
            tiltaksnummer = arenaGjennomforing.tiltaksnummer,
            navn = arenaGjennomforing.navn,
            startDato = arenaGjennomforing.startDato,
            sluttDato = arenaGjennomforing.sluttDato,
            status = when (arenaGjennomforing.avslutningsstatus) {
                Avslutningsstatus.IKKE_AVSLUTTET -> GjennomforingStatusType.GJENNOMFORES
                Avslutningsstatus.AVBRUTT -> GjennomforingStatusType.AVBRUTT
                Avslutningsstatus.AVLYST -> GjennomforingStatusType.AVLYST
                Avslutningsstatus.AVSLUTTET -> GjennomforingStatusType.AVSLUTTET
            },
            arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )
        if (previous == null || hasRelevantChanges(arenadata, previous)) {
            queries.enkeltplass.setArenaData(arenadata)
        }

        val next = queries.enkeltplass.getOrError(arenaGjennomforing.id)
        publishTiltaksgjennomforingV2ToKafka(TiltaksgjennomforingV2Mapper.fromEnkeltplass(next))
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet med orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }
            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
    }

    private fun hasRelevantChanges(
        arenaGjennomforing: ArenaGjennomforingDbo,
        current: Gjennomforing,
    ): Boolean {
        return arenaGjennomforing.tiltaksnummer != current.tiltaksnummer || arenaGjennomforing.arenaAnsvarligEnhet != current.arenaAnsvarligEnhet?.enhetsnummer
    }

    private fun hasRelevantChanges(
        arenadata: EnkeltplassArenaDataDbo,
        current: Enkeltplass,
    ): Boolean {
        return arenadata != EnkeltplassArenaDataDbo(
            id = current.id,
            tiltaksnummer = current.arena?.tiltaksnummer,
            navn = current.arena?.navn,
            startDato = current.arena?.startDato,
            sluttDato = current.arena?.sluttDato,
            status = current.arena?.status,
            arenaAnsvarligEnhet = current.arena?.arenaAnsvarligEnhet,
        )
    }

    private fun QueryContext.logUpdateAvtale(avtale: Avtale) {
        queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            "Endret i Arena",
            Arena,
            avtale.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(avtale) }
    }

    private fun QueryContext.logUpdateGjennomforing(gjennomforing: Gjennomforing) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Endret i Arena",
            Arena,
            gjennomforing.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(gjennomforing) }
    }

    private fun QueryContext.logTiltaksnummerHentetFraArena(gjennomforing: Gjennomforing) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Oppdatert med tiltaksnummer fra Arena",
            Tiltaksadministrasjon,
            gjennomforing.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(gjennomforing) }
    }

    private fun QueryContext.publishTiltaksgjennomforingV1ToKafka(dto: TiltaksgjennomforingV1Dto) {
        val record = StoredProducerRecord(
            config.gjennomforingV1Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(dto: TiltaksgjennomforingV2Dto) {
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}

private fun Avtale.toArenaAvtaleDbo(): ArenaAvtaleDbo? {
    return arrangor?.organisasjonsnummer?.value?.let {
        ArenaAvtaleDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            avtalenummer = avtalenummer,
            arrangorOrganisasjonsnummer = it,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = arenaAnsvarligEnhet?.enhetsnummer,
            avtaletype = avtaletype,
            avslutningsstatus = when (status) {
                is AvtaleStatus.Aktiv -> Avslutningsstatus.IKKE_AVSLUTTET
                is AvtaleStatus.Avbrutt -> Avslutningsstatus.AVBRUTT
                is AvtaleStatus.Avsluttet -> Avslutningsstatus.AVSLUTTET
                is AvtaleStatus.Utkast -> Avslutningsstatus.IKKE_AVSLUTTET
            },
            prisbetingelser = prismodell.prisbetingelser(),
        )
    }
}
