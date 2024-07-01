package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.client.statement.*
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
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.Tiltakskoder.isAmtTiltak
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import java.util.*

class TiltakdeltakerEventProcessor(
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient,
) : ArenaEventProcessor {

    override suspend fun shouldHandleEvent(event: ArenaEvent): Boolean {
        return event.arenaTable === ArenaTable.Deltaker
    }

    override suspend fun handleEvent(event: ArenaEvent) = either {
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

        val norskIdent = ords.getFnr(data.PERSON_ID)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .map { it?.fnr }
            .bind()

        if (norskIdent == null) {
            return@either ProcessingResult(Ignored, "Deltaker ignorert fordi fødselsnummer mangler i Arena")
        }

        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        val deltaker = data
            .toDeltaker(mapping.entityId)
            .flatMap { entities.upsertDeltaker(it) }
            .bind()

        if (isRelevantForBrukersTiltakshistorikk(data)) {
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

            upsertTiltakshistorikk(deltaker, event, tiltakstype, tiltaksgjennomforing, NorskIdent(norskIdent))
                .bind()

            if (isGruppetiltak(tiltakstype.tiltakskode) && !isAmtTiltak(tiltakstype.tiltakskode)) {
                upsertDeltaker(deltaker, event, tiltaksgjennomforing)
                    .bind()
            }
        }

        ProcessingResult(Handled)
    }

    private suspend fun upsertTiltakshistorikk(
        deltaker: Deltaker,
        event: ArenaEvent,
        tiltakstype: Tiltakstype,
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: NorskIdent,
    ): Either<ProcessingError, HttpResponse> = deltaker
        .toTiltakshistorikkDbo(tiltakstype, tiltaksgjennomforing, norskIdent)
        .flatMap { tiltakshistorikk ->
            if (event.operation == ArenaEvent.Operation.Delete) {
                client.request<Any>(HttpMethod.Delete, "/api/v1/intern/arena/tiltakshistorikk/${tiltakshistorikk.id}")
            } else {
                client.request(HttpMethod.Put, "/api/v1/intern/arena/tiltakshistorikk", tiltakshistorikk)
            }.mapLeft {
                ProcessingError.fromResponseException(it)
            }
        }

    private suspend fun upsertDeltaker(
        deltaker: Deltaker,
        event: ArenaEvent,
        tiltaksgjennomforing: Tiltaksgjennomforing,
    ): Either<ProcessingError, HttpResponse> {
        val dbo = deltaker.toDeltakerDbo(tiltaksgjennomforing)

        return if (event.operation == ArenaEvent.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/intern/arena/deltaker/${dbo.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/intern/arena/deltaker", dbo)
        }.mapLeft {
            ProcessingError.fromResponseException(it)
        }
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        client.request<Any>(HttpMethod.Delete, "/api/v1/intern/arena/tiltakshistorikk/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()

        client.request<Any>(HttpMethod.Delete, "/api/v1/intern/arena/deltaker/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()

        entities.deleteDeltaker(mapping.entityId).bind()
    }

    private fun isRelevantForBrukersTiltakshistorikk(data: ArenaTiltakdeltaker): Boolean {
        val date = ArenaUtils.parseNullableTimestamp(data.DATO_TIL) ?: ArenaUtils.parseTimestamp(data.REG_DATO)
        return Tiltakshistorikk.isRelevantTiltakshistorikk(date)
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
                registrertDato = ArenaUtils.parseTimestamp(REG_DATO),
                status = ArenaUtils.toDeltakerstatus(DELTAKERSTATUSKODE),
            )
        }
        .mapLeft { ProcessingError.ProcessingFailed(it.localizedMessage) }

    private suspend fun Deltaker.toTiltakshistorikkDbo(
        tiltakstype: Tiltakstype,
        tiltaksgjennomforing: Tiltaksgjennomforing,
        norskIdent: NorskIdent,
    ) = either<ProcessingError, ArenaTiltakshistorikkDbo> {
        if (isGruppetiltak(tiltakstype.tiltakskode)) {
            ArenaTiltakshistorikkDbo.Gruppetiltak(
                id = id,
                norskIdent = norskIdent,
                status = status,
                fraDato = fraDato,
                tilDato = tilDato,
                registrertIArenaDato = registrertDato,
                tiltaksgjennomforingId = tiltaksgjennomforing.id,
            )
        } else {
            val virksomhetsnummer = tiltaksgjennomforing.arrangorId.let { id ->
                ords.getArbeidsgiver(id)
                    .mapLeft { ProcessingError.fromResponseException(it) }
                    .flatMap {
                        it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke arrangør i Arena ORDS").left()
                    }
                    .map { it.virksomhetsnummer }
                    .bind()
            }
            ArenaTiltakshistorikkDbo.IndividueltTiltak(
                id = id,
                norskIdent = norskIdent,
                status = status,
                fraDato = fraDato,
                tilDato = tilDato,
                registrertIArenaDato = registrertDato,
                beskrivelse = tiltaksgjennomforing.navn,
                tiltakstypeId = tiltakstype.id,
                arrangorOrganisasjonsnummer = virksomhetsnummer,
            )
        }
    }

    private fun Deltaker.toDeltakerDbo(tiltaksgjennomforing: Tiltaksgjennomforing) = DeltakerDbo(
        id = id,
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        status = status,
        opphav = Deltakeropphav.ARENA,
        startDato = fraDato?.toLocalDate(),
        sluttDato = tilDato?.toLocalDate(),
        registrertDato = registrertDato,
    )
}
