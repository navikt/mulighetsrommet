package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.http.*
import kotlinx.coroutines.delay
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
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
import no.nav.mulighetsrommet.domain.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.Tiltakskoder.isGruppetiltak
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus
import java.time.LocalDateTime
import java.util.*

class TiltakgjennomforingEventProcessor(
    private val entities: ArenaEntityService,
    private val client: MulighetsrommetApiClient,
    private val ords: ArenaOrdsProxyClient,
    private val config: Config = Config(),
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.Tiltaksgjennomforing

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

        val isGruppetiltak = isGruppetiltak(data.TILTAKSKODE)
        if (!isGruppetiltak && !isRelevantForBrukersTiltakshistorikk(data)) {
            return@either ProcessingResult(
                Ignored,
                "Tiltaksgjennomføring ignorert fordi den ikke lengre er relevant for brukers tiltakshistorikk",
            )
        }

        val avtaleId = data.AVTALE_ID?.let { resolveFromMappingStatus(it).bind() }
        val tiltaksgjennomforing = entities.getMapping(event.arenaTable, event.arenaId)
            .flatMap { data.toTiltaksgjennomforing(it.entityId, avtaleId) }
            .flatMap {
                retry(
                    times = config.retryUpsertTimes,
                    condition = { it.isLeft { error -> error is ProcessingError.ForeignKeyViolation } },
                ) {
                    entities.upsertTiltaksgjennomforing(it)
                }
            }
            .bind()

        if (isGruppetiltak) {
            upsertTiltaksgjennomforing(event.operation, tiltaksgjennomforing).bind()
        } else {
            ProcessingResult(Handled)
        }
    }

    override suspend fun deleteEntity(event: ArenaEvent) = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${mapping.entityId}")
            .mapLeft { ProcessingError.fromResponseException(it) }
            .flatMap { entities.deleteTiltaksgjennomforing(mapping.entityId) }
            .bind()
    }

    override fun getDependentEntities(event: ArenaEvent): List<ArenaEntityMapping> {
        return entities.getDeltakereByTiltaksgjennomforingId(event.arenaId.toInt()).mapNotNull {
            entities.getMapping(ArenaTable.Deltaker, it.tiltaksdeltakerId.toString()).getOrNull()
        }
    }

    private fun resolveFromMappingStatus(avtaleId: Int): Either<ProcessingError, Int?> {
        return entities.getMapping(ArenaTable.AvtaleInfo, avtaleId.toString())
            .flatMap { mapping ->
                when (mapping.status) {
                    ArenaEntityMapping.Status.Handled -> avtaleId.right()
                    ArenaEntityMapping.Status.Ignored -> null.right()
                    else -> ProcessingError.ForeignKeyViolation("Avtale har enda ikke blitt prosessert").left()
                }
            }
    }

    private suspend fun upsertTiltaksgjennomforing(
        operation: ArenaEvent.Operation,
        tiltaksgjennomforing: Tiltaksgjennomforing,
    ): Either<ProcessingError, ProcessingResult> = either {
        val tiltakstypeMapping = entities
            .getMapping(ArenaTable.Tiltakstype, tiltaksgjennomforing.tiltakskode)
            .bind()

        val avtaleMapping = tiltaksgjennomforing.avtaleId?.let {
            entities
                .getMapping(ArenaTable.AvtaleInfo, it.toString())
                .bind()
        }

        val sak = entities
            .getSak(tiltaksgjennomforing.sakId)
            .bind()
        val virksomhetsnummer = tiltaksgjennomforing.arrangorId.let { id ->
            ords.getArbeidsgiver(id)
                .mapLeft { ProcessingError.fromResponseException(it) }
                .flatMap { it?.right() ?: ProcessingError.ProcessingFailed("Fant ikke arrangør i Arena ORDS").left() }
                .map { it.virksomhetsnummer }.bind()
        }
        val dbo =
            tiltaksgjennomforing.toDbo(tiltakstypeMapping.entityId, sak, virksomhetsnummer, avtaleMapping?.entityId)

        val response = if (operation == ArenaEvent.Operation.Delete) {
            client.request<Any>(HttpMethod.Delete, "/api/v1/internal/arena/tiltaksgjennomforing/${dbo.id}")
        } else {
            client.request(HttpMethod.Put, "/api/v1/internal/arena/tiltaksgjennomforing", dbo)
        }
        response.mapLeft { ProcessingError.fromResponseException(it) }.map { ProcessingResult(Handled) }.bind()
    }

    private fun isRelevantForBrukersTiltakshistorikk(data: ArenaTiltaksgjennomforing): Boolean {
        // Siden nye instanser av applikasjonen må lese gjennomføringene før deltakelsene vil man ha tilfenner
        // der denne sjekken returnerer `false` selv om gjennomføringen _egentlig_ har relevante deltakelser
        // i Arena.
        // Vi har vurdert denne mangelen som OK og planlegger å ta en nytt sjau på tiltakshistorikken etter hvert.
        if (anyDeltakereIsRelevantForBrukersTiltakshistorikk(data)) {
            return true
        }

        val date = ArenaUtils.parseNullableTimestamp(data.DATO_TIL) ?: ArenaUtils.parseTimestamp(data.REG_DATO)
        return Tiltakshistorikk.isRelevantTiltakshistorikk(date)
    }

    private fun anyDeltakereIsRelevantForBrukersTiltakshistorikk(data: ArenaTiltaksgjennomforing): Boolean {
        val deltakere = entities.getDeltakereByTiltaksgjennomforingId(data.TILTAKGJENNOMFORING_ID)

        return deltakere.any { deltaker ->
            val date = deltaker.tilDato ?: deltaker.registrertDato
            Tiltakshistorikk.isRelevantTiltakshistorikk(date)
        }
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing(id: UUID, avtaleId: Int?) = Either
        .catch {
            requireNotNull(DATO_FRA)
            requireNotNull(LOKALTNAVN)
            requireNotNull(ARBGIV_ID_ARRANGOR)

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
                status = TILTAKSTATUSKODE,
                avtaleId = avtaleId,
                deltidsprosent = PROSENT_DELTID,
            )
        }.mapLeft { ProcessingError.ProcessingFailed(it.localizedMessage) }

    private fun Tiltaksgjennomforing.toDbo(tiltakstypeId: UUID, sak: Sak, virksomhetsnummer: String, avtaleId: UUID?) =
        ArenaTiltaksgjennomforingDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstypeId,
            tiltaksnummer = "${sak.aar}#${sak.lopenummer}",
            arrangorOrganisasjonsnummer = virksomhetsnummer,
            startDato = fraDato.toLocalDate(),
            sluttDato = tilDato?.toLocalDate(),
            arenaAnsvarligEnhet = sak.enhet,
            avslutningsstatus = resolveAvslutningsstatus(status, tilDato),
            apentForInnsok = apentForInnsok,
            antallPlasser = antallPlasser,
            avtaleId = avtaleId,
            deltidsprosent = deltidsprosent,
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
