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
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaAvtaleInfo
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalekode
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class AvtaleInfoEndretConsumer(
    override val config: ConsumerConfig,
    override val events: ArenaEventRepository,
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaTopicConsumer(
    ArenaTables.AvtaleInfo
) {
    companion object {
        val ArenaAvtaleCutoffDate = ArenaUtils.parseTimestamp("2023-01-01 00:00:00")
    }

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeArenaData(payload: JsonElement): ArenaEvent {
        val decoded = ArenaEventData.decode<ArenaAvtaleInfo>(payload)

        return ArenaEvent(
            arenaTable = decoded.table,
            arenaId = decoded.data.AVTALE_ID.toString(),
            payload = payload,
            status = ArenaEvent.ConsumptionStatus.Pending
        )
    }

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val (_, operation, data) = ArenaEventData.decode<ArenaAvtaleInfo>(event.payload)

        ensureNotNull(data.DATO_FRA) {
            ConsumptionError.Ignored("Avtale mangler fra-dato")
        }

        ensureNotNull(data.DATO_TIL) {
            ConsumptionError.Ignored("Avtale mangler til-dato")
        }

        ensureNotNull(data.ARBGIV_ID_LEVERANDOR) {
            ConsumptionError.Ignored("Avtale mangler leverandør")
        }

        ensure(Tiltakskoder.isGruppetiltak(data.TILTAKSKODE)) {
            ConsumptionError.Ignored("Avtale er ikke knyttet til et gruppetiltak")
        }

        ensure(isRecentAvtale(data)) {
            ConsumptionError.Ignored("Avtale har en til-dato som er før 2023")
        }

        val mapping = entities.getOrCreateMapping(event)

        val avtale = data
            .toAvtale(mapping.entityId)
            .flatMap { entities.upsertAvtale(it) }
            .bind()

        val dbo = resolveAvtaleDbo(avtale).bind()

        val response = if (operation == ArenaEventData.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/avtale/${dbo.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/avtale", dbo)
        }
        response.mapLeft { ConsumptionError.fromResponseException(it) }
            .map { ArenaEvent.ConsumptionStatus.Processed }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ConsumptionError, Unit> = either {
        entities.getMapping(event.arenaTable, event.arenaId)
            .map { entities.deleteAvtale(it.entityId) }
            .bind()
    }

    private fun isRecentAvtale(avtale: ArenaAvtaleInfo): Boolean {
        if (avtale.DATO_TIL == null) {
            return true
        }

        return ArenaAvtaleCutoffDate.isBefore(ArenaUtils.parseTimestamp(avtale.DATO_TIL))
    }

    private fun ArenaAvtaleInfo.toAvtale(id: UUID) = Either
        .catch {
            requireNotNull(DATO_FRA)
            requireNotNull(DATO_TIL)
            requireNotNull(ARBGIV_ID_LEVERANDOR)

            Avtale(
                id = id,
                aar = AAR,
                lopenr = LOPENRAVTALE,
                tiltakskode = TILTAKSKODE,
                navn = AVTALENAVN,
                leverandorId = ARBGIV_ID_LEVERANDOR,
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseTimestamp(DATO_TIL),
                ansvarligEnhet = ORGENHET_ANSVARLIG,
                rammeavtale = AVTALEKODE == Avtalekode.Rammeavtale,
                status = Avtale.Status.fromArenaAvtalestatuskode(AVTALESTATUSKODE),
                prisbetingelser = PRIS_BETBETINGELSER,
            )
        }
        .mapLeft { ConsumptionError.InvalidPayload(it.localizedMessage) }

    private suspend fun resolveAvtaleDbo(avtale: Avtale): Either<ConsumptionError, AvtaleDbo> = either {
        val tiltakstypeMapping = entities
            .getMapping(ArenaTables.Tiltakstype, avtale.tiltakskode)
            .bind()
        val leverandorOrganisasjonsnummer = ords.getArbeidsgiver(avtale.leverandorId)
            .mapLeft { ConsumptionError.fromResponseException(it) }
            .leftIfNull { ConsumptionError.InvalidPayload("Fant ikke leverandør i Arena ORDS") }
            .map { it.organisasjonsnummerMorselskap }
            .bind()

        AvtaleDbo(
            id = avtale.id,
            navn = avtale.navn,
            tiltakstypeId = tiltakstypeMapping.entityId,
            avtalenummer = "${avtale.aar}#${avtale.lopenr}",
            leverandorOrganisasjonsnummer = leverandorOrganisasjonsnummer,
            startDato = avtale.fraDato.toLocalDate(),
            sluttDato = avtale.tilDato.toLocalDate(),
            enhet = avtale.ansvarligEnhet,
            avtaletype = if (avtale.rammeavtale) {
                Avtaletype.Rammeavtale
            } else {
                Avtaletype.Avtale
            },
            avtalestatus = when (avtale.status) {
                Avtale.Status.Planlagt -> Avtalestatus.Planlagt
                Avtale.Status.Aktiv -> Avtalestatus.Aktiv
                Avtale.Status.Avsluttet -> Avtalestatus.Avsluttet
                Avtale.Status.Avbrutt -> Avtalestatus.Avbrutt
            },
            prisbetingelser = avtale.prisbetingelser,
        )
    }
}
