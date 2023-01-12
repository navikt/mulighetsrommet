package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatMap
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaSak
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService
) : ArenaTopicConsumer(
    ArenaTables.Sak
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaSak>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.SAK_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaSak>(event.payload)

        ensure(sakIsRelatedToTiltaksgjennomforing(decoded.data)) {
            ConsumptionError.Ignored("""Sak ignorert fordi den ikke er en tiltakssak (SAKSKODE != "TILT")""")
        }

        ensure(sakHasEnhet(decoded.data)) {
            ConsumptionError.Ignored("""Sak ignorert fordi den ikke har en tilhørende enhet (AETATENHET_ANSVARLIG = null)""")
        }

        decoded.data
            .toSak()
            .flatMap { entities.upsertSak(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: ArenaSak): Boolean = payload.SAKSKODE == "TILT"

    private fun sakHasEnhet(payload: ArenaSak): Boolean = !payload.AETATENHET_ANSVARLIG.isNullOrBlank()

    private fun ArenaSak.toSak() = Either
        .catch {
            Sak(
                sakId = SAK_ID,
                aar = AAR,
                lopenummer = LOPENRSAK,
                enhet = AETATENHET_ANSVARLIG
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }
}
