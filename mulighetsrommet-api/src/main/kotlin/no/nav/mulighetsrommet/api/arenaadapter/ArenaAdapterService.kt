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
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.OpprettEnkeltplass
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
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
    private val enkeltplassService: GjennomforingEnkeltplassService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val gjennomforingV2Topic: String,
    )

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaGjennomforingDbo): UUID? {
        val arrangor = syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        val erTiltakMigrert = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).any {
            it.tiltakskode != null && tiltakstypeService.erMigrert(it.tiltakskode)
        }
        if (erTiltakMigrert) {
            updateArenadata(arenaGjennomforing)
            return null
        }

        if (Tiltakskoder.isEgenRegiTiltak(arenaGjennomforing.arenaKode)) {
            return upsertEgenRegiTiltak(arenaGjennomforing)
        } else if (Tiltakskoder.isGruppetiltak(arenaGjennomforing.arenaKode)) {
            throw IllegalArgumentException("Ugyldig gjennomføring. Forventet ikke å motta nye gjennomføringer for tiltakskode=${arenaGjennomforing.arenaKode}")
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

    private fun updateArenadata(arenaGjennomforing: ArenaGjennomforingDbo): Unit = db.transaction {
        val arenadata = GjennomforingArenaDataDbo(
            id = arenaGjennomforing.id,
            tiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
            arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )

        val previous = queries.gjennomforing.getGjennomforing(arenaGjennomforing.id)
            ?: throw IllegalStateException("Tiltakstype tiltakskode=${arenaGjennomforing.arenaKode} er migrert, men gjennomføring fra Arena er ukjent")

        if (!harArenadataEndringer(arenadata, previous)) {
            return
        }

        queries.gjennomforing.setArenaData(arenadata)

        // TODO: dette burde heller gjøres via en egen service slik at vi har et sentralt sted å sørge for at fts er oppdatert riktig
        //  - dette er også i konflikt med consumer for enkeltplass-fnr
        val fts = when (previous) {
            is GjennomforingAvtale -> listOf(arenaGjennomforing.navn)
            is GjennomforingEnkeltplass, is GjennomforingArena -> listOf()
        }
        // FIXME: Denne kalles her fordi tiltaksnummeret blir satt under panseret. Kanskje bedre om dette settes eksplisitt fra service i stedet
        queries.gjennomforing.setFreeTextSearch(arenaGjennomforing.id, fts)

        val next = queries.gjennomforing.getGjennomforingOrError(arenaGjennomforing.id)
        if (previous.arena?.tiltaksnummer == null) {
            logTiltaksnummerHentetFraArena(next)
        } else {
            logUpdateGjennomforing(next)
        }
        publishTiltaksgjennomforingV2ToKafka(next.id)
    }

    private fun upsertEnkeltplass(
        arenaGjennomforing: ArenaGjennomforingDbo,
        arrangor: ArrangorDto,
    ) {
        require(Tiltakskoder.isEnkeltplassTiltak(arenaGjennomforing.arenaKode)) {
            "Enkeltplasser er ikke støttet for tiltakstype ${arenaGjennomforing.arenaKode}"
        }

        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(arenaGjennomforing.arenaKode).singleOrNull()
            ?: throw IllegalArgumentException("Fant ikke én tiltakstype for arenaKode=${arenaGjennomforing.arenaKode}")

        val opprettEnkeltplass = OpprettEnkeltplass(
            id = arenaGjennomforing.id,
            tiltakstypeId = tiltakstype.id,
            arrangorId = arrangor.id,
            navn = arenaGjennomforing.navn,
            startDato = arenaGjennomforing.startDato,
            sluttDato = arenaGjennomforing.sluttDato,
            status = mapAvslutningsstatus(arenaGjennomforing.avslutningsstatus),
            deltidsprosent = arenaGjennomforing.deltidsprosent,
            antallPlasser = arenaGjennomforing.antallPlasser,
            arenaTiltaksnummer = Tiltaksnummer(arenaGjennomforing.tiltaksnummer),
            arenaAnsvarligEnhet = arenaGjennomforing.arenaAnsvarligEnhet,
        )
        enkeltplassService.upsert(opprettEnkeltplass)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(orgnr).getOrElse { error ->
            if (error == BrregError.NotFound) {
                logger.warn("Virksomhet med orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
            }
            throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
        }
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

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(id: UUID) {
        val dto = getAsTiltaksgjennomforingV2Dto(id)
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(TiltaksgjennomforingV2Dto.serializer(), dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.getAsTiltaksgjennomforingV2Dto(id: UUID): TiltaksgjennomforingV2Dto {
        return when (val gjennomforing = queries.gjennomforing.getGjennomforingOrError(id)) {
            is GjennomforingAvtale -> {
                val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
                TiltaksgjennomforingV2Mapper.fromGjennomforingAvtale(gjennomforing, detaljer)
            }

            is GjennomforingEnkeltplass -> TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing)

            is GjennomforingArena -> TiltaksgjennomforingV2Mapper.fromGjennomforingArena(gjennomforing)
        }
    }
}

private fun harArenadataEndringer(
    arenadata: GjennomforingArenaDataDbo,
    gjennomforing: Gjennomforing,
): Boolean {
    return arenadata != GjennomforingArenaDataDbo(
        gjennomforing.id,
        gjennomforing.arena?.tiltaksnummer,
        gjennomforing.arena?.ansvarligNavEnhet,
    )
}

private fun mapAvslutningsstatus(avslutningsstatus: Avslutningsstatus): GjennomforingStatusType = when (avslutningsstatus) {
    Avslutningsstatus.IKKE_AVSLUTTET -> GjennomforingStatusType.GJENNOMFORES
    Avslutningsstatus.AVBRUTT -> GjennomforingStatusType.AVBRUTT
    Avslutningsstatus.AVLYST -> GjennomforingStatusType.AVLYST
    Avslutningsstatus.AVSLUTTET -> GjennomforingStatusType.AVSLUTTET
}
