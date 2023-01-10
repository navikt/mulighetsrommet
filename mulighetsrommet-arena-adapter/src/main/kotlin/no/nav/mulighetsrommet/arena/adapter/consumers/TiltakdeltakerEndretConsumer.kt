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
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.dto.isGruppetiltak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo as MrTiltakshistorikk

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
            status = ArenaEvent.ConsumptionStatus.Pending
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
        val tiltaksgjennomforing = entities
            .getTiltaksgjennomforing(tiltaksgjennomforingMapping.entityId)
            .bind()
        val norskIdent = ords.getFnr(deltaker.personId)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { it?.fnr }
            .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke norsk ident i Arena ORDS for Arena personId=${deltaker.personId}") }
            .bind()
        val tiltakstypeMapping = entities
            .getMapping(ArenaTables.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val tiltakstype = entities
            .getTiltakstype(tiltakstypeMapping.entityId)
            .bind()

        val mrtiltakshistorikk = if (isGruppetiltak(tiltakstype.tiltakskode)) {
            deltaker.toGruppeDomain(tiltaksgjennomforing, norskIdent)
        } else {
            val virksomhetsnummer = tiltaksgjennomforing.arrangorId?.let { id ->
                ords.getArbeidsgiver(id)
                    .mapLeft { ConsumptionError.fromResponseException(it) }
                    .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke arrangør i Arena ORDS for arrangorId=${tiltaksgjennomforing.arrangorId}") }
                    .map { it.virksomhetsnummer }
                    .bind()
            }
            deltaker.toIndividuellDomain(tiltaksgjennomforing, tiltakstype, virksomhetsnummer, norskIdent)
        }

        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/internal/arena/tiltakshistorikk", mrtiltakshistorikk)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    private fun ArenaTiltakdeltaker.toDeltaker(id: UUID) = Deltaker(
        id = id,
        tiltaksdeltakerId = TILTAKDELTAKER_ID,
        tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
        personId = PERSON_ID,
        fraDato = ProcessingUtils.getArenaDateFromTo(DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(DATO_TIL),
        status = ProcessingUtils.toDeltakerstatus(DELTAKERSTATUSKODE)
    )

    private fun Deltaker.toGruppeDomain(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: String
    ): MrTiltakshistorikk {
        return MrTiltakshistorikk.Gruppetiltak(
            id = id,
            norskIdent = norskIdent,
            status = status,
            fraDato = fraDato,
            tilDato = tilDato,
            tiltaksgjennomforingId = tiltaksgjennomforing.id
        )
    }

    private fun Deltaker.toIndividuellDomain(
        tiltaksgjennomforing: Tiltaksgjennomforing,
        tiltakstype: Tiltakstype,
        virksomhetsnummer: String?,
        norskIdent: String
    ): MrTiltakshistorikk {
        return MrTiltakshistorikk.IndividueltTiltak(
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
