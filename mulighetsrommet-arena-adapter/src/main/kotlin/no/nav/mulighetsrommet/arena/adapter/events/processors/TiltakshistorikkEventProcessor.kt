package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaHistTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltakelse
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskoder.isGruppetiltak
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltakerGjennomforingId
import no.nav.tiltak.historikk.TiltakshistorikkClient

class TiltakshistorikkEventProcessor(
    private val entities: ArenaEntityService,
    private val client: TiltakshistorikkClient,
    private val ords: ArenaOrdsProxyClient,
) : ArenaEventProcessor {

    override suspend fun shouldHandleEvent(event: ArenaEvent): Boolean {
        return event.arenaTable === ArenaTable.Deltaker || event.arenaTable === ArenaTable.HistDeltaker
    }

    override suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingResult> = either {
        val data: ArenaTiltakdeltakelse = when (event.arenaTable) {
            ArenaTable.Deltaker -> event.decodePayload<ArenaTiltakdeltaker>()
            ArenaTable.HistDeltaker -> event.decodePayload<ArenaHistTiltakdeltaker>()
            else -> raise(ProcessingError.ProcessingFailed("Unexpected table: ${event.arenaTable}"))
        }

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

        if (isGruppetiltak(tiltakstype.tiltakskode)) {
            return@either ProcessingResult(Handled)
        }

        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        val deltaker = TiltakshistorikkArenaDeltakerGjennomforingId(
            id = mapping.entityId,
            arenaGjennomforingId = tiltaksgjennomforing.id,
        )

        upsertDeltakerGjennomforingId(event.operation, deltaker)
            .map { ProcessingResult(Handled) }
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()

//        val norskIdent = ords.getFnr(data.PERSON_ID)
//            .mapLeft { ProcessingError.fromResponseException(it) }
//            .map { it?.fnr }
//            .bind()
//
//        if (norskIdent == null) {
//            return@either ProcessingResult(Ignored, "Historikk ikke relevant fordi fødselsnummer mangler i Arena")
//        }
//
//        val organisasjonsnummer = ords.getArbeidsgiver(tiltaksgjennomforing.arrangorId)
//            .mapLeft { ProcessingError.fromResponseException(it) }
//            .flatMap { it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke arrangør i Arena ORDS").left() }
//            .map { Organisasjonsnummer(it.virksomhetsnummer) }
//            .bind()
//
//        val deltaker = TiltakshistorikkArenaDeltaker(
//            id = mapping.entityId,
//            arenaGjennomforingId = tiltaksgjennomforing.id,
//            arenaRegDato = ArenaUtils.parseTimestamp(data.REG_DATO),
//            arenaModDato = ArenaUtils.parseTimestamp(data.MOD_DATO),
//            norskIdent = NorskIdent(norskIdent),
//            arenaTiltakskode = tiltakstype.tiltakskode,
//            status = ArenaDeltakerStatus.valueOf(data.DELTAKERSTATUSKODE.name),
//            startDato = ArenaUtils.parseNullableTimestamp(data.DATO_FRA),
//            sluttDato = ArenaUtils.parseNullableTimestamp(data.DATO_TIL),
//            beskrivelse = tiltaksgjennomforing.navn,
//            arrangorOrganisasjonsnummer = organisasjonsnummer,
//            dagerPerUke = data.ANTALL_DAGER_PR_UKE,
//            deltidsprosent = data.PROSENT_DELTID,
//        )
//
//        upsertDeltaker(event.operation, deltaker)
//            .map { ProcessingResult(Handled) }
//            .mapLeft { ProcessingError.fromResponseException(it) }
//            .bind()
    }

//    private suspend fun upsertDeltaker(
//        operation: ArenaEvent.Operation,
//        deltaker: TiltakshistorikkArenaDeltaker,
//    ) = if (operation == ArenaEvent.Operation.Delete) {
//        client.deleteArenaDeltaker(deltaker.id)
//    } else {
//        client.upsertArenaDeltaker(deltaker)
//    }

    private suspend fun upsertDeltakerGjennomforingId(
        operation: ArenaEvent.Operation,
        deltaker: TiltakshistorikkArenaDeltakerGjennomforingId,
    ) = if (operation == ArenaEvent.Operation.Delete) {
        Either.right()
    } else {
        client.setArenaDeltakerGjennomforingId(deltaker)
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        client.deleteArenaDeltaker(mapping.entityId)
            .mapLeft { ProcessingError.fromResponseException(it) }
            .bind()
    }
}
