package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.arena.JaNeiStatus
import no.nav.mulighetsrommet.arena.UpsertTiltaksgjennomforingResponse
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.clients.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.tiltak.historikk.TiltakshistorikkArenaGjennomforing
import no.nav.tiltak.historikk.TiltakshistorikkClient
import java.time.LocalDateTime
import java.util.UUID

class TiltakgjennomforingEventProcessor(
    private val config: Config,
    private val entities: ArenaEntityService,
    private val ords: ArenaOrdsProxyClient,
    private val mulighetsrommetApiClient: MulighetsrommetApiClient,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
) : ArenaEventProcessor {

    override suspend fun shouldHandleEvent(event: ArenaEvent): Boolean {
        return event.arenaTable === ArenaTable.Tiltaksgjennomforing
    }

    data class Config(
        val retryUpsertTimes: Int = 1,
    )

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val data = event.decodePayload<ArenaTiltaksgjennomforing>()

        if (data.DATO_FRA == null) {
            return@either ProcessingResult(Ignored, "Tiltaksgjennomføring ignorert fordi DATO_FRA er null")
        }

        if (data.LOKALTNAVN == null) {
            return@either ProcessingResult(Ignored, "Tiltaksgjennomføring ignorert fordi LOKALTNAVN er null")
        }

        if (data.ARBGIV_ID_ARRANGOR == null) {
            return@either ProcessingResult(Ignored, "Tiltaksgjennomføring ignorert fordi ARBGIV_ID_ARRANGOR er null")
        }

        val tiltaksgjennomforing = entities.getMapping(event.arenaTable, event.arenaId)
            .flatMap {
                val previous = entities.getTiltaksgjennomforingOrNull(it.entityId)
                data.toTiltaksgjennomforing(it.entityId, previous?.sanityId)
            }
            .flatMap {
                retry(
                    times = config.retryUpsertTimes,
                    condition = { result -> result.isLeft { error -> error is ProcessingError.ForeignKeyViolation } },
                ) {
                    entities.upsertTiltaksgjennomforing(it)
                }
            }
            .bind()

        val avtaleMapping = tiltaksgjennomforing.avtaleId?.let {
            entities.getMappingIfHandled(ArenaTable.AvtaleInfo, it.toString())
        }
        val sak = entities.getSak(tiltaksgjennomforing.sakId).bind()
        val virksomhetsnummer = tiltaksgjennomforing.arrangorId.let { id ->
            ords.getArbeidsgiver(id)
                .mapLeft { ProcessingError.fromResponseException(it) }
                .flatMap { it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke arrangør i Arena ORDS").left() }
                .map { it.virksomhetsnummer }.bind()
        }

        var status = Handled

        if (erTiltakRelevantForTiltaksadministrasjon(data)) {
            val dbo = tiltaksgjennomforing.toDbo(
                sak = sak,
                virksomhetsnummer = virksomhetsnummer,
                avtaleId = avtaleMapping?.entityId,
            )
            status = upsertTiltaksgjennomforingToApi(event.operation, dbo).bind()
        }

        if (erTiltakRelevantForTiltakshistorikk(status, data)) {
            val tiltakstypeMapping = entities
                .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
                .bind()
            val gjennomforing = data
                .toTiltakshistorikk(tiltaksgjennomforing.id, tiltakstypeMapping.entityId, virksomhetsnummer)
            status = upsertTiltaksgjennomforingToTiltakshistorikk(event.operation, gjennomforing).bind()
        }

        ProcessingResult(status)
    }

    override suspend fun deleteEntity(event: ArenaEvent) = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        mulighetsrommetApiClient.request<Any>(
            HttpMethod.Delete,
            "/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}",
        )
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { entities.deleteTiltaksgjennomforing(mapping.entityId) }
            .bind()
    }

    private fun erTiltakRelevantForTiltaksadministrasjon(data: ArenaTiltaksgjennomforing): Boolean {
        return Tiltakskoder.isGruppetiltak(data.TILTAKSKODE) ||
            Tiltakskoder.isEgenRegiTiltak(data.TILTAKSKODE) ||
            Tiltakskoder.isEnkeltplassTiltak(data.TILTAKSKODE)
    }

    private fun erTiltakRelevantForTiltakshistorikk(
        status: ArenaEntityMapping.Status,
        data: ArenaTiltaksgjennomforing,
    ): Boolean {
        // TODO: slutte å sende opplæring-enkeltplasser når komet får delt disse deltakelsene i prod (via kafka)
        return status == Handled && !Tiltakskoder.isGruppetiltak(data.TILTAKSKODE)
    }

    private suspend fun upsertTiltaksgjennomforingToApi(
        operation: ArenaEvent.Operation,
        dbo: ArenaGjennomforingDbo,
    ): Either<ProcessingError, ArenaEntityMapping.Status> {
        return if (operation == ArenaEvent.Operation.Delete) {
            mulighetsrommetApiClient.request<Any>(
                HttpMethod.Delete,
                "/api/v1/intern/arena/tiltaksgjennomforing/${dbo.id}",
            )
                .flatMap {
                    if (dbo.sanityId != null) {
                        mulighetsrommetApiClient.request<Any>(
                            HttpMethod.Delete,
                            "/api/v1/intern/arena/sanity/tiltaksgjennomforing/${dbo.sanityId}",
                        )
                    } else {
                        it.right()
                    }
                }
        } else {
            mulighetsrommetApiClient.request(HttpMethod.Put, "/api/v1/intern/arena/tiltaksgjennomforing", dbo)
                .onRight {
                    val sanityId = it.body<UpsertTiltaksgjennomforingResponse>().sanityId
                    if (sanityId != null && dbo.sanityId == null) {
                        entities.upsertSanityId(dbo.id, sanityId)
                    }
                }
        }.mapLeft { ProcessingError.fromResponseException(it) }.map { Handled }
    }

    private suspend fun upsertTiltaksgjennomforingToTiltakshistorikk(
        operation: ArenaEvent.Operation,
        dbo: TiltakshistorikkArenaGjennomforing,
    ): Either<ProcessingError, ArenaEntityMapping.Status> {
        return if (operation == ArenaEvent.Operation.Delete) {
            tiltakshistorikkClient.deleteArenaGjennomforing(dbo.id)
        } else {
            tiltakshistorikkClient.upsertArenaGjennomforing(dbo)
        }.mapLeft { ProcessingError.fromResponseException(it) }.map { Handled }
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing(id: UUID, sanityId: UUID?) = Either
        .catch {
            requireNotNull(DATO_FRA)
            requireNotNull(LOKALTNAVN)
            requireNotNull(ARBGIV_ID_ARRANGOR)

            Tiltaksgjennomforing(
                id = id,
                sanityId = sanityId,
                tiltaksgjennomforingId = TILTAKGJENNOMFORING_ID,
                sakId = SAK_ID,
                tiltakskode = TILTAKSKODE,
                arrangorId = ARBGIV_ID_ARRANGOR,
                navn = LOKALTNAVN,
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseNullableTimestamp(DATO_TIL),
                apentForInnsok = STATUS_TREVERDIKODE_INNSOKNING != JaNeiStatus.Nei,
                antallPlasser = ANTALL_DELTAKERE,
                status = TILTAKSTATUSKODE,
                avtaleId = AVTALE_ID,
                deltidsprosent = PROSENT_DELTID,
            )
        }.mapLeft { ProcessingError.ProcessingFailed(it.localizedMessage) }

    private fun Tiltaksgjennomforing.toDbo(sak: Sak, virksomhetsnummer: String, avtaleId: UUID?) = ArenaGjennomforingDbo(
        id = id,
        sanityId = sanityId,
        navn = navn,
        arenaKode = tiltakskode,
        tiltaksnummer = "${sak.aar}#${sak.lopenummer}",
        arrangorOrganisasjonsnummer = virksomhetsnummer,
        startDato = fraDato.toLocalDate(),
        sluttDato = tilDato?.toLocalDate(),
        arenaAnsvarligEnhet = sak.enhet,
        avslutningsstatus = resolveAvslutningsstatus(status, tilDato),
        apentForPamelding = apentForInnsok,
        antallPlasser = antallPlasser ?: 1,
        avtaleId = avtaleId,
        deltidsprosent = deltidsprosent,
    )

    private fun ArenaTiltaksgjennomforing.toTiltakshistorikk(id: UUID, tiltaktypeId: UUID, virksomhetsnummer: String) = TiltakshistorikkArenaGjennomforing(
        id = id,
        navn = requireNotNull(LOKALTNAVN),
        arenaTiltakskode = TILTAKSKODE,
        tiltakstypeId = tiltaktypeId,
        arenaRegDato = ArenaUtils.parseTimestamp(REG_DATO),
        arenaModDato = ArenaUtils.parseTimestamp(MOD_DATO),
        arrangorOrganisasjonsnummer = Organisasjonsnummer(virksomhetsnummer),
        deltidsprosent = PROSENT_DELTID,
    )

    private fun resolveAvslutningsstatus(status: String, tilDato: LocalDateTime?): Avslutningsstatus {
        val avslutningsstatus = Avslutningsstatus.fromArenastatus(status)
        // Overstyr med status AVBRUTT hvis gjennomføring ble AVSLUTTET før sluttdato
        return if (avslutningsstatus == Avslutningsstatus.AVSLUTTET && (tilDato == null || tilDato.isAfter(LocalDateTime.now()))) {
            Avslutningsstatus.AVBRUTT
        } else {
            avslutningsstatus
        }
    }
}

suspend fun <T> retry(
    times: Int = 10,
    delayMs: Long = 1000,
    condition: (T) -> Boolean = { true },
    block: suspend () -> T,
): T {
    repeat(times - 1) {
        val result = block()

        if (!condition(result)) {
            return result
        }

        delay(delayMs)
    }

    return block()
}
