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
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.arena.JaNeiStatus
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import java.util.*

class TiltakgjennomforingEventProcessor(
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.Tiltaksgjennomforing

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val data = event.decodePayload<ArenaTiltaksgjennomforing>()

        val isGruppetiltak = isGruppetiltak(data.TILTAKSKODE)
        ensure(isGruppetiltak || isRegisteredAfterAktivitetsplanen(data)) {
            ProcessingError.Ignored("Tiltaksgjennomføring ignorert fordi den ble opprettet før Aktivitetsplanen")
        }

        ensureNotNull(data.DATO_FRA) {
            ProcessingError.Ignored("Tiltaksgjennomføring ignorert fordi DATO_FRA er null")
        }

        ensureNotNull(data.LOKALTNAVN) {
            ProcessingError.Ignored("Tiltaksgjennomføring ignorert fordi LOKALTNAVN er null")
        }

        val mapping = entities.getOrCreateMapping(event)
        val tiltaksgjennomforing = data
            .toTiltaksgjennomforing(mapping.entityId)
            .flatMap { entities.upsertTiltaksgjennomforing(it) }
            .bind()

        if (isGruppetiltak) {
            upsertTiltaksgjennomforing(event.operation, tiltaksgjennomforing).bind()
        } else {
            ArenaEvent.ProcessingStatus.Processed
        }
    }

    override suspend fun deleteEntity(event: ArenaEvent) = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { entities.deleteTiltaksgjennomforing(mapping.entityId) }
            .bind()
    }

    private suspend fun upsertTiltaksgjennomforing(
        operation: ArenaEvent.Operation,
        tiltaksgjennomforing: Tiltaksgjennomforing
    ): Either<ProcessingError, ArenaEvent.ProcessingStatus> = either {
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()
        val sak = entities
            .getSak(tiltaksgjennomforing.sakId)
            .bind()
        val virksomhetsnummer = tiltaksgjennomforing.arrangorId?.let { id ->
            ords.getArbeidsgiver(id)
                .mapLeft { ProcessingError.fromResponseException(it) }
                .leftIfNull { ProcessingError.InvalidPayload("Fant ikke arrangør i Arena ORDS for arrangorId=${tiltaksgjennomforing.arrangorId}") }
                .map { it.virksomhetsnummer }
                .bind()
        }
        val dbo = tiltaksgjennomforing
            .toDbo(tiltakstypeMapping.entityId, sak, virksomhetsnummer)

        val response = if (operation == ArenaEvent.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${dbo.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltaksgjennomforing", dbo)
        }
        response.mapLeft { ProcessingError.fromResponseException(it) }
            .map { ArenaEvent.ProcessingStatus.Processed }
            .bind()
    }

    private fun isRegisteredAfterAktivitetsplanen(data: ArenaTiltaksgjennomforing): Boolean {
        return !ArenaUtils.parseTimestamp(data.REG_DATO).isBefore(AktivitetsplanenLaunchDate)
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing(id: UUID) = Either
        .catch {
            requireNotNull(DATO_FRA)
            requireNotNull(LOKALTNAVN)

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
        .mapLeft { ProcessingError.InvalidPayload(it.localizedMessage) }

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
            avslutningsstatus = Avslutningsstatus.fromArenastatus(status)
        )
}
