package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatMap
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltak
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
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
            status = ArenaEvent.ConsumptionStatus.Pending
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either<ConsumptionError, ArenaEvent.ConsumptionStatus> {
        val decoded = ArenaEventData.decode<ArenaTiltak>(event.payload)

        val mapping = entities.getOrCreateMapping(event)
        val tiltakstype = decoded.data
            .toTiltakstype(mapping.entityId)
            .flatMap { entities.upsertTiltakstype(it) }
            .bind()

        val method = if (decoded.operation == ArenaEventData.Operation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.request(method, "/api/v1/internal/arena/tiltakstype", tiltakstype.toDbo())
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ConsumptionError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakstype/${mapping.entityId}")
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
        entities.deleteTiltakstype(mapping.entityId).bind()
    }

    private fun ArenaTiltak.toTiltakstype(id: UUID) = Either
        .catch {
            Tiltakstype(
                id = id,
                navn = TILTAKSNAVN,
                tiltaksgruppekode = TILTAKSGRUPPEKODE,
                tiltakskode = TILTAKSKODE,
                registrertIArenaDato = ArenaUtils.parseTimestamp(REG_DATO),
                sistEndretIArenaDato = ArenaUtils.parseTimestamp(MOD_DATO),
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseTimestamp(DATO_TIL),
                rettPaaTiltakspenger = ArenaUtils.parseJaNei(STATUS_BASISYTELSE),
                administrasjonskode = ADMINISTRASJONKODE,
                sendTilsagnsbrevTilDeltaker = ArenaUtils.parseJaNei(STATUS_KOPI_TILSAGN),
                tiltakstypeSkalHaAnskaffelsesprosess = ArenaUtils.parseJaNei(STATUS_ANSKAFFELSE),
                maksAntallPlasser = MAKS_ANT_PLASSER,
                maksAntallSokere = MAKS_ANT_SOKERE,
                harFastAntallPlasser = ArenaUtils.parseNulleableJaNei(STATUS_FAST_ANT_PLASSER),
                skalSjekkeAntallDeltakere = ArenaUtils.parseNulleableJaNei(STATUS_SJEKK_ANT_DELTAKERE),
                visLonnstilskuddskalkulator = ArenaUtils.parseJaNei(STATUS_KALKULATOR),
                rammeavtale = RAMMEAVTALE,
                opplaeringsgruppe = OPPLAERINGSGRUPPE,
                handlingsplan = HANDLINGSPLAN,
                tiltaksgjennomforingKreverSluttdato = ArenaUtils.parseJaNei(STATUS_SLUTTDATO),
                maksPeriodeIMnd = MAKS_PERIODE,
                tiltaksgjennomforingKreverMeldeplikt = ArenaUtils.parseNulleableJaNei(STATUS_MELDEPLIKT),
                tiltaksgjennomforingKreverVedtak = ArenaUtils.parseJaNei(STATUS_VEDTAK),
                tiltaksgjennomforingReservertForIABedrift = ArenaUtils.parseJaNei(STATUS_IA_AVTALE),
                harRettPaaTilleggsstonader = ArenaUtils.parseJaNei(STATUS_TILLEGGSSTONADER),
                harRettPaaUtdanning = ArenaUtils.parseJaNei(STATUS_UTDANNING),
                tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = ArenaUtils.parseJaNei(AUTOMATISK_TILSAGNSBREV),
                visBegrunnelseForInnsoking = ArenaUtils.parseJaNei(STATUS_BEGRUNNELSE_INNSOKT),
                sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = ArenaUtils.parseJaNei(STATUS_HENVISNING_BREV),
                sendKopibrevOgHovedbrevTilArbeidsgiver = ArenaUtils.parseJaNei(STATUS_KOPIBREV)
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }

    private fun Tiltakstype.toDbo() = TiltakstypeDbo(
        id = id,
        navn = navn,
        tiltakskode = tiltakskode,
        registrertDatoIArena = registrertIArenaDato,
        sistEndretDatoIArena = sistEndretIArenaDato,
        fraDato = fraDato.toLocalDate(),
        tilDato = tilDato.toLocalDate(),
        rettPaaTiltakspenger = rettPaaTiltakspenger
    )
}
