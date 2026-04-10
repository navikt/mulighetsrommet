package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.getOrNone
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.mapper.prisbetingelser
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.util.UUID

data class UpsertGjennomforingEnkeltplass(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val navn: String?,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val prisbetingelser: String?,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val ansvarligEnhet: NavEnhetNummer,
    val arenaTiltaksnummer: Tiltaksnummer?,
    val arenaAnsvarligEnhet: String?,
)

class GjennomforingEnkeltplassService(
    private val config: Config,
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val tiltakstyper: TiltakstypeService,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    fun create(create: UpsertGjennomforingEnkeltplass): Validated<GjennomforingEnkeltplass> = db.transaction {
        if (queries.gjennomforing.getGjennomforing(create.id) != null) {
            return FieldError.of("Gjennomføringen er allerede opprettet").nel().left()
        }

        upsert(create)
            .also { updateFreeTextSearch(it, null) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .right()
    }

    fun update(update: UpsertGjennomforingEnkeltplass): Validated<GjennomforingEnkeltplass> = db.transaction {
        return when (val gjennomforing = queries.gjennomforing.getGjennomforing(update.id)) {
            null -> FieldError.of("Gjennomføring finnes ikke").nel().left()

            !is GjennomforingEnkeltplass -> FieldError.of("Gjennomføring er ikke av typen enkeltplass").nel().left()

            else if (!harEnkeltplassEndringer(update, gjennomforing)) -> gjennomforing.right()

            else -> upsert(update)
                .also { publishTiltaksgjennomforingV2ToKafka(it) }
                .right()
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

        val personalia = getDeltakerPersonalia(id, AccessType.M2M)

        getOrError(id)
            .also { updateFreeTextSearch(it, personalia?.norskIdent) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    fun updateFromDeltaker(deltaker: Deltaker, norskIdent: NorskIdent): GjennomforingEnkeltplass = db.transaction {
        val gjennomforing = getOrError(deltaker.gjennomforingId)

        getDeltaker(deltaker.gjennomforingId)?.let {
            check(it.id == deltaker.id) {
                "Enkeltplass med id=${deltaker.gjennomforingId} har allerede en annen deltaker"
            }

            if (deltaker.endretTidspunkt < it.endretTidspunkt) {
                return gjennomforing
            }
        }

        val norskIdent = norskIdent.takeIf { deltaker.status.type != DeltakerStatusType.FEILREGISTRERT }
        updateFreeTextSearch(gjennomforing, norskIdent)

        if (!tiltakstyper.erMigrert(gjennomforing.tiltakstype.tiltakskode)) {
            return gjennomforing
        }

        upsert(toUpsertGjennomforingEnkeltplass(gjennomforing, deltaker)).also {
            publishTiltaksgjennomforingV2ToKafka(it)
        }
    }

    fun get(id: UUID): GjennomforingEnkeltplass? = db.session {
        when (val gjennomforing = queries.gjennomforing.getGjennomforing(id)) {
            null -> null
            !is GjennomforingEnkeltplass -> error("Gjennomføring med id=$id er ikke en enkeltplass")
            else -> gjennomforing
        }
    }

    private suspend fun QueryContext.getDeltakerPersonalia(gjennomforingId: UUID, accessType: AccessType): Personalia? {
        val deltaker = getDeltaker(gjennomforingId)
        return deltaker
            ?.let { personaliaService.getPersonalia(listOf(it.id), accessType) }
            ?.getOrNone(deltaker.id)
            ?.getOrNull()
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker? {
        val deltakelser = queries.deltaker.getByGjennomforingId(gjennomforingId)
        if (deltakelser.size > 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet kun én)")
        }
        return deltakelser.firstOrNull()
    }

    private fun QueryContext.upsert(upsert: UpsertGjennomforingEnkeltplass): GjennomforingEnkeltplass {
        val tiltakstype = tiltakstyper.getByTiltakskode(upsert.tiltakskode)

        val prismodellId = upsertPrismodell(upsert.id, upsert.prisbetingelser)
        val dbo = GjennomforingDbo(
            type = GjennomforingType.ENKELTPLASS,
            id = upsert.id,
            tiltakstypeId = tiltakstype.id,
            arrangorId = upsert.arrangorId,
            navn = upsert.navn ?: tiltakstype.navn,
            startDato = upsert.startDato,
            sluttDato = upsert.sluttDato,
            status = upsert.status,
            deltidsprosent = upsert.deltidsprosent,
            antallPlasser = upsert.antallPlasser,
            oppstart = GjennomforingOppstartstype.ENKELTPLASS,
            pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
            arenaTiltaksnummer = upsert.arenaTiltaksnummer,
            arenaAnsvarligEnhet = upsert.arenaAnsvarligEnhet,
            prismodellId = prismodellId,
            ansvarligEnhet = upsert.ansvarligEnhet,
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

    private fun QueryContext.upsertPrismodell(gjennomforingId: UUID, prisbetingelser: String?): UUID {
        val prismodell = queries.gjennomforing.getPrismodell(gjennomforingId) ?: run {
            val prismodellDbo = PrismodellDbo(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                prisbetingelser = prisbetingelser,
                satser = null,
                systemId = null,
                tilsagnPerDeltaker = false,
            )
            queries.prismodell.upsert(prismodellDbo)
            queries.prismodell.getOrError(prismodellDbo.id)
        }

        if (prismodell.prisbetingelser() != prisbetingelser) {
            queries.prismodell.setPrisbetingelser(prismodell.id, prisbetingelser)
        }

        return prismodell.id
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

private fun toUpsertGjennomforingEnkeltplass(
    gjennomforing: GjennomforingEnkeltplass,
    deltaker: Deltaker,
): UpsertGjennomforingEnkeltplass = UpsertGjennomforingEnkeltplass(
    id = gjennomforing.id,
    tiltakskode = gjennomforing.tiltakstype.tiltakskode,
    arrangorId = gjennomforing.arrangor.id,
    navn = gjennomforing.navn,
    prisbetingelser = gjennomforing.prismodell.prisbetingelser(),
    ansvarligEnhet = gjennomforing.ansvarligEnhet.enhetsnummer,
    arenaTiltaksnummer = gjennomforing.arena?.tiltaksnummer,
    arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
    antallPlasser = gjennomforing.antallPlasser,
    startDato = deltaker.startDato ?: gjennomforing.startDato,
    sluttDato = deltaker.sluttDato,
    status = toGjennomforingStatusType(deltaker),
    // TODO: nullable i stedet for default 100
    deltidsprosent = deltaker.deltakelsesmengder.lastOrNull()?.deltakelsesprosent ?: 100.0,
)

private fun toGjennomforingStatusType(deltaker: Deltaker): GjennomforingStatusType = when (deltaker.status.type) {
    DeltakerStatusType.FEILREGISTRERT,
    DeltakerStatusType.IKKE_AKTUELL,
    DeltakerStatusType.AVBRUTT_UTKAST,
    DeltakerStatusType.AVBRUTT,
    -> GjennomforingStatusType.AVBRUTT

    DeltakerStatusType.KLADD,
    DeltakerStatusType.PABEGYNT_REGISTRERING,
    DeltakerStatusType.UTKAST_TIL_PAMELDING,
    DeltakerStatusType.SOKT_INN,
    DeltakerStatusType.VURDERES,
    DeltakerStatusType.VENTELISTE,
    DeltakerStatusType.VENTER_PA_OPPSTART,
    DeltakerStatusType.DELTAR,
    -> GjennomforingStatusType.GJENNOMFORES

    DeltakerStatusType.FULLFORT,
    DeltakerStatusType.HAR_SLUTTET,
    -> GjennomforingStatusType.AVSLUTTET
}

private fun harEnkeltplassEndringer(
    opprett: UpsertGjennomforingEnkeltplass,
    gjennomforing: GjennomforingEnkeltplass,
): Boolean = opprett != UpsertGjennomforingEnkeltplass(
    id = gjennomforing.id,
    tiltakskode = gjennomforing.tiltakstype.tiltakskode,
    arrangorId = gjennomforing.arrangor.id,
    navn = gjennomforing.navn,
    startDato = gjennomforing.startDato,
    sluttDato = gjennomforing.sluttDato,
    status = gjennomforing.status,
    prisbetingelser = gjennomforing.prismodell.prisbetingelser(),
    deltidsprosent = gjennomforing.deltidsprosent,
    antallPlasser = gjennomforing.antallPlasser,
    ansvarligEnhet = gjennomforing.ansvarligEnhet.enhetsnummer,
    arenaTiltaksnummer = gjennomforing.arena?.tiltaksnummer,
    arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
)
