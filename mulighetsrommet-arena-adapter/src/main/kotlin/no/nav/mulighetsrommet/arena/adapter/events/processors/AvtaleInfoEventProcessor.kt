package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.continuations.ensureNotNull
import arrow.core.flatMap
import arrow.core.leftIfNull
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaAvtaleInfo
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalekode
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.util.*

class AvtaleInfoEventProcessor(
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.AvtaleInfo

    companion object {
        val ArenaAvtaleCutoffDate = ArenaUtils.parseTimestamp("2023-01-01 00:00:00")
    }

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val data = event.decodePayload<ArenaAvtaleInfo>()

        ensureNotNull(data.AVTALENAVN) {
            ProcessingError.Ignored("Avtale mangler navn")
        }

        ensureNotNull(data.DATO_FRA) {
            ProcessingError.Ignored("Avtale mangler fra-dato")
        }

        ensureNotNull(data.DATO_TIL) {
            ProcessingError.Ignored("Avtale mangler til-dato")
        }

        ensureNotNull(data.ARBGIV_ID_LEVERANDOR) {
            ProcessingError.Ignored("Avtale mangler leverandør")
        }

        ensure(Tiltakskoder.isGruppetiltak(data.TILTAKSKODE)) {
            ProcessingError.Ignored("Avtale er ikke knyttet til et gruppetiltak")
        }

        ensure(isRecentAvtale(data)) {
            ProcessingError.Ignored("Avtale har en til-dato som er før 2023")
        }

        val mapping = entities.getOrCreateMapping(event)

        data
            .toAvtale(mapping.entityId)
            .flatMap { entities.upsertAvtale(it) }
            .flatMap { toAvtaleDbo(it) }
            .flatMap { avtale ->
                val response = if (event.operation == ArenaEvent.Operation.Delete) {
                    client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/avtale/${avtale.id}")
                } else {
                    client.request(HttpMethod.Put, "/api/v1/internal/arena/avtale", avtale)
                }
                response.mapLeft { ProcessingError.fromResponseException(it) }
            }
            .map { ArenaEvent.ProcessingStatus.Processed }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
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
            requireNotNull(AVTALENAVN)
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
        .mapLeft { ProcessingError.InvalidPayload(it.localizedMessage) }

    private suspend fun toAvtaleDbo(avtale: Avtale): Either<ProcessingError, AvtaleDbo> = either {
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, avtale.tiltakskode)
            .bind()
        val leverandorOrganisasjonsnummer = ords.getArbeidsgiver(avtale.leverandorId)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .leftIfNull { ProcessingError.InvalidPayload("Fant ikke leverandør i Arena ORDS") }
            .map { it.organisasjonsnummerMorselskap }
            .bind()

        val startDato = avtale.fraDato.toLocalDate()
        val sluttDato = avtale.tilDato.toLocalDate()

        val avslutningsstatus = when (avtale.status) {
            Avtale.Status.Avsluttet -> Avslutningsstatus.AVSLUTTET
            Avtale.Status.Avbrutt -> Avslutningsstatus.AVBRUTT
            else -> Avslutningsstatus.IKKE_AVSLUTTET
        }

        val avtaletype = when {
            avtale.rammeavtale -> Avtaletype.Rammeavtale
            else -> Avtaletype.Avtale
        }

        AvtaleDbo(
            id = avtale.id,
            navn = avtale.navn,
            tiltakstypeId = tiltakstypeMapping.entityId,
            avtalenummer = "${avtale.aar}#${avtale.lopenr}",
            leverandorOrganisasjonsnummer = leverandorOrganisasjonsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = avtale.ansvarligEnhet,
            avtaletype = avtaletype,
            avslutningsstatus = avslutningsstatus,
            prisbetingelser = avtale.prisbetingelser,
        )
    }
}
