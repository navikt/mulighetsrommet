package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringDbo
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Enkeltplass(
    val gjennomforing: GjennomforingEnkeltplass,
    // TODO: gjøre totrinnskontroll del av GjennomforingEnkeltplass i stedet?
    val okonomi: Totrinnskontroll?,
)

data class EnkeltplassRequest(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val ansvarligEnhet: NavEnhetNummer,
    val kategorisering: OpplaringKategoriseringRequest?,
    val prismodell: UpsertGjennomforingEnkeltplass.Prismodell,
)

data class UpsertGjennomforingEnkeltplass(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val status: GjennomforingStatusType,
    val prismodell: UpsertGjennomforingEnkeltplass.Prismodell,
    val ansvarligEnhet: NavEnhetNummer,
    // TODO: fjerne fra modell når feltene ikke lengre trengs for å deles med arena
    val startDato: LocalDate? = null,
    val sluttDato: LocalDate? = null,
    val navn: String? = null,
    val deltidsprosent: Double = 100.0,
    val antallPlasser: Int = 1,
    val arenaTiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
) {
    sealed interface Prismodell {
        data class Anskaffelse(
            val totalbelop: Int?,
        ) : UpsertGjennomforingEnkeltplass.Prismodell

        data class TilskuddTilOpplaering(
            val tilskudd: Map<Opplaeringtilskudd.Kode, Int>,
            val tilleggsopplysninger: String?,
        ) : UpsertGjennomforingEnkeltplass.Prismodell

        data class IngenKostnader(
            val aarsak: Prismodell.IngenKostnader.Aarsak,
            val tilleggsopplysninger: String?,
        ) : UpsertGjennomforingEnkeltplass.Prismodell
    }
}

