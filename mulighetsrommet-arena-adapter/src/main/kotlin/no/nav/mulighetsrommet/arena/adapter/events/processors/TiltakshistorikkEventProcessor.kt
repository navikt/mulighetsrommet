package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isAmtTiltak
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer

class TiltakshistorikkEventProcessor(
    private val entities: ArenaEntityService,
    // TODO egent client for `tiltakshistorikk`
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient,
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.Deltaker

    override suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingResult> = either {
        val data = event.decodePayload<ArenaTiltakdeltaker>()

        val tiltaksgjennomforingIsIgnored = entities
            .isIgnored(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        if (tiltaksgjennomforingIsIgnored) {
            return@either ProcessingResult(
                Ignored,
                "Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert",
            )
        }

        val tiltaksgjennomforingMapping = entities
            .getMapping(ArenaTable.Tiltaksgjennomforing, data.TILTAKGJENNOMFORING_ID.toString())
            .bind()
        val tiltaksgjennomforing = entities
            .getTiltaksgjennomforing(tiltaksgjennomforingMapping.entityId)
            .bind()
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val tiltakstype = entities
            .getTiltakstype(tiltakstypeMapping.entityId)
            .bind()

        if (isAmtTiltak(tiltakstype.tiltakskode)) {
            return@either ProcessingResult(Handled)
        }

        val deltakerMapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        val norskIdent = ords.getFnr(data.PERSON_ID)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke norsk ident i Arena ORDS").left() }
            .map { NorskIdent(it.fnr) }
            .bind()

        val organisasjonsnummer = ords.getArbeidsgiver(tiltaksgjennomforing.arrangorId)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke arrangør i Arena ORDS").left() }
            .map { Organisasjonsnummer(it.virksomhetsnummer) }
            .bind()

        val deltaker = ArenaDeltakerDbo(
            id = deltakerMapping.entityId,
            norskIdent = norskIdent,
            arenaTiltakskode = tiltakstype.tiltakskode,
            status = ArenaDeltakerStatus.valueOf(data.DELTAKERSTATUSKODE.name),
            startDato = ArenaUtils.parseNullableTimestamp(data.DATO_FRA),
            sluttDato = ArenaUtils.parseNullableTimestamp(data.DATO_TIL),
            beskrivelse = tiltaksgjennomforing.navn,
            arrangorOrganisasjonsnummer = organisasjonsnummer,
            registrertIArenaDato = ArenaUtils.parseTimestamp(data.REG_DATO),
        )

        upsertDeltaker(event.operation, deltaker)
            .map { ProcessingResult(Handled) }
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()
    }

    private suspend fun upsertDeltaker(
        operation: ArenaEvent.Operation,
        deltaker: ArenaDeltakerDbo,
    ) = if (operation == ArenaEvent.Operation.Delete) {
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/deltaker/${deltaker.id}")
    } else {
        client.request(HttpMethod.Put, "/api/v1/internal/arena/deltaker", deltaker)
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()
    }
}
