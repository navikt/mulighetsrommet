package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.continuations.either
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.arena.JaNeiStatus
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TiltakgjennomforingEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val arenaEntityMappings: ArenaEntityMappingRepository,
    private val client: MulighetsrommetApiClient
) : ArenaTopicConsumer(
    ArenaTables.Tiltaksgjennomforing
) {

    companion object {
        val ArenaDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val AktivitetsplanenLaunchDate: LocalDateTime = LocalDateTime.parse(
            "2017-12-04 00:00:00",
            ArenaDateTimeFormatter
        )
    }

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltaksgjennomforing>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.TILTAKGJENNOMFORING_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending,
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaTiltaksgjennomforing>(event.payload)

        ensure(isRegisteredAfterAktivitetsplanen(decoded.data)) {
            ConsumptionError.Ignored("Tiltaksgjennomføring ignorert fordi den ble opprettet før Aktivitetsplanen")
        }

        val mapping = arenaEntityMappings.get(event.arenaTable, event.arenaId) ?: arenaEntityMappings.insert(
            ArenaEntityMapping.Tiltaksgjennomforing(event.arenaTable, event.arenaId, UUID.randomUUID())
        )

        val tiltaksgjennomforing = decoded.data
            .toTiltaksgjennomforing(mapping.entityId)
            .let { tiltaksgjennomforinger.upsert(it) }
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
            .bind()

        // TODO: oppdater til ny api-modell
        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/arena/tiltaksgjennomforing", tiltaksgjennomforing)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun isRegisteredAfterAktivitetsplanen(data: ArenaTiltaksgjennomforing): Boolean {
        return LocalDateTime.parse(data.REG_DATO, ArenaDateTimeFormatter).isAfter(AktivitetsplanenLaunchDate)
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing(id: UUID) = Tiltaksgjennomforing(
        id = id,
        tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
        sakId = SAK_ID,
        tiltakskode = TILTAKSKODE,
        arrangorId = ARBGIV_ID_ARRANGOR,
        navn = LOKALTNAVN,
        fraDato = ProcessingUtils.getArenaDateFromTo(DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(DATO_TIL),
        apentForInnsok = STATUS_TREVERDIKODE_INNSOKNING != JaNeiStatus.Nei,
        antallPlasser = ANTALL_DELTAKERE,
    )
}