class GjennomforingEnkeltplassService(
    private val config: Config,
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val tiltakstyper: TiltakstypeService,
    private val totrinnskontroll: TotrinnskontrollService,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    fun opprettUtkast(utkast: EnkeltplassRequest, opprettetAv: NavIdent): Validated<Enkeltplass> = db.transaction {
        val existing = getEnkeltplass(utkast.id)
        if (existing != null) {
            return existing.right()
        }

        upsert(utkast.toUpsert())
            .also { upsertKategorisering(utkast) }
            .also { updateFreeTextSearch(it, norskIdent = null) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .let { logEndring("Opprettet utkast", it.id, opprettetAv) }
            .right()
    }

    fun soktInn(soktInn: EnkeltplassRequest, opprettetAv: NavIdent): Validated<Enkeltplass> = db.transaction {
        val existing = getEnkeltplass(soktInn.id)

        if (existing?.okonomi?.besluttelse == TotrinnskontrollBesluttelse.GODKJENT) {
            return existing.right()
        }

        when (existing) {
            null -> upsert(soktInn.toUpsert())
                .also { upsertKategorisering(soktInn) }
                .also { updateFreeTextSearch(it, norskIdent = null) }
                .also { publishTiltaksgjennomforingV2ToKafka(it) }

            else -> upsert(soktInn.toUpsert(existing.gjennomforing))
                .also { upsertKategorisering(soktInn) }
                .also { publishTiltaksgjennomforingV2ToKafka(it) }
        }

        settOkonomiTilGodkjenning(soktInn.id, opprettetAv).right()
    }

    /**
     * TODO: kan denne slettes?
     * Ila. høsten 2026 skal enkeltplassene (ENKELAMO, ENKFAGYRKE, HOYEREUTD) migreres fra Arena og de aktive
     * gjennomføringene må bli tilgjengelige i Tiltadm.
     * Det er fortsatt litt uklart hvordan vi gjør dette (f.eks. basert på status vs. cutoff-dato), så foreløpig
     * står denne rutinen ubrukt.
     */
    fun synkroniserFraArena(
        upsert: UpsertGjennomforingEnkeltplass,
    ): Validated<GjennomforingEnkeltplass> = db.transaction {
        return when (val gjennomforing = queries.gjennomforing.getGjennomforing(upsert.id)) {
            is GjennomforingAvtale -> FieldError.of("Gjennomføring er ikke av typen enkeltplass").nel().left()

            is GjennomforingEnkeltplass if (!harEnkeltplassEndringer(upsert, gjennomforing)) -> gjennomforing.right()

            null -> upsert(upsert)
                .also { updateFreeTextSearch(it, norskIdent = null) }
                .also { publishTiltaksgjennomforingV2ToKafka(it) }
                .right()

            else -> upsert(upsert)
                .also { publishTiltaksgjennomforingV2ToKafka(it) }
                .right()
        }
    }

    suspend fun updateArenaData(
        id: UUID,
        arenadata: Gjennomforing.ArenaData,
    ): GjennomforingEnkeltplass = db.transaction {
        val (gjennomforing) = getAndAquireLock(id)
        if (gjennomforing.arena == arenadata) {
            return gjennomforing
        }

        val arenadataDbo = GjennomforingArenaDataDbo(
            id = id,
            tiltaksnummer = arenadata.tiltaksnummer,
            arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
        )
        queries.gjennomforing.setArenaData(arenadataDbo)

        val personalia = getDeltakerPersonalia(id, PersonaliaService.OnBehalfOf.System)

        queries.gjennomforing.getGjennomforingEnkeltplassOrError(id)
            .also { updateFreeTextSearch(it, personalia?.norskIdent()) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .also { logEndring("Oppdatert med tiltaksnummer fra Arena", it.id, Tiltaksadministrasjon) }
    }

    fun updateFromDeltaker(
        deltaker: Deltaker,
        norskIdent: NorskIdent,
    ): Enkeltplass = db.transaction {
        val enkeltplass = getAndAquireLock(deltaker.gjennomforingId)

        getDeltaker(deltaker.gjennomforingId)?.let { eksisterende ->
            when {
                deltaker.id != eksisterende.id && deltaker.erFeilregistrert() -> return enkeltplass
                deltaker.id != eksisterende.id -> error("Enkeltplass med id=${deltaker.gjennomforingId} har allerede en annen deltaker")
                deltaker.endretTidspunkt < eksisterende.endretTidspunkt -> return enkeltplass
            }
        }

        val norskIdent = norskIdent.takeIf { !deltaker.erFeilregistrert() }
        updateFreeTextSearch(enkeltplass.gjennomforing, norskIdent)

        if (!tiltakstyper.erMigrert(enkeltplass.gjennomforing.tiltakstype.tiltakskode)) {
            return enkeltplass
        }

        val upsert = deltaker.toUpsert(enkeltplass.gjennomforing)
        if (!harEnkeltplassEndringer(upsert, enkeltplass.gjennomforing)) {
            return enkeltplass
        }

        upsert(upsert)
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .let { logEndring("Oppdatert fra deltaker", it.id, Tiltaksadministrasjon) }
    }

    fun get(id: UUID): Enkeltplass? = db.session {
        getEnkeltplass(id)
    }

    fun settOkonomiGodkjent(
        id: UUID,
        agent: Agent,
    ): Validated<Enkeltplass> = db.transaction {
        val (_, okonomi) = getAndAquireLock(id)
        if (okonomi == null) {
            return FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel().left()
        }

        return settOkonomiGodkjent(id, okonomi, agent)
    }

    fun settOkonomiPaVent(
        id: UUID,
        navIdent: NavIdent,
        forklaring: String?,
    ): Validated<Enkeltplass> = db.transaction {
        val (_, okonomi) = getAndAquireLock(id)
        if (okonomi == null) {
            return FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel().left()
        }

        settOkonomiPaVent(id, okonomi, navIdent, forklaring)
    }

    private suspend fun QueryContext.getDeltakerPersonalia(
        gjennomforingId: UUID,
        onBehalfOf: PersonaliaService.OnBehalfOf,
    ): Personalia? {
        return getDeltaker(gjennomforingId)?.let {
            personaliaService.getPersonalia(it.id, onBehalfOf)
        }
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker? {
        val deltakelser = queries.deltaker.getByGjennomforingId(gjennomforingId)
        if (deltakelser.size > 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet kun én)")
        }
        return deltakelser.firstOrNull()
    }

    private fun TransactionalQueryContext.upsert(upsert: UpsertGjennomforingEnkeltplass): GjennomforingEnkeltplass {
        val tiltakstype = tiltakstyper.getByTiltakskode(upsert.tiltakskode)

        val prismodellId = upsertPrismodell(upsert.id, upsert.prismodell)
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
        )
        queries.gjennomforing.upsert(dbo)

        return queries.gjennomforing.getGjennomforingEnkeltplassOrError(dbo.id)
    }

    private fun QueryContext.updateFreeTextSearch(
        gjennomforing: GjennomforingEnkeltplass,
        norskIdent: NorskIdent?,
    ) {
        val fts = listOf(gjennomforing.arrangor.navn) +
            gjennomforing.lopenummer.toFreeTextSearch() +
            gjennomforing.arena?.tiltaksnummer?.toFreeTextSearch().orEmpty() +
            listOfNotNull(norskIdent?.let { NorskIdentHasher.hash(it) }) +
            gjennomforing.tiltakstype.navn

        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, fts)
    }

    private fun QueryContext.getEnkeltplass(id: UUID): Enkeltplass? {
        return when (val gjennomforing = queries.gjennomforing.getGjennomforing(id)) {
            null -> null

            !is GjennomforingEnkeltplass -> error("Gjennomføring med id=$id er ikke en enkeltplass")

            else -> {
                val okonomi = totrinnskontroll.get(id, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                Enkeltplass(gjennomforing, okonomi)
            }
        }
    }

    private fun TransactionalQueryContext.getAndAquireLock(id: UUID): Enkeltplass {
        queries.gjennomforing.aquireLock(id)
        return requireNotNull(getEnkeltplass(id))
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        gjennomforingId: UUID,
        endretAv: Agent,
    ): Enkeltplass {
        val enkeltplass = requireNotNull(getEnkeltplass(gjennomforingId))
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.GJENNOMFORING,
            operation,
            endretAv,
            gjennomforingId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(enkeltplass.gjennomforing)
        }
        return enkeltplass
    }

    private fun QueryContext.upsertPrismodell(
        gjennomforingId: UUID,
        prismodell: UpsertGjennomforingEnkeltplass.Prismodell,
    ): UUID {
        val prismodellId = queries.gjennomforing.getPrismodell(gjennomforingId)?.id ?: UUID.randomUUID()
        val dbo = when (prismodell) {
            is UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse -> PrismodellDbo(
                id = prismodellId,
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                prisbetingelser = null,
                tilsagnPerDeltaker = true,
                totalbelop = prismodell.totalbelop,
            )

            is UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering -> PrismodellDbo(
                id = prismodellId,
                type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
                valuta = Valuta.NOK,
                prisbetingelser = prismodell.tilleggsopplysninger,
                tilskudd = prismodell.tilskudd,
            )

            is UpsertGjennomforingEnkeltplass.Prismodell.IngenKostnader -> PrismodellDbo(
                id = prismodellId,
                type = PrismodellType.INGEN_KOSTNADER,
                valuta = Valuta.NOK,
                prisbetingelser = prismodell.tilleggsopplysninger,
                aarsak = prismodell.aarsak.name,
            )
        }
        queries.prismodell.upsert(dbo)
        return dbo.id
    }

    private fun TransactionalQueryContext.upsertKategorisering(
        request: EnkeltplassRequest,
    ) {
        val kurstyper = context(this.session) { OpplaringKategoriseringQueries.getKurstyper() }
        val kurstypeId = when (request.tiltakskode) {
            Tiltakskode.STUDIESPESIALISERING,
            -> kurstyper.find { it.kode == Kurstype.Kode.STUDIESPESIALISERING }?.id

            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            -> kurstyper.find { it.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET }?.id

            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            -> request.kategorisering?.kurstypeId

            else -> null
        }
        val opplaringKategoriseringDbo = OpplaringKategoriseringDbo(
            kurstypeId = kurstypeId,
            bransjeId = request.kategorisering?.bransjeId,
            forerkort = request.kategorisering?.forerkort?.toSet() ?: emptySet(),
            sertifiseringer = request.kategorisering?.sertifiseringer ?: emptySet(),
            utdanningslop = request.kategorisering?.utdanningsprogramId?.let { programId ->
                UtdanningslopDbo(
                    utdanningsprogram = programId,
                    utdanninger = request.kategorisering.larefag?.toSet() ?: emptySet(),
                )
            },
        )
        context(this.session) {
            OpplaringKategoriseringQueries.upsert(
                request.id,
                opplaringKategoriseringDbo,
            )
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

    private fun TransactionalQueryContext.settOkonomiTilGodkjenning(
        id: UUID,
        agent: Agent,
    ): Enkeltplass {
        totrinnskontroll.opprett(id, TotrinnskontrollType.ENKELTPLASS_OKONOMI, agent)
        return logEndring("Deltaker søkt inn", id, agent)
    }

    private fun TransactionalQueryContext.settOkonomiGodkjent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
    ): Validated<Enkeltplass> {
        return totrinnskontroll.godkjent(okonomi.copy(forklaring = null), agent).map {
            logEndring("Enkeltplass ble godkjent", id, agent)
        }
    }

    private fun TransactionalQueryContext.settOkonomiPaVent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
        forklaring: String?,
    ): Either<NonEmptyList<FieldError>, Enkeltplass> {
        return totrinnskontroll.sattPaVent(okonomi, agent, forklaring = forklaring).map {
            logEndring("Godkjenning ble satt på vent", id, agent)
        }
    }
}

