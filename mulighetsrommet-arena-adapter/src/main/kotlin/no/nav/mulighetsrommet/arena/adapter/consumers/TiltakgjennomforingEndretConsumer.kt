package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.flatMap
import arrow.core.leftIfNull
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.arena.JaNeiStatus
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakgjennomforingEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaTopicConsumer(
    ArenaTable.Tiltaksgjennomforing
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltaksgjennomforing>(payload)

        return ArenaEvent(
            arenaTable = ArenaTable.fromTable(decoded.table),
            arenaId = decoded.data.TILTAKGJENNOMFORING_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val (_, operation, data) = ArenaEventData.decode<ArenaTiltaksgjennomforing>(event.payload)

        val isGruppetiltak = isGruppetiltak(data.TILTAKSKODE)
        ensure(isGruppetiltak || isRegisteredAfterAktivitetsplanen(data)) {
            ConsumptionError.Ignored("Tiltaksgjennomføring ignorert fordi den ble opprettet før Aktivitetsplanen")
        }
        ensureNotNull(data.DATO_FRA) {
            ConsumptionError.Ignored("Tiltaksgjennomføring ignorert fordi DATO_FRA er null")
        }

        val mapping = entities.getOrCreateMapping(event)
        val tiltaksgjennomforing = data
            .toTiltaksgjennomforing(mapping.entityId)
            .flatMap { entities.upsertTiltaksgjennomforing(it) }
            .bind()

        if (isGruppetiltak) {
            upsertTiltaksgjennomforing(operation, tiltaksgjennomforing).bind()
        } else {
            ArenaEvent.ConsumptionStatus.Processed
        }
    }

    override suspend fun deleteEntity(event: ArenaEvent) = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${mapping.entityId}")
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .flatMap { entities.deleteTiltaksgjennomforing(mapping.entityId) }
            .bind()
    }

    private suspend fun upsertTiltaksgjennomforing(
        operation: ArenaEventData.Operation,
        tiltaksgjennomforing: Tiltaksgjennomforing
    ): Either<ConsumptionError, ArenaEvent.ConsumptionStatus> = either {
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val sak = entities
            .getSak(tiltaksgjennomforing.sakId)
            .bind()
        val virksomhetsnummer = tiltaksgjennomforing.arrangorId?.let { id ->
            ords.getArbeidsgiver(id)
                .mapLeft { ConsumptionError.fromResponseException(it) }
                .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke arrangør i Arena ORDS for arrangorId=${tiltaksgjennomforing.arrangorId}") }
                .map { it.virksomhetsnummer }
                .bind()
        }
        val dbo = tiltaksgjennomforing
            .toDbo(tiltakstypeMapping.entityId, sak, virksomhetsnummer)

        val response = if (operation == ArenaEventData.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${dbo.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltaksgjennomforing", dbo)
        }
        response.mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun isRegisteredAfterAktivitetsplanen(data: ArenaTiltaksgjennomforing): Boolean {
        return !ArenaUtils.parseTimestamp(data.REG_DATO).isBefore(AktivitetsplanenLaunchDate)
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing(id: UUID) = Either
        .catch {
            requireNotNull(DATO_FRA)
            Tiltaksgjennomforing(
                id = id,
                tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
                sakId = SAK_ID,
                tiltakskode = TILTAKSKODE,
                arrangorId = ARBGIV_ID_ARRANGOR,
                navn = LOKALTNAVN,
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseNullableTimestamp(DATO_TIL),
                apentForInnsok = STATUS_TREVERDIKODE_INNSOKNING != JaNeiStatus.Nei,
                antallPlasser = ANTALL_DELTAKERE,
                status = TILTAKSTATUSKODE
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }

    private fun Tiltaksgjennomforing.toDbo(tiltakstypeId: UUID, sak: Sak, virksomhetsnummer: String?) =
        TiltaksgjennomforingDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstypeId,
            tiltaksnummer = "${sak.aar}#${sak.lopenummer}",
            virksomhetsnummer = virksomhetsnummer,
            startDato = fraDato.toLocalDate(),
            sluttDato = tilDato?.toLocalDate(),
            enhet = sak.enhet,
            avslutningsstatus = Avslutningsstatus.fromArenaStatus(status)
        )
}
