package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

data class OpprettGjennomforingEnkeltplass(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val arenaTiltaksnummer: Tiltaksnummer?,
    val arenaAnsvarligEnhet: String?,
)

class GjennomforingEnkeltplassService(
    private val config: Config,
    private val db: ApiDatabase,
    private val deltakerClient: AmtDeltakerClient,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    fun upsert(opprett: OpprettGjennomforingEnkeltplass): Validated<GjennomforingEnkeltplass> = db.transaction {
        return when (val gjennomforing = queries.gjennomforing.getGjennomforing(opprett.id)) {
            null -> {
                upsert(opprett)
                    .also { updateFreeTextSearch(it, null) }
                    .also { publishTiltaksgjennomforingV2ToKafka(it) }
                    .right()
            }

            !is GjennomforingEnkeltplass -> {
                FieldError.of("Gjennomføring er ikke av typen enkeltplass").nel().left()
            }

            else if (!harEnkeltplassEndringer(opprett, gjennomforing)) -> {
                gjennomforing.right()
            }

            else -> {
                upsert(opprett)
                    .also { publishTiltaksgjennomforingV2ToKafka(it) }
                    .right()
            }
        }
    }

    suspend fun updateArenaData(id: UUID, arenadata: Gjennomforing.ArenaData): GjennomforingEnkeltplass = db.transaction {
        val previous = getOrError(id)
        if (previous.arena == arenadata) {
            return previous
        }

        val arenadataDbo = GjennomforingArenaDataDbo(
            id = id,
            tiltaksnummer = arenadata.tiltaksnummer,
            arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
        )
        queries.gjennomforing.setArenaData(arenadataDbo)

        val personalia = getDeltakerPersonalia(id)

        getOrError(id)
            .also { updateFreeTextSearch(it, personalia?.norskIdent) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    fun handleChangeDeltaker(deltaker: AmtDeltakerEksternV1Dto): GjennomforingEnkeltplass = db.transaction {
        getOrError(deltaker.gjennomforingId).also {
            val norskIdent = when (deltaker.status.type) {
                DeltakerStatusType.FEILREGISTRERT -> null
                else -> NorskIdent(deltaker.personIdent)
            }
            updateFreeTextSearch(it, norskIdent)
        }
    }

    private suspend fun getDeltakerPersonalia(gjennomforingId: UUID): DeltakerPersonalia? {
        val deltakelser = db.session { queries.deltaker.getByGjennomforingId(gjennomforingId) }
        if (deltakelser.size > 1) {
            throw Exception("Forventet kun én deltaker på en enkeltplass-gjennomføring")
        }

        return deltakelser.firstOrNull()
            ?.let { deltakerClient.hentPersonalia(listOf(it.id)) }
            ?.map { personalia -> personalia.first() }
            ?.getOrElse { error ->
                when (error) {
                    AmtDeltakerError.NotFound -> null

                    AmtDeltakerError.BadRequest,
                    AmtDeltakerError.Error,
                    -> throw Exception("Klarte ikke hente personalia for deltaker til gjennomføring med id=$gjennomforingId error=$error")
                }
            }
    }

    private fun QueryContext.upsert(opprett: OpprettGjennomforingEnkeltplass): GjennomforingEnkeltplass {
        val prismodellId = getOrCreatePrismodell(opprett.id)
        val dbo = GjennomforingDbo(
            type = GjennomforingType.ENKELTPLASS,
            id = opprett.id,
            tiltakstypeId = opprett.tiltakstypeId,
            arrangorId = opprett.arrangorId,
            navn = opprett.navn,
            startDato = opprett.startDato,
            sluttDato = opprett.sluttDato,
            status = opprett.status,
            deltidsprosent = opprett.deltidsprosent,
            antallPlasser = opprett.antallPlasser,
            oppstart = GjennomforingOppstartstype.ENKELTPLASS,
            pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
            arenaTiltaksnummer = opprett.arenaTiltaksnummer,
            arenaAnsvarligEnhet = opprett.arenaAnsvarligEnhet,
            prismodellId = prismodellId,
            avtaleId = null,
            oppmoteSted = null,
            faneinnhold = null,
            beskrivelse = null,
            estimertVentetidVerdi = null,
            estimertVentetidEnhet = null,
            tilgjengeligForArrangorDato = null,
        )
        queries.gjennomforing.upsert(dbo)
        return getOrError(dbo.id)
    }

    private fun QueryContext.updateFreeTextSearch(gjennomforing: GjennomforingEnkeltplass, norskIdent: NorskIdent?) {
        val fts = listOf(gjennomforing.arrangor.navn) +
            gjennomforing.lopenummer.toFreeTextSearch() +
            gjennomforing.arena?.tiltaksnummer?.toFreeTextSearch().orEmpty() +
            listOfNotNull(norskIdent?.let { NorskIdentHasher.hash(it) })

        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, fts)
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingEnkeltplass {
        return queries.gjennomforing.getGjennomforingEnkeltplassOrError(id)
    }

    private fun QueryContext.getOrCreatePrismodell(gjennomforingId: UUID): UUID {
        return queries.gjennomforing.getPrismodell(gjennomforingId)?.id ?: run {
            val prismodellDbo = PrismodellDbo(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                prisbetingelser = null,
                satser = null,
                systemId = null,
                tilsagnPerDeltaker = false,
            )
            queries.prismodell.upsert(prismodellDbo)
            prismodellDbo.id
        }
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingEnkeltplass) {
        val dto = TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing)
        val record = StoredProducerRecord(
            config.gjennomforingV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}

private fun harEnkeltplassEndringer(opprett: OpprettGjennomforingEnkeltplass, gjennomforing: Gjennomforing): Boolean {
    return opprett.navn != gjennomforing.navn ||
        opprett.arenaTiltaksnummer?.value != gjennomforing.arena?.tiltaksnummer?.value ||
        opprett.arrangorId != gjennomforing.arrangor.id ||
        opprett.startDato != gjennomforing.startDato ||
        opprett.sluttDato != gjennomforing.sluttDato ||
        opprett.arenaAnsvarligEnhet != gjennomforing.arena?.ansvarligNavEnhet ||
        opprett.deltidsprosent != gjennomforing.deltidsprosent ||
        opprett.status != gjennomforing.status
}
