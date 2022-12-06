package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.continuations.either
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltak
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.mulighetsrommet.domain.models.Tiltakstype as MrTiltakstype

class TiltakEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val tiltakstyper: TiltakstypeRepository,
    private val arenaEntityMappings: ArenaEntityMappingRepository,
    private val client: MulighetsrommetApiClient
) : ArenaTopicConsumer(
    ArenaTables.Tiltakstype
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltak>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.TILTAKSKODE,
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending,
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaTiltak>(event.payload)

        val mapping = arenaEntityMappings.get(event.arenaTable, event.arenaId) ?: arenaEntityMappings.insert(
            ArenaEntityMapping(event.arenaTable, event.arenaId, UUID.randomUUID())
        )

        val tiltakstype = decoded.data
            .toTiltakstype(mapping.entityId)
            .let { tiltakstyper.upsert(it) }
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
            .bind()

        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/arena/tiltakstype", tiltakstype.toDomain())
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun ArenaTiltak.toTiltakstype(id: UUID) = Tiltakstype(
        id = id,
        navn = TILTAKSNAVN,
        tiltakskode = TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(DATO_TIL)
    )

    private fun Tiltakstype.toDomain() = MrTiltakstype(
        id = id,
        navn = navn,
        tiltakskode = tiltakskode,
    )
}