private fun EnkeltplassRequest.toUpsert(gjennomforing: GjennomforingEnkeltplass? = null) = UpsertGjennomforingEnkeltplass(
    id = id,
    tiltakskode = tiltakskode,
    arrangorId = arrangorId,
    ansvarligEnhet = ansvarligEnhet,
    prismodell = prismodell,
    status = gjennomforing?.status ?: GjennomforingStatusType.GJENNOMFORES,
    startDato = gjennomforing?.startDato,
    sluttDato = gjennomforing?.sluttDato,
    deltidsprosent = gjennomforing?.deltidsprosent ?: 100.0,
    antallPlasser = gjennomforing?.antallPlasser ?: 1,
    navn = gjennomforing?.navn,
    arenaTiltaksnummer = gjennomforing?.arena?.tiltaksnummer,
    arenaAnsvarligEnhet = gjennomforing?.arena?.ansvarligNavEnhet,
)

private fun Deltaker.toUpsert(
    gjennomforing: GjennomforingEnkeltplass,
): UpsertGjennomforingEnkeltplass = UpsertGjennomforingEnkeltplass(
    id = gjennomforing.id,
    tiltakskode = gjennomforing.tiltakstype.tiltakskode,
    arrangorId = gjennomforing.arrangor.id,
    navn = gjennomforing.navn,
    prismodell = toUpsertPrismodell(gjennomforing.prismodell),
    ansvarligEnhet = gjennomforing.ansvarligEnhet.enhetsnummer,
    arenaTiltaksnummer = gjennomforing.arena?.tiltaksnummer,
    arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
    antallPlasser = gjennomforing.antallPlasser,
    startDato = startDato ?: gjennomforing.startDato,
    sluttDato = sluttDato,
    status = toGjennomforingStatusType(this),
    // TODO: nullable i stedet for default 100
    deltidsprosent = deltakelsesmengder.lastOrNull()?.deltakelsesprosent ?: 100.0,
)

