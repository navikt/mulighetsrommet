package no.nav.mulighetsrommet.api.gjennomforing.service

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

data class OpprettGjennomforingArena(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val arenaTiltaksnummer: Tiltaksnummer?,
    val arenaAnsvarligEnhet: String?,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
)

class GjennomforingArenaService(
    private val config: Config,
    private val db: ApiDatabase,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    fun upsert(opprett: OpprettGjennomforingArena): Unit = db.transaction {
        val previous = queries.gjennomforing.getGjennomforing(opprett.id)
        if (previous != null && !harGjennomforingEndringer(opprett, previous)) {
            return
        }

        val dbo = GjennomforingDbo(
            type = GjennomforingType.ARENA,
            id = opprett.id,
            tiltakstypeId = opprett.tiltakstypeId,
            arrangorId = opprett.arrangorId,
            navn = opprett.navn,
            startDato = opprett.startDato,
            sluttDato = opprett.sluttDato,
            status = opprett.status,
            deltidsprosent = opprett.deltidsprosent,
            antallPlasser = opprett.antallPlasser,
            arenaTiltaksnummer = opprett.arenaTiltaksnummer,
            arenaAnsvarligEnhet = opprett.arenaAnsvarligEnhet,
            oppstart = opprett.oppstart,
            pameldingType = opprett.pameldingType,
        )
        queries.gjennomforing.upsert(dbo)

        getOrError(dbo.id).also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    fun updateArenaData(id: UUID, arenadata: Gjennomforing.ArenaData): GjennomforingArena = db.transaction {
        val previous = getOrError(id)
        if (previous.arena == arenadata) {
            return previous
        }

        queries.gjennomforing.setArenaData(
            GjennomforingArenaDataDbo(
                id = id,
                tiltaksnummer = arenadata.tiltaksnummer,
                arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
            ),
        )

        getOrError(id).also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingArena {
        return queries.gjennomforing.getGjennomforingArenaOrError(id)
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingArena) {
        val dto = TiltaksgjennomforingV2Mapper.fromGjennomforingArena(gjennomforing)
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}

private fun harGjennomforingEndringer(opprett: OpprettGjennomforingArena, gjennomforing: Gjennomforing): Boolean {
    return opprett.arrangorId != gjennomforing.arrangor.id ||
        opprett.navn != gjennomforing.navn ||
        opprett.startDato != gjennomforing.startDato ||
        opprett.sluttDato != gjennomforing.sluttDato ||
        opprett.deltidsprosent != gjennomforing.deltidsprosent ||
        opprett.antallPlasser != gjennomforing.antallPlasser ||
        opprett.status != gjennomforing.status ||
        opprett.arenaTiltaksnummer?.value != gjennomforing.arena?.tiltaksnummer?.value ||
        opprett.arenaAnsvarligEnhet != gjennomforing.arena?.ansvarligNavEnhet
}
