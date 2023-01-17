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

    private fun ArenaTiltak.toTiltakstype(id: UUID) = Either
        .catch {
            Tiltakstype(
                id = id,
                navn = TILTAKSNAVN,
                tiltaksgruppekode = TILTAKSGRUPPEKODE,
                tiltakskode = TILTAKSKODE,
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseTimestamp(DATO_TIL),
                rettPaaTiltakspenger = ArenaUtils.jaNeiTilBoolean(STATUS_BASISYTELSE),
                administrasjonskode = ADMINISTRASJONKODE,
                sendTilsagnsbrevTilDeltaker = ArenaUtils.jaNeiTilBoolean(STATUS_KOPI_TILSAGN),
                tiltakstypeSkalHaAnskaffelsesprosess = ArenaUtils.jaNeiTilBoolean(STATUS_ANSKAFFELSE),
                maksAntallPlasser = MAKS_ANT_PLASSER,
                maksAntallSokere = MAKS_ANT_SOKERE,
                harFastAntallPlasser = ArenaUtils.optionalJaNeiTilBoolean(STATUS_FAST_ANT_PLASSER),
                skalSjekkeAntallDeltakere = ArenaUtils.optionalJaNeiTilBoolean(STATUS_SJEKK_ANT_DELTAKERE),
                visLonnstilskuddskalkulator = ArenaUtils.jaNeiTilBoolean(STATUS_KALKULATOR),
                rammeavtale = RAMMEAVTALE,
                opplaeringsgruppe = OPPLAERINGSGRUPPE,
                handlingsplan = HANDLINGSPLAN,
                tiltaksgjennomforingKreverSluttdato = ArenaUtils.jaNeiTilBoolean(STATUS_SLUTTDATO),
                maksPeriodeIMnd = MAKS_PERIODE,
                tiltaksgjennomforingKreverMeldeplikt = ArenaUtils.optionalJaNeiTilBoolean(STATUS_MELDEPLIKT),
                tiltaksgjennomforingKreverVedtak = ArenaUtils.jaNeiTilBoolean(STATUS_VEDTAK),
                tiltaksgjennomforingReservertForIABedrift = ArenaUtils.jaNeiTilBoolean(STATUS_IA_AVTALE),
                harRettPaaTilleggsstonader = ArenaUtils.jaNeiTilBoolean(STATUS_TILLEGGSSTONADER),
                harRettPaaUtdanning = ArenaUtils.jaNeiTilBoolean(STATUS_UTDANNING),
                tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = ArenaUtils.jaNeiTilBoolean(AUTOMATISK_TILSAGNSBREV),
                visBegrunnelseForInnsoking = ArenaUtils.jaNeiTilBoolean(STATUS_BEGRUNNELSE_INNSOKT),
                sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = ArenaUtils.jaNeiTilBoolean(STATUS_HENVISNING_BREV),
                sendKopibrevOgHovedbrevTilArbeidsgiver = ArenaUtils.jaNeiTilBoolean(STATUS_KOPIBREV)
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }

    private fun Tiltakstype.toDbo() = TiltakstypeDbo(
        id = id,
        navn = navn,
        tiltakskode = tiltakskode,
        fraDato = fraDato.toLocalDate(),
        tilDato = tilDato.toLocalDate(),
        rettPaaTiltakspenger = rettPaaTiltakspenger
    )
}
