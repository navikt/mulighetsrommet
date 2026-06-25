package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
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
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassPrisendringDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toFieldErrors
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
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val tiltakstyper: TiltakstypeService,
) {
    fun opprettUtkast(utkast: EnkeltplassRequest, opprettetAv: NavIdent): Validated<Enkeltplass> = db.transaction {
        val enkeltplass = getEnkeltplass(utkast.id)
        if (enkeltplass != null) {
            return enkeltplass.right()
        }

        upsert(utkast.toUpsert())
            .also { upsertKategorisering(utkast.id, utkast.tiltakskode, utkast.kategorisering) }
            .also { updateFreeTextSearch(it, norskIdent = null) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .let { logEndring("Opprettet utkast", it.id, opprettetAv) }
            .right()
    }

    fun soktInn(soktInn: EnkeltplassRequest, opprettetAv: NavIdent): Validated<Enkeltplass> = db.transaction {
        val enkeltplass = getEnkeltplass(soktInn.id)

        if (enkeltplass?.okonomi?.status == TotrinnskontrollStatus.GODKJENT) {
            return enkeltplass.right()
        }

        when (enkeltplass) {
            null -> upsert(soktInn.toUpsert())
                .also { upsertKategorisering(soktInn.id, soktInn.tiltakskode, soktInn.kategorisering) }
                .also { updateFreeTextSearch(it, norskIdent = null) }
                .also { publishTiltaksgjennomforingV2ToKafka(it) }

            else -> upsert(soktInn.toUpsert(enkeltplass.gjennomforing))
                .also { upsertKategorisering(soktInn.id, soktInn.tiltakskode, soktInn.kategorisering) }
                .also { publishTiltaksgjennomforingV2ToKafka(it) }
        }

        settOkonomiTilGodkjenning(soktInn.id, opprettetAv).right()
    }

    fun endreInnhold(
        gjennomforingId: UUID,
        kategorisering: OpplaringKategoriseringRequest?,
    ): Enkeltplass = db.transaction {
        val enkeltplass = getAndAquireLock(gjennomforingId)
        upsertKategorisering(
            gjennomforingId,
            enkeltplass.gjennomforing.tiltakstype.tiltakskode,
            kategorisering,
        )
        publishTiltaksgjennomforingV2ToKafka(enkeltplass.gjennomforing)
        logEndring("Innhold endret", gjennomforingId, Tiltaksadministrasjon)
    }

    fun endrePrisinformasjon(
        gjennomforingId: UUID,
        totrinnskontrollId: UUID,
        endretAv: NavIdent,
        prisinformasjon: UpsertGjennomforingEnkeltplass.Prismodell,
    ): Validated<Enkeltplass> = db.transaction {
        val enkeltplass = getAndAquireLock(gjennomforingId)

        val okonomi = enkeltplass.okonomi ?: error("Kan ikke endre prismodell før deltaker er søkt inn")

        if (okonomi.status == TotrinnskontrollStatus.RETURNERT) {
            return FieldError.of("Kan ikke endre prismodell på en enkeltplass med returnert økonomi").nel().left()
        }

        if (okonomi.kanBesluttes()) {
            upsertPrismodell(gjennomforingId, prisinformasjon)
            val oppdatert = queries.gjennomforing.getGjennomforingEnkeltplassOrError(gjennomforingId)
            publishTiltaksgjennomforingV2ToKafka(oppdatert)

            if (okonomi.status == TotrinnskontrollStatus.SATT_PA_VENT) {
                okonomi.tilbakestill(endretAv).fold({ return it.toFieldErrors().left() }, { tilbakestilt ->
                    queries.totrinnskontroll.upsert(tilbakestilt)
                    outbox.publish(tilbakestilt)
                })
            }

            val prisendring = Totrinnskontroll.opprett(
                totrinnskontrollId,
                gjennomforingId,
                TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
                endretAv,
            )
            queries.totrinnskontroll.upsert(prisendring)
            outbox.publish(prisendring)

            val godkjentPrisendring = prisendring.godkjenn(Tiltaksadministrasjon)
                .getOrElse { error("Kunne ikke godkjenne prisendring") }
            queries.totrinnskontroll.upsert(godkjentPrisendring)
            outbox.publish(godkjentPrisendring)

            logEndring("Pris- og betalingsbetingelser endret", gjennomforingId, endretAv).right()
        } else {
            val prisendring = queries.totrinnskontroll.get(gjennomforingId, TotrinnskontrollType.ENKELTPLASS_PRISENDRING)
            if (prisendring?.kanBesluttes() == true) {
                val returnert = prisendring.returner(Tiltaksadministrasjon)
                    .getOrElse { error("Klarte ikke returnere prisendring") }
                queries.totrinnskontroll.upsert(returnert)
                outbox.publish(returnert)

                queries.enkeltplassPrisendring.getByGjennomforingId(gjennomforingId)?.let { pending ->
                    queries.prismodell.deletePrismodell(pending.prismodellId)
                    queries.enkeltplassPrisendring.deleteByTotrinnskontrollId(pending.totrinnskontrollId)
                }
            }

            val nyPrismodellId = UUID.randomUUID()
            queries.prismodell.upsert(toPrismodellDbo(nyPrismodellId, prisinformasjon))

            val prisendring2 = Totrinnskontroll.opprett(
                totrinnskontrollId,
                gjennomforingId,
                TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
                endretAv,
            )
            queries.totrinnskontroll.upsert(prisendring2)
            outbox.publish(prisendring2)

            queries.enkeltplassPrisendring.insert(
                EnkeltplassPrisendringDbo(
                    totrinnskontrollId = totrinnskontrollId,
                    gjennomforingId = gjennomforingId,
                    prismodellId = nyPrismodellId,
                ),
            )

            logEndring("Prisendring sendt til godkjenning", gjennomforingId, endretAv).right()
        }
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
    ): Enkeltplass = db.transaction {
        val enkeltplass = getAndAquireLock(id)
        if (enkeltplass.gjennomforing.arena == arenadata) {
            return enkeltplass
        }

        val arenadataDbo = GjennomforingArenaDataDbo(
            id = id,
            tiltaksnummer = arenadata.tiltaksnummer,
            arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
        )
        queries.gjennomforing.setArenaData(arenadataDbo)

        val personalia = getDeltakerPersonalia(id, PersonaliaService.OnBehalfOf.System)

        getEnkeltplassOrError(id)
            .also { updateFreeTextSearch(it.gjennomforing, personalia?.norskIdent()) }
            .also { publishTiltaksgjennomforingV2ToKafka(it.gjennomforing) }
            .also { logEndring("Oppdatert med tiltaksnummer fra Arena", it.gjennomforing.id, Tiltaksadministrasjon) }
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
        val gjennomforing = getAndAquireLock(id)

        val prisendring = queries.totrinnskontroll.get(id, TotrinnskontrollType.ENKELTPLASS_PRISENDRING)
        if (prisendring != null && prisendring.kanBesluttes()) {
            return godkjennPrisendring(id, prisendring, agent)
        }

        val okonomi = gjennomforing.okonomi
            ?: return FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel().left()

        return settOkonomiGodkjent(id, okonomi, agent)
    }

    fun settOkonomiPaVent(
        id: UUID,
        navIdent: NavIdent,
        forklaring: String?,
    ): Validated<Enkeltplass> = db.transaction {
        val gjennomforing = getAndAquireLock(id)

        val prisendring = queries.totrinnskontroll.get(id, TotrinnskontrollType.ENKELTPLASS_PRISENDRING)
        if (prisendring != null && prisendring.kanBesluttes()) {
            return settPrisendringPaVent(id, prisendring, navIdent, forklaring)
        }

        val okonomi = gjennomforing.okonomi
            ?: return FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel().left()

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
                val okonomi = queries.totrinnskontroll.get(id, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                Enkeltplass(gjennomforing, okonomi)
            }
        }
    }

    private fun QueryContext.getEnkeltplassOrError(id: UUID): Enkeltplass {
        return checkNotNull(getEnkeltplass(id))
    }

    private fun TransactionalQueryContext.getAndAquireLock(id: UUID): Enkeltplass {
        queries.gjennomforing.aquireLock(id)
        return getEnkeltplassOrError(id)
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        gjennomforingId: UUID,
        endretAv: Agent,
    ): Enkeltplass {
        val enkeltplass = getEnkeltplassOrError(gjennomforingId)
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
        val dbo = toPrismodellDbo(prismodellId, prismodell)
        queries.prismodell.upsert(dbo)
        return dbo.id
    }

    private fun toPrismodellDbo(id: UUID, prismodell: UpsertGjennomforingEnkeltplass.Prismodell): PrismodellDbo = when (prismodell) {
        is UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse -> PrismodellDbo(
            id = id,
            type = PrismodellType.ANNEN_AVTALT_PRIS,
            valuta = Valuta.NOK,
            prisbetingelser = null,
            tilsagnPerDeltaker = true,
            totalbelop = prismodell.totalbelop,
        )

        is UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering -> PrismodellDbo(
            id = id,
            type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
            valuta = Valuta.NOK,
            prisbetingelser = prismodell.tilleggsopplysninger,
            tilskudd = prismodell.tilskudd,
        )

        is UpsertGjennomforingEnkeltplass.Prismodell.IngenKostnader -> PrismodellDbo(
            id = id,
            type = PrismodellType.INGEN_KOSTNADER,
            valuta = Valuta.NOK,
            prisbetingelser = prismodell.tilleggsopplysninger,
            aarsak = prismodell.aarsak.name,
        )
    }

    private fun TransactionalQueryContext.upsertKategorisering(
        id: UUID,
        tiltakskode: Tiltakskode,
        kategorisering: OpplaringKategoriseringRequest?,
    ) {
        val kurstyper = context(session) { OpplaringKategoriseringQueries.getKurstyper() }
        val kurstypeId = when (tiltakskode) {
            Tiltakskode.STUDIESPESIALISERING,
            -> kurstyper.find { it.kode == Kurstype.Kode.STUDIESPESIALISERING }?.id

            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            -> kurstyper.find { it.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET }?.id

            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            -> kategorisering?.kurstypeId

            else -> null
        }
        val opplaringKategoriseringDbo = OpplaringKategoriseringDbo(
            kurstypeId = kurstypeId,
            bransjeId = kategorisering?.bransjeId,
            forerkort = kategorisering?.forerkort?.toSet() ?: emptySet(),
            sertifiseringer = kategorisering?.sertifiseringer ?: emptySet(),
            utdanningslop = kategorisering?.utdanningsprogramId?.let { programId ->
                UtdanningslopDbo(
                    utdanningsprogram = programId,
                    utdanninger = kategorisering.larefag?.toSet() ?: emptySet(),
                )
            },
        )
        context(session) {
            OpplaringKategoriseringQueries.upsert(id, opplaringKategoriseringDbo)
        }
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingEnkeltplass) {
        outbox.publish(TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing))
    }

    private fun TransactionalQueryContext.settOkonomiTilGodkjenning(
        id: UUID,
        agent: Agent,
    ): Enkeltplass {
        val totrinnskontroll = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = id,
            type = TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            behandletAv = agent,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        outbox.publish(totrinnskontroll)
        return logEndring("Deltaker søkt inn", id, agent)
    }

    private fun TransactionalQueryContext.settOkonomiGodkjent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
    ): Validated<Enkeltplass> {
        return okonomi.copy(forklaring = null).godkjenn(agent).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
            logEndring("Enkeltplass ble godkjent", id, agent)
        }
    }

    private fun TransactionalQueryContext.settOkonomiPaVent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
        forklaring: String?,
    ): Validated<Enkeltplass> {
        return okonomi.settPaVent(agent, forklaring = forklaring).mapLeft { it.toFieldErrors() }.map { paVent ->
            queries.totrinnskontroll.upsert(paVent)
            outbox.publish(paVent)
            logEndring("Godkjenning ble satt på vent", id, agent)
        }
    }

    private fun TransactionalQueryContext.godkjennPrisendring(
        gjennomforingId: UUID,
        prisendring: Totrinnskontroll,
        agent: Agent,
    ): Validated<Enkeltplass> {
        return prisendring.godkjenn(agent).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)

            val pending = requireNotNull(queries.enkeltplassPrisendring.getByGjennomforingId(gjennomforingId)) {
                "Fant ikke prisendring for gjennomforing $gjennomforingId"
            }
            val gammelPrismodellId = queries.gjennomforing.getPrismodell(gjennomforingId)?.id
            queries.gjennomforing.setPrismodellId(gjennomforingId, pending.prismodellId)
            gammelPrismodellId?.let { queries.prismodell.deletePrismodell(it) }
            queries.enkeltplassPrisendring.deleteByTotrinnskontrollId(pending.totrinnskontrollId)

            val oppdatert = queries.gjennomforing.getGjennomforingEnkeltplassOrError(gjennomforingId)
            publishTiltaksgjennomforingV2ToKafka(oppdatert)
            logEndring("Prisendring ble godkjent", gjennomforingId, agent)
        }
    }

    private fun TransactionalQueryContext.settPrisendringPaVent(
        gjennomforingId: UUID,
        prisendring: Totrinnskontroll,
        agent: Agent,
        forklaring: String?,
    ): Validated<Enkeltplass> {
        return prisendring.settPaVent(agent, forklaring = forklaring).mapLeft { it.toFieldErrors() }.map { paVent ->
            queries.totrinnskontroll.upsert(paVent)
            outbox.publish(paVent)
            logEndring("Prisendring ble satt på vent", gjennomforingId, agent)
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
    startDato = startDato,
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
