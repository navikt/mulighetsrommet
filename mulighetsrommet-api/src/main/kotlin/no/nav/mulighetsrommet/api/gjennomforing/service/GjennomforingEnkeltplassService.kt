package no.nav.mulighetsrommet.api.gjennomforing.service

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

data class OpprettEnkeltplass(
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

    fun upsert(opprett: OpprettEnkeltplass): Unit = db.transaction {
        val previous = queries.gjennomforing.getGjennomforing(opprett.id)
        if (previous != null && !harEnkeltplassEndringer(opprett, previous)) {
            return
        }

        val sluttDato = opprett.sluttDato
        val type = if (sluttDato == null || sluttDato >= ArenaMigrering.EnkeltplassSluttDatoCutoffDate) {
            GjennomforingType.ENKELTPLASS
        } else {
            GjennomforingType.ARENA
        }

        val prismodellId = if (type == GjennomforingType.ENKELTPLASS) {
            getOrCreatePrismodell(opprett.id)
        } else {
            null
        }

        val dbo = GjennomforingDbo(
            type = type,
            id = opprett.id,
            tiltakstypeId = opprett.tiltakstypeId,
            arrangorId = opprett.arrangorId,
            navn = opprett.navn,
            startDato = opprett.startDato,
            sluttDato = opprett.sluttDato,
            status = opprett.status,
            deltidsprosent = opprett.deltidsprosent,
            antallPlasser = opprett.antallPlasser,
            oppstart = GjennomforingOppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
            arenaTiltaksnummer = opprett.arenaTiltaksnummer,
            arenaAnsvarligEnhet = opprett.arenaAnsvarligEnhet,
            prismodellId = prismodellId,
        )
        queries.gjennomforing.upsert(dbo)

        publishTiltaksgjennomforingV2ToKafka(dbo.id)
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

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(id: UUID) {
        val dto = getAsTiltaksgjennomforingV2Dto(id)
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
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

private fun harEnkeltplassEndringer(opprett: OpprettEnkeltplass, gjennomforing: Gjennomforing): Boolean {
    return opprett.navn != gjennomforing.navn ||
        opprett.arenaTiltaksnummer?.value != gjennomforing.arena?.tiltaksnummer?.value ||
        opprett.arrangorId != gjennomforing.arrangor.id ||
        opprett.startDato != gjennomforing.startDato ||
        opprett.sluttDato != gjennomforing.sluttDato ||
        opprett.arenaAnsvarligEnhet != gjennomforing.arena?.ansvarligNavEnhet ||
        opprett.deltidsprosent != gjennomforing.deltidsprosent ||
        opprett.status != gjennomforing.status
}