private fun toUpsertPrismodell(prismodell: Prismodell): UpsertGjennomforingEnkeltplass.Prismodell = when (prismodell) {
    is Prismodell.AnnenAvtaltPris -> UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(prismodell.totalbelop)

    is Prismodell.IngenKostnader -> UpsertGjennomforingEnkeltplass.Prismodell.IngenKostnader(
        prismodell.aarsak,
        prismodell.tilleggsopplysninger,
    )

    is Prismodell.TilskuddTilOpplaering -> UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
        prismodell.tilskudd,
        prismodell.tilleggsopplysninger,
    )

    is Prismodell.AvtaltPrisPerHeleUkesverk,
    is Prismodell.AvtaltPrisPerManedsverk,
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
    is Prismodell.AvtaltPrisPerUkesverk,
    is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass,
    is Prismodell.ForhandsgodkjentPrisPerManedsverk,
    -> error("${prismodell.type} er ikke støttet for enkeltplasser")
}

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
    prismodell = toUpsertPrismodell(gjennomforing.prismodell),
    deltidsprosent = gjennomforing.deltidsprosent,
    antallPlasser = gjennomforing.antallPlasser,
    ansvarligEnhet = gjennomforing.ansvarligEnhet.enhetsnummer,
    arenaTiltaksnummer = gjennomforing.arena?.tiltaksnummer,
    arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
)
