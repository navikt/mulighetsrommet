package no.nav.mulighetsrommet.api.arenaadapter

import arrow.core.getOrElse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingEnkeltplassDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class ArenaAdapterService(
    private val config: Config,
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val gjennomforingV2Topic: String,
    )

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaGjennomforingDbo): UUID? = db.session {
        val arrangor = syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        if (Tiltakskoder.isEgenRegiTiltak(arenaGjennomforing.arenaKode)) {
            return upsertEgenRegiTiltak(arenaGjennomforing)
        } else if (Tiltakskoder.isGruppetiltak(arenaGjennomforing.arenaKode)) {
            upsertGruppetiltak(arenaGjennomforing)
        } else if (Tiltakskoder.isEnkeltplassTiltak(arenaGjennomforing.arenaKode)) {
            upsertEnkeltplass(arenaGjennomforing, arrangor)
        }
        return null
    }

    suspend fun removeSanityTiltaksgjennomforing(sanityId: UUID) {
        sanityService.deleteSanityTiltaksgjennomforing(sanityId)
    }

    private suspend fun upsertEgenRegiTiltak(
        arenaGjennomforing: ArenaGjennomforingDbo,
    ): UUID? {
        require(Tiltakskoder.isEgenRegiTiltak(arenaGjennomforing.arenaKode)) {
            "Gjennomføring for tiltakstype ${arenaGjennomforing.arenaKode} skal ikke skrives til Sanity"
        }

        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).singleOrNull()
            ?: throw IllegalArgumentException("Fant ikke én tiltakstype for arenaKode=${arenaGjennomforing.arenaKode}")

        val sluttDato = arenaGjennomforing.sluttDato
        return if (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate)) {
            sanityService.createOrPatchSanityTiltaksgjennomforing(arenaGjennomforing, tiltakstype.sanityId)
        } else {
            null
        }
    }

    private fun upsertGruppetiltak(
        arenaGjennomforing: ArenaGjennomforingDbo,
    ): Unit = db.transaction {
        val arenadata = GjennomforingArenaDataDbo(
            id = arenaGjennomforing.id,
            tiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
            arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )

        val previous = queries.gjennomforing.getGjennomforingAvtaleOrError(arenaGjennomforing.id)

        if (!harEndringer(arenadata, previous)) {
            logger.info("Gjennomføring hadde ingen endringer")
            return@transaction
        }

        queries.gjennomforing.setArenaData(arenadata)
        // FIXME: Denne kalles her fordi tiltaksnummeret blir satt under panseret. Kanskje bedre om dette settes eksplisitt i stedet
        queries.gjennomforing.setFreeTextSearch(arenaGjennomforing.id, listOf(arenaGjennomforing.navn))

        val next = queries.gjennomforing.getGjennomforingAvtaleOrError(arenaGjennomforing.id)
        if (previous.arena?.tiltaksnummer == null) {
            logTiltaksnummerHentetFraArena(next)
        } else {
            logUpdateGjennomforing(next)
        }

        publishTiltaksgjennomforingV2ToKafka(TiltaksgjennomforingV2Mapper.fromGjennomforing(next))
    }

    private fun upsertEnkeltplass(
        arenaGjennomforing: ArenaGjennomforingDbo,
        arrangor: ArrangorDto,
    ): Unit = db.transaction {
        require(Tiltakskoder.isEnkeltplassTiltak(arenaGjennomforing.arenaKode)) {
            "Enkeltplasser er ikke støttet for tiltakstype ${arenaGjennomforing.arenaKode}"
        }

        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).singleOrNull()
            ?: throw IllegalArgumentException("Fant ikke én tiltakstype for arenaKode=${arenaGjennomforing.arenaKode}")

        val arenadata = GjennomforingArenaDataDbo(
            id = arenaGjennomforing.id,
            tiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
            arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )

        if (tiltakstypeService.erMigrert(tiltakstype.tiltakskode!!)) {
            val previous = queries.gjennomforing.getGjennomforingEnkeltplass(arenaGjennomforing.id)
                ?: throw IllegalStateException("Tiltakstype tiltakskode=${tiltakstype.tiltakskode} er migrert, men gjennomføring fra Arena er ukjent")

            if (harEndringer(arenadata, previous)) {
                queries.gjennomforing.setArenaData(arenadata)
                // FIXME: Denne kalles her fordi tiltaksnummeret blir satt under panseret. Kanskje bedre om dette settes eksplisitt i stedet
                queries.gjennomforing.setFreeTextSearch(arenaGjennomforing.id, listOf())
            }

            return
        }

        val sluttDato = arenaGjennomforing.sluttDato
        val status = when (arenaGjennomforing.avslutningsstatus) {
            Avslutningsstatus.IKKE_AVSLUTTET -> GjennomforingStatusType.GJENNOMFORES
            Avslutningsstatus.AVBRUTT -> GjennomforingStatusType.AVBRUTT
            Avslutningsstatus.AVLYST -> GjennomforingStatusType.AVLYST
            Avslutningsstatus.AVSLUTTET -> GjennomforingStatusType.AVSLUTTET
        }
        if (sluttDato == null || sluttDato >= ArenaMigrering.EnkeltplassSluttDatoCutoffDate) {
            val previous = queries.gjennomforing.getGjennomforingEnkeltplass(arenaGjennomforing.id)
            val dbo = GjennomforingEnkeltplassDbo(
                id = arenaGjennomforing.id,
                tiltakstypeId = tiltakstype.id,
                arrangorId = arrangor.id,
                navn = arenaGjennomforing.navn,
                startDato = arenaGjennomforing.startDato,
                sluttDato = sluttDato,
                status = status,
                deltidsprosent = arenaGjennomforing.deltidsprosent,
                antallPlasser = arenaGjennomforing.antallPlasser ?: 1,
            )

            if (previous == null || harEndringer(dbo, previous) || harEndringer(arenadata, previous)) {
                queries.gjennomforing.upsertEnkeltplass(dbo)
                queries.gjennomforing.setArenaData(arenadata)

                val next = queries.gjennomforing.getGjennomforingEnkeltplassOrError(arenaGjennomforing.id)
                publishTiltaksgjennomforingV2ToKafka(TiltaksgjennomforingV2Mapper.fromGjennomforing(next))
            }
        } else {
            val previous = queries.gjennomforing.getGjennomforingArena(arenaGjennomforing.id)
            val dbo = GjennomforingArenaDbo(
                id = arenaGjennomforing.id,
                tiltakstypeId = tiltakstype.id,
                arrangorId = arrangor.id,
                navn = arenaGjennomforing.navn,
                startDato = arenaGjennomforing.startDato,
                sluttDato = sluttDato,
                status = status,
                deltidsprosent = arenaGjennomforing.deltidsprosent,
                antallPlasser = arenaGjennomforing.antallPlasser ?: 1,
                tiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
                arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
            )

            if (previous == null || harEndringer(dbo, previous)) {
                queries.gjennomforing.upsertGjennomforingArena(dbo)

                val next = queries.gjennomforing.getGjennomforingArenaOrError(arenaGjennomforing.id)
                publishTiltaksgjennomforingV2ToKafka(TiltaksgjennomforingV2Mapper.fromGjennomforing(next))
            }
        }
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet med orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }
            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
    }

    private fun harEndringer(
        arenadata: GjennomforingArenaDataDbo,
        current: Gjennomforing,
    ): Boolean {
        return arenadata.tiltaksnummer != current.arena?.tiltaksnummer || arenadata.arenaAnsvarligEnhet != current.arena?.ansvarligNavEnhet
    }

    private fun harEndringer(
        enkeltplass: GjennomforingEnkeltplassDbo,
        current: GjennomforingEnkeltplass,
    ): Boolean {
        return enkeltplass != GjennomforingEnkeltplassDbo(
            id = current.id,
            tiltakstypeId = current.tiltakstype.id,
            arrangorId = current.arrangor.id,
            navn = current.navn,
            startDato = current.startDato,
            sluttDato = current.sluttDato,
            status = current.status,
            deltidsprosent = current.deltidsprosent,
            antallPlasser = current.antallPlasser,
        )
    }

    private fun harEndringer(
        dbo: GjennomforingArenaDbo,
        current: GjennomforingArena,
    ): Boolean {
        return dbo != GjennomforingArenaDbo(
            id = current.id,
            tiltakstypeId = current.tiltakstype.id,
            arrangorId = current.arrangor.id,
            navn = current.navn,
            startDato = current.startDato,
            sluttDato = current.sluttDato,
            status = current.status,
            deltidsprosent = current.deltidsprosent,
            antallPlasser = current.antallPlasser,
            tiltaksnummer = current.arena?.tiltaksnummer!!,
            arenaAnsvarligEnhet = current.arena.ansvarligNavEnhet,
        )
    }

    private fun QueryContext.logUpdateGjennomforing(gjennomforing: GjennomforingAvtale) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Endret i Arena",
            Arena,
            gjennomforing.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(gjennomforing) }
    }

    private fun QueryContext.logTiltaksnummerHentetFraArena(gjennomforing: GjennomforingAvtale) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            "Oppdatert med tiltaksnummer fra Arena",
            Tiltaksadministrasjon,
            gjennomforing.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(gjennomforing) }
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(dto: TiltaksgjennomforingV2Dto) {
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(TiltaksgjennomforingV2Dto.serializer(), dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}
