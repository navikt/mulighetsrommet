package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.continuations.either
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
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakdeltakerEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaTopicConsumer(
    ArenaTable.Deltaker
) {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaTiltakdeltaker>(payload)

        return ArenaEvent(
            arenaTable = ArenaTable.fromTable(decoded.table),
            arenaId = decoded.data.TILTAKDELTAKER_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val (_, operation, data) = ArenaEventData.decode<ArenaTiltakdeltaker>(event.payload)

        ensure(isRegisteredAfterAktivitetsplanen(data)) {
            ConsumptionError.Ignored("Deltaker ignorert fordi den registrert før Aktivitetsplanen")
        }

        val tiltaksgjennomforingIsIgnored = entities
            .isIgnored(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        ensure(!tiltaksgjennomforingIsIgnored) {
            ConsumptionError.Ignored("Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert")
        }

        val mapping = entities.getOrCreateMapping(event)
        val deltaker = data
            .toDeltaker(mapping.entityId)
            .flatMap { entities.upsertDeltaker(it) }
            .bind()

        val tiltaksgjennomforingMapping = entities
            .getMapping(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        val tiltaksgjennomforing = entities
            .getTiltaksgjennomforing(tiltaksgjennomforingMapping.entityId)
            .bind()
        val norskIdent = ords.getFnr(deltaker.personId)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { it?.fnr }
            .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke norsk ident i Arena ORDS for Arena personId=${deltaker.personId}") }
            .bind()
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val tiltakstype = entities
            .getTiltakstype(tiltakstypeMapping.entityId)
            .bind()

        val tiltakshistorikk = if (isGruppetiltak(tiltakstype.tiltakskode)) {
            deltaker.toGruppeDbo(tiltaksgjennomforing, norskIdent)
        } else {
            val virksomhetsnummer = tiltaksgjennomforing.arrangorId?.let { id ->
                ords.getArbeidsgiver(id)
                    .mapLeft { ConsumptionError.fromResponseException(it) }
                    .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke arrangør i Arena ORDS for arrangorId=${tiltaksgjennomforing.arrangorId}") }
                    .map { it.virksomhetsnummer }
                    .bind()
            }
            deltaker.toIndividuellDbo(tiltaksgjennomforing, tiltakstype, virksomhetsnummer, norskIdent)
        }

        val response = if (operation == ArenaEventData.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${tiltakshistorikk.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltakshistorikk", tiltakshistorikk)
        }
        response.mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ConsumptionError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}")
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .flatMap { entities.deleteDeltaker(mapping.entityId) }
            .bind()
    }

    private fun isRegisteredAfterAktivitetsplanen(data: ArenaTiltakdeltaker): Boolean {
        return !ArenaUtils.parseTimestamp(data.REG_DATO).isBefore(AktivitetsplanenLaunchDate)
    }

    private fun ArenaTiltakdeltaker.toDeltaker(id: UUID) = Either
        .catch {
            Deltaker(
                id = id,
                tiltaksdeltakerId = TILTAKDELTAKER_ID,
                tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
                personId = PERSON_ID,
                fraDato = ArenaUtils.parseNullableTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseNullableTimestamp(DATO_TIL),
                status = ArenaUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }

    private fun Deltaker.toGruppeDbo(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: String
    ): TiltakshistorikkDbo {
        return TiltakshistorikkDbo.Gruppetiltak(
            id = id,
            norskIdent = norskIdent,
            status = status,
            fraDato = fraDato,
            tilDato = tilDato,
            tiltaksgjennomforingId = tiltaksgjennomforing.id
        )
    }

    private fun Deltaker.toIndividuellDbo(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        tiltakstype: Tiltakstype,
        virksomhetsnummer: String?,
        norskIdent: String
    ): TiltakshistorikkDbo {
        return TiltakshistorikkDbo.IndividueltTiltak(
            id = id,
            norskIdent = norskIdent,
            status = status,
            fraDato = fraDato,
            tilDato = tilDato,
            beskrivelse = tiltaksgjennomforing.navn,
            tiltakstypeId = tiltakstype.id,
            virksomhetsnummer = virksomhetsnummer
        )
    }
}
