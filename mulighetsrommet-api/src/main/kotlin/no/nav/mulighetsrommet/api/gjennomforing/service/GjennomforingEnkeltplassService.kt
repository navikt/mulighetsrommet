package no.nav.mulighetsrommet.api.gjennomforing.service

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

data class OpprettGjennomforingEnkeltplass(
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
)

class GjennomforingEnkeltplassService(
    private val config: Config,
    private val db: ApiDatabase,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    fun upsert(opprett: OpprettGjennomforingEnkeltplass): Unit = db.transaction {
        val previous = queries.gjennomforing.getGjennomforing(opprett.id)
        if (previous != null && !harEnkeltplassEndringer(opprett, previous)) {
            return
        }

        val prismodellId = getOrCreatePrismodell(opprett.id)
        val dbo = GjennomforingDbo(
            type = GjennomforingType.ENKELTPLASS,
            id = opprett.id,
            tiltakstypeId = opprett.tiltakstypeId,
            arrangorId = opprett.arrangorId,
            navn = opprett.navn,
            startDato = opprett.startDato,
            sluttDato = opprett.sluttDato,
            status = opprett.status,
            deltidsprosent = opprett.deltidsprosent,
            antallPlasser = opprett.antallPlasser,
            oppstart = GjennomforingOppstartstype.ENKELTPLASS,
            pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
            arenaTiltaksnummer = opprett.arenaTiltaksnummer,
            arenaAnsvarligEnhet = opprett.arenaAnsvarligEnhet,
            prismodellId = prismodellId,
            avtaleId = null,
            oppmoteSted = null,
            faneinnhold = null,
            beskrivelse = null,
            estimertVentetidVerdi = null,
            estimertVentetidEnhet = null,
            tilgjengeligForArrangorDato = null,
        )
        queries.gjennomforing.upsert(dbo)

        getOrError(dbo.id)
            .also { updateFreeTextSearch(it) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    fun updateArenaData(id: UUID, arenadata: Gjennomforing.ArenaData): GjennomforingEnkeltplass = db.transaction {
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

        getOrError(id)
            .also { updateFreeTextSearch(it) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    private fun QueryContext.updateFreeTextSearch(gjennomforing: GjennomforingEnkeltplass) {
        val fts = listOf(gjennomforing.arrangor.navn) +
            gjennomforing.lopenummer.toFreeTextSearch() +
            gjennomforing.arena?.tiltaksnummer?.toFreeTextSearch().orEmpty()

        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, fts)
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingEnkeltplass {
        return queries.gjennomforing.getGjennomforingEnkeltplassOrError(id)
    }

    private fun QueryContext.getOrCreatePrismodell(gjennomforingId: UUID): UUID {
        return queries.gjennomforing.getPrismodell(gjennomforingId)?.id ?: run {
            val prismodellDbo = PrismodellDbo(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                prisbetingelser = null,
                satser = null,
                systemId = null,
            )
            queries.prismodell.upsert(prismodellDbo)
            prismodellDbo.id
        }
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingEnkeltplass) {
        val dto = TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing)
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}

private fun harEnkeltplassEndringer(opprett: OpprettGjennomforingEnkeltplass, gjennomforing: Gjennomforing): Boolean {
    return opprett.navn != gjennomforing.navn ||
        opprett.arenaTiltaksnummer?.value != gjennomforing.arena?.tiltaksnummer?.value ||
        opprett.arrangorId != gjennomforing.arrangor.id ||
        opprett.startDato != gjennomforing.startDato ||
        opprett.sluttDato != gjennomforing.sluttDato ||
        opprett.arenaAnsvarligEnhet != gjennomforing.arena?.ansvarligNavEnhet ||
        opprett.deltidsprosent != gjennomforing.deltidsprosent ||
        opprett.status != gjennomforing.status
}
