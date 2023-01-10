package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.continuations.either
import arrow.core.leftIfNull
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo as MrDeltaker

class TiltakdeltakerEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaTopicConsumer(
    ArenaTables.Deltaker
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.TILTAKDELTAKER_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending,
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(event.payload)

        val tiltaksgjennomforingIsIgnored = entities
            .isIgnored(ArenaTables.Tiltaksgjennomforing, decoded.data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        ensure(!tiltaksgjennomforingIsIgnored) {
            ConsumptionError.Ignored("Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert")
        }

        val mapping = entities.getOrCreateMapping(event)
        val deltaker = decoded.data
            .toDeltaker(mapping.entityId)
            .let { entities.upsertDeltaker(it) }
            .bind()

        val tiltaksgjennomforingMapping = entities
            .getMapping(ArenaTables.Tiltaksgjennomforing, decoded.data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        val norskIdent = ords.getFnr(deltaker.personId)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { it?.fnr }
            .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke norsk ident i Arena ORDS for Arena personId=${deltaker.personId}") }
            .bind()
        val mrDeltaker = deltaker.toDomain(tiltaksgjennomforingMapping.entityId, norskIdent)

        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/internal/arena/deltaker", mrDeltaker)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun ArenaTiltakdeltaker.toDeltaker(id: UUID) = Deltaker(
        id = id,
        tiltaksdeltakerId = TILTAKDELTAKER_ID,
        tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
        personId = PERSON_ID,
        fraDato = ArenaUtils.parseNullableTimestamp(DATO_FRA),
        tilDato = ArenaUtils.parseNullableTimestamp(DATO_TIL),
        status = ArenaUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
    )

    private fun Deltaker.toDomain(tiltaksgjennomforingId: UUID, norskIdent: String) = MrDeltaker(
        id = id,
        tiltaksgjennomforingId = tiltaksgjennomforingId,
        norskIdent = norskIdent,
        status = status,
        fraDato = fraDato,
        tilDato = tilDato,
    )
}
