package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptySetOf
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.admin.personalia.PersonaliaPort
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.admin.totrinnskontroll.toFieldErrors
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.Enkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollError
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.validation.Validated
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UpsertEnkeltplass(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val ansvarligEnhet: NavEnhetNummer,
    val kategorisering: OpplaringKategoriseringRequest?,
    val prismodell: Prismodell,
) {
    sealed interface Prismodell {
        data class Anskaffelse(
            val totalbelop: Int,
        ) : UpsertEnkeltplass.Prismodell

        data class TilskuddTilOpplaering(
            val tilskudd: Map<Opplaeringtilskudd.Kode, Int>,
            val tilleggsopplysninger: String?,
        ) : UpsertEnkeltplass.Prismodell

        data class IngenKostnader(
            val aarsak: Prismodell.IngenKostnader.Aarsak,
            val tilleggsopplysninger: String?,
        ) : UpsertEnkeltplass.Prismodell
    }
}

data class UpsertArenaEnkeltplass(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val arrangorId: UUID,
    val status: GjennomforingEnkeltplassStatus,
    val prismodell: UpsertEnkeltplass.Prismodell,
    val ansvarligEnhet: NavEnhetNummer,
    // TODO: fjerne fra modell når feltene ikke lengre trengs for å deles med arena
    val startDato: LocalDate? = null,
    val sluttDato: LocalDate? = null,
    val navn: String? = null,
    val deltidsprosent: Double = 100.0,
    val antallPlasser: Int = 1,
    val arenaTiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class TotrinnskontrollBehandling(
    val id: UUID,
    val behandletAv: NavIdent,
    val begrunnelse: String?,
)

class GjennomforingEnkeltplassService(
    private val db: AdminDatabase,
    private val personaliaPort: PersonaliaPort,
    private val tiltakstyper: TiltakstypeService,
) {

    fun opprettUtkast(utkast: UpsertEnkeltplass, opprettetAv: NavIdent): Validated<Enkeltplass> = db.transaction {
        val enkeltplass = getEnkeltplass(utkast.id)
        if (enkeltplass != null) {
            return@transaction enkeltplass.right()
        }

        upsert(utkast.toUpsert())
            .also { upsertKategorisering(utkast.id, utkast.tiltakskode, utkast.kategorisering) }
            .also { updateFreeTextSearch(it, norskIdent = null) }
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .let { logEndring("Opprettet utkast", it.id, opprettetAv) }
            .right()
    }

    fun soktInn(soktInn: UpsertEnkeltplass, behandling: TotrinnskontrollBehandling): Validated<Enkeltplass> = db.transaction {
        val enkeltplass = getEnkeltplass(soktInn.id)

        if (enkeltplass?.okonomi?.status == TotrinnskontrollStatus.GODKJENT) {
            return@transaction enkeltplass.right()
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

        settOkonomiTilGodkjenning(soktInn.id, behandling).right()
    }

    fun endreInnhold(
        gjennomforingId: UUID,
        kategorisering: OpplaringKategoriseringRequest?,
        behandletAv: NavIdent,
    ): Enkeltplass = db.transaction {
        val enkeltplass = getAndAcquireLock(gjennomforingId)
        upsertKategorisering(
            gjennomforingId,
            enkeltplass.gjennomforing.tiltakstype.tiltakskode,
            kategorisering,
        )
        publishTiltaksgjennomforingV2ToKafka(enkeltplass.gjennomforing)
        logEndring("Innhold endret", gjennomforingId, behandletAv)
    }

    fun endrePrisinformasjon(
        gjennomforingId: UUID,
        prisinformasjon: UpsertEnkeltplass.Prismodell,
        behandling: TotrinnskontrollBehandling,
    ): Validated<Enkeltplass> = db.transaction {
        if (queries.totrinnskontroll.findById(behandling.id) != null) {
            return@transaction getEnkeltplassOrError(gjennomforingId).right()
        }

        val enkeltplass = getAndAcquireLock(gjennomforingId)

        val okonomi = enkeltplass.okonomi
            ?: return@transaction FieldError.of("Kan ikke endre prismodell før deltaker er søkt inn").nel().left()

        when (okonomi.status) {
            TotrinnskontrollStatus.TIL_BEHANDLING,
            TotrinnskontrollStatus.SATT_PA_VENT,
            -> {
                okonomi.returner(behandling.behandletAv).fold({
                    return@transaction it.toFieldErrors().left()
                }, { returnert ->
                    queries.totrinnskontroll.upsert(returnert)
                    outbox.publish(returnert)
                })

                endrePrismodell(gjennomforingId, prisinformasjon, behandling)
            }

            TotrinnskontrollStatus.GODKJENT -> {
                enkeltplass.prisendring?.also {
                    slettEksisterendePrisendring(it.totrinnskontroll)
                }
                upsertPrisendring(gjennomforingId, prisinformasjon, behandling)
                logEndring("Prisendring sendt til godkjenning", gjennomforingId, behandling.behandletAv).right()
            }

            TotrinnskontrollStatus.RETURNERT,
            -> return@transaction FieldError.of("Kan ikke endre prismodell på en enkeltplass med returnert økonomi")
                .nel()
                .left()
        }
    }

    fun tilbakekallPrisinformasjon(
        gjennomforingId: UUID,
        behandling: TotrinnskontrollBehandling,
    ): Either<TotrinnskontrollError, Enkeltplass> = db.transaction {
        val enkeltplass = getAndAcquireLock(gjennomforingId)

        val totrinnskontroll = queries.totrinnskontroll.findById(behandling.id)
            ?: return@transaction enkeltplass.right()

        totrinnskontroll.returner(behandling.behandletAv).map { returnert ->
            queries.totrinnskontroll.upsert(returnert)
            outbox.publish(returnert)

            if (returnert.type == TotrinnskontrollType.ENKELTPLASS_PRISENDRING) {
                commands.enkeltplassPrisendring.getByGjennomforingId(gjennomforingId)?.let { pending ->
                    commands.enkeltplassPrisendring.deleteByTotrinnskontrollId(pending.totrinnskontrollId)
                    repository.prismodell.deletePrismodell(pending.prismodellId)
                }
            }

            logEndring("Prisendring tilbakekalt", gjennomforingId, behandling.behandletAv)
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
        upsert: UpsertArenaEnkeltplass,
    ): Validated<GjennomforingEnkeltplass> = db.transaction {
        return@transaction when (val gjennomforing = repository.gjennomforing.getGjennomforing(upsert.id)) {
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
    ): Enkeltplass {
        val (eksisterende, deltaker) = db.session { getEnkeltplassOrError(id) to getDeltaker(id) }
        if (eksisterende.gjennomforing.arena == arenadata) {
            return eksisterende
        }

        // Personalia hentes før transaksjonen fordi kallet er suspend og AdminDatabase.transaction ikke er inline
        val personalia = deltaker?.let { personaliaPort.getPersonaliaAsSystem(it.id) }

        return db.transaction {
            val enkeltplass = getAndAcquireLock(id)
            if (enkeltplass.gjennomforing.arena == arenadata) {
                return@transaction enkeltplass
            }

            val arenaData = SetGjennomforingArenaData(
                id = id,
                tiltaksnummer = arenadata.tiltaksnummer,
                arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
            )
            commands.gjennomforing.setArenaData(arenaData)

            getEnkeltplassOrError(id)
                .also { updateFreeTextSearch(it.gjennomforing, personalia?.norskIdent()) }
                .also { publishTiltaksgjennomforingV2ToKafka(it.gjennomforing) }
                .also { logEndring("Oppdatert med tiltaksnummer fra Arena", it.gjennomforing.id, Tiltaksadministrasjon) }
        }
    }

    fun updateFromDeltaker(
        deltaker: Deltaker,
        norskIdent: NorskIdent,
    ): Enkeltplass = db.transaction {
        val enkeltplass = getAndAcquireLock(deltaker.gjennomforingId)

        getDeltaker(deltaker.gjennomforingId)?.let { eksisterende ->
            when {
                deltaker.id != eksisterende.id && deltaker.erFeilregistrert() -> return@transaction enkeltplass
                deltaker.id != eksisterende.id -> error("Enkeltplass med id=${deltaker.gjennomforingId} har allerede en annen deltaker")
                deltaker.endretTidspunkt < eksisterende.endretTidspunkt -> return@transaction enkeltplass
            }
        }

        val norskIdent = norskIdent.takeIf { !deltaker.erFeilregistrert() }
        updateFreeTextSearch(enkeltplass.gjennomforing, norskIdent)

        if (!tiltakstyper.erMigrert(enkeltplass.gjennomforing.tiltakstype.tiltakskode)) {
            return@transaction enkeltplass
        }

        val upsert = deltaker.toUpsert(enkeltplass.gjennomforing)
        if (!harEnkeltplassEndringer(upsert, enkeltplass.gjennomforing)) {
            return@transaction enkeltplass
        }

        upsert(upsert)
            .also { publishTiltaksgjennomforingV2ToKafka(it) }
            .also { haandterAvsluttendeStatus(enkeltplass, deltaker.status.type) }
            .let { logEndring("Oppdatert fra deltaker", it.id, Tiltaksadministrasjon) }
    }

    fun get(id: UUID): Enkeltplass? = db.session {
        getEnkeltplass(id)
    }

    fun settOkonomiGodkjent(
        id: UUID,
        forventetTotrinnskontrollId: UUID,
        navIdent: NavIdent,
    ): Either<EnkeltplassBeslutningError, Enkeltplass> = db.transaction {
        val enkeltplass = getAndAcquireLock(id)
        if (!kanBeslutteEnkeltplass(enkeltplass, navIdent)) {
            return@transaction EnkeltplassBeslutningError.ManglerTilgang("Du mangler tilgang til å beslutte økonomi for enkeltplassen").left()
        }

        val prisendring = enkeltplass.prisendring
        if (prisendring?.totrinnskontroll?.kanBesluttes() == true) {
            if (prisendring.totrinnskontroll.id != forventetTotrinnskontrollId) {
                if (erPrisendringTrukketTilbake(id, forventetTotrinnskontrollId)) {
                    return@transaction EnkeltplassBeslutningError.ForslagTrukketTilbake(
                        "Nav-veileder har trukket tilbake forslaget. Det kan ikke lenger godkjennes.",
                    ).left()
                }
                return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                    FieldError.of("Grunnlaget har endret seg siden det ble hentet. Forsøk igjen.").nel(),
                ).left()
            }

            return@transaction godkjennPrisendring(id, prisendring.totrinnskontroll, navIdent)
                .mapLeft(EnkeltplassBeslutningError::Valideringsfeil)
        }

        val okonomi = enkeltplass.okonomi
            ?: return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel(),
            ).left()

        if (okonomi.id != forventetTotrinnskontrollId) {
            if (erPrisendringTrukketTilbake(id, forventetTotrinnskontrollId)) {
                return@transaction EnkeltplassBeslutningError.ForslagTrukketTilbake(
                    "Nav-veileder har trukket tilbake forslaget. Det kan ikke lenger godkjennes.",
                ).left()
            }
            return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                FieldError.of("Grunnlaget har endret seg siden det ble hentet. Forsøk igjen.").nel(),
            ).left()
        }

        return@transaction settOkonomiGodkjent(id, okonomi, navIdent).mapLeft(EnkeltplassBeslutningError::Valideringsfeil)
    }

    private fun QueryContext.erPrisendringTrukketTilbake(
        gjennomforingId: UUID,
        totrinnskontrollId: UUID,
    ): Boolean {
        val historiskKontroll = queries.totrinnskontroll.findById(totrinnskontrollId) ?: return false
        return historiskKontroll.entityId == gjennomforingId &&
            historiskKontroll.type == TotrinnskontrollType.ENKELTPLASS_PRISENDRING &&
            historiskKontroll.status == TotrinnskontrollStatus.RETURNERT
    }

    fun settOkonomiPaVent(
        id: UUID,
        forventetTotrinnskontrollId: UUID,
        navIdent: NavIdent,
        begrunnelse: String?,
    ): Either<EnkeltplassBeslutningError, Enkeltplass> = db.transaction {
        val enkeltplass = getAndAcquireLock(id)
        if (!kanBeslutteEnkeltplass(enkeltplass, navIdent)) {
            return@transaction EnkeltplassBeslutningError.ManglerTilgang("Du mangler tilgang til å beslutte økonomi for enkeltplassen").left()
        }

        val prisendring = enkeltplass.prisendring
        if (prisendring?.totrinnskontroll?.kanBesluttes() == true) {
            if (prisendring.totrinnskontroll.id != forventetTotrinnskontrollId) {
                return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                    FieldError.of("Grunnlaget har endret seg siden det ble hentet. Forsøk igjen.").nel(),
                ).left()
            }

            return@transaction settPrisendringPaVent(
                id,
                prisendring.totrinnskontroll,
                navIdent,
                begrunnelse,
            ).mapLeft(EnkeltplassBeslutningError::Valideringsfeil)
        }

        val okonomi = enkeltplass.okonomi
            ?: return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                FieldError.of("Økonomi har ikke blitt sendt til godkjenning").nel(),
            ).left()

        if (okonomi.id != forventetTotrinnskontrollId) {
            return@transaction EnkeltplassBeslutningError.Valideringsfeil(
                FieldError.of("Grunnlaget har endret seg siden det ble hentet. Forsøk igjen.").nel(),
            ).left()
        }

        settOkonomiPaVent(id, okonomi, navIdent, begrunnelse).mapLeft(EnkeltplassBeslutningError::Valideringsfeil)
    }

    private fun QueryContext.kanBeslutteEnkeltplass(enkeltplass: Enkeltplass, navIdent: NavIdent): Boolean {
        val ansatt = repository.navAnsatt.getOrError(navIdent)
        val kontekst = OkonomiBeslutningContext(
            GjennomforingType.ENKELTPLASS,
            nonEmptySetOf(enkeltplass.gjennomforing.ansvarligEnhet.enhetsnummer),
        )
        return OkonomiAuthorization.erOkonomiBeslutter(ansatt, kontekst)
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker? {
        val deltakelser = repository.deltaker.getByGjennomforing(gjennomforingId)
        if (deltakelser.size > 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet kun én)")
        }
        return deltakelser.firstOrNull()
    }

    private fun QueryContext.upsert(upsert: UpsertArenaEnkeltplass): GjennomforingEnkeltplass {
        val tiltakstype = tiltakstyper.getByTiltakskode(upsert.tiltakskode)

        val prismodellId = upsertPrismodell(upsert.id, upsert.prismodell)
        val dbo = UpsertGjennomforing(
            type = GjennomforingType.ENKELTPLASS,
            id = upsert.id,
            tiltakskode = tiltakstype.tiltakskode,
            arrangorId = upsert.arrangorId,
            navn = upsert.navn ?: tiltakstype.navn,
            startDato = upsert.startDato,
            sluttDato = upsert.sluttDato,
            status = upsert.status.type,
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
        commands.gjennomforing.upsert(dbo)

        return repository.gjennomforing.getGjennomforingEnkeltplassOrError(dbo.id)
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

        commands.gjennomforing.setFreeTextSearch(gjennomforing.id, fts)
    }

    private fun QueryContext.endrePrismodell(
        gjennomforingId: UUID,
        prisinformasjon: UpsertEnkeltplass.Prismodell,
        behandling: TotrinnskontrollBehandling,
    ): Either<Nothing, Enkeltplass> {
        upsertPrismodell(gjennomforingId, prisinformasjon)

        val oppdatert = repository.gjennomforing.getGjennomforingEnkeltplassOrError(gjennomforingId)
        publishTiltaksgjennomforingV2ToKafka(oppdatert)

        val okonomi = Totrinnskontroll.opprett(
            id = behandling.id,
            entityId = gjennomforingId,
            type = TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            behandletAv = behandling.behandletAv,
            behandletBegrunnelse = behandling.begrunnelse,
        )
        queries.totrinnskontroll.upsert(okonomi)
        outbox.publish(okonomi)

        return logEndring("Pris og betalingsbetingelser endret", gjennomforingId, behandling.behandletAv).right()
    }

    private fun QueryContext.haandterAvsluttendeStatus(
        enkeltplass: Enkeltplass,
        status: DeltakerStatusType,
    ) {
        when (status) {
            DeltakerStatusType.DELTAR,
            DeltakerStatusType.FULLFORT,
            DeltakerStatusType.HAR_SLUTTET,
            DeltakerStatusType.KLADD,
            DeltakerStatusType.PABEGYNT_REGISTRERING,
            DeltakerStatusType.SOKT_INN,
            DeltakerStatusType.UTKAST_TIL_PAMELDING,
            DeltakerStatusType.VENTELISTE,
            DeltakerStatusType.VENTER_PA_OPPSTART,
            DeltakerStatusType.VURDERES,
            DeltakerStatusType.AVBRUTT,
            -> return

            DeltakerStatusType.IKKE_AKTUELL,
            DeltakerStatusType.FEILREGISTRERT,
            DeltakerStatusType.AVBRUTT_UTKAST,
            -> {
                enkeltplass.okonomi
                    ?.takeIf { it.kanBesluttes() }
                    ?.let {
                        it.returner(Tiltaksadministrasjon).fold(
                            { throw IllegalStateException("Systemet klarte ikke returnere enkeltplass okonomi") },
                            { queries.totrinnskontroll.upsert(it) },
                        )
                    }

                enkeltplass.prisendring
                    ?.takeIf { it.totrinnskontroll.kanBesluttes() }
                    ?.let {
                        it.totrinnskontroll.returner(Tiltaksadministrasjon).fold(
                            { throw IllegalStateException("Systemet klarte ikke returnere enkeltplass prisendring") },
                            { queries.totrinnskontroll.upsert(it) },
                        )
                    }
            }
        }
    }

    private fun QueryContext.slettEksisterendePrisendring(prisendring: Totrinnskontroll) {
        val returnert = prisendring.returner(Tiltaksadministrasjon).getOrElse {
            error("Klarte ikke returnere prisendring")
        }
        queries.totrinnskontroll.upsert(returnert)
        outbox.publish(returnert)

        commands.enkeltplassPrisendring.getByGjennomforingId(prisendring.entityId)?.let { pending ->
            commands.enkeltplassPrisendring.deleteByTotrinnskontrollId(pending.totrinnskontrollId)
            repository.prismodell.deletePrismodell(pending.prismodellId)
        }
    }

    private fun QueryContext.upsertPrisendring(
        gjennomforingId: UUID,
        prisinformasjon: UpsertEnkeltplass.Prismodell,
        behandling: TotrinnskontrollBehandling,
    ) {
        val prismodellId = UUID.randomUUID()
        repository.prismodell.upsert(toPrismodell(prismodellId, prisinformasjon))

        val prisendring = Totrinnskontroll.opprett(
            id = behandling.id,
            entityId = gjennomforingId,
            type = TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
            behandletAv = behandling.behandletAv,
            behandletBegrunnelse = behandling.begrunnelse,
        )
        queries.totrinnskontroll.upsert(prisendring)
        outbox.publish(prisendring)

        commands.enkeltplassPrisendring.insert(
            EnkeltplassPrisendringRef(
                totrinnskontrollId = behandling.id,
                gjennomforingId = gjennomforingId,
                prismodellId = prismodellId,
            ),
        )
    }

    private fun QueryContext.getEnkeltplass(id: UUID): Enkeltplass? {
        return when (val gjennomforing = repository.gjennomforing.getGjennomforing(id)) {
            null -> null

            !is GjennomforingEnkeltplass -> error("Gjennomføring med id=$id er ikke en enkeltplass")

            else -> {
                val okonomi = queries.totrinnskontroll.get(id, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
                val prisendring = commands.enkeltplassPrisendring.getByGjennomforingId(id)?.let {
                    val prismodell = repository.prismodell.getOrError(it.prismodellId)
                    val totrinnskontroll = queries.totrinnskontroll.getById(it.totrinnskontrollId)
                    Enkeltplass.Prisendring(prismodell, totrinnskontroll)
                }
                Enkeltplass(gjennomforing, okonomi, prisendring)
            }
        }
    }

    private fun QueryContext.getEnkeltplassOrError(id: UUID): Enkeltplass {
        return checkNotNull(getEnkeltplass(id))
    }

    private fun QueryContext.getAndAcquireLock(id: UUID): Enkeltplass {
        commands.gjennomforing.aquireLock(id)
        return getEnkeltplassOrError(id)
    }

    private fun QueryContext.logEndring(
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
        prismodell: UpsertEnkeltplass.Prismodell,
    ): UUID {
        val prismodellId = queries.gjennomforing.getPrismodell(gjennomforingId)?.id ?: UUID.randomUUID()
        repository.prismodell.upsert(toPrismodell(prismodellId, prismodell))
        return prismodellId
    }

    private fun toPrismodell(id: UUID, prismodell: UpsertEnkeltplass.Prismodell): Prismodell = when (prismodell) {
        is UpsertEnkeltplass.Prismodell.Anskaffelse -> Prismodell.AnskaffetEnkeltplass(
            id = id,
            valuta = Valuta.NOK,
            totalbelop = prismodell.totalbelop.NOK,
        )

        is UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering -> Prismodell.TilskuddTilOpplaering(
            id = id,
            valuta = Valuta.NOK,
            tilleggsopplysninger = prismodell.tilleggsopplysninger,
            tilskudd = prismodell.tilskudd.mapValues { it.value.NOK },
        )

        is UpsertEnkeltplass.Prismodell.IngenKostnader -> Prismodell.IngenKostnader(
            id = id,
            valuta = Valuta.NOK,
            tilleggsopplysninger = prismodell.tilleggsopplysninger,
            aarsak = prismodell.aarsak,
        )
    }

    private fun QueryContext.upsertKategorisering(
        id: UUID,
        tiltakskode: Tiltakskode,
        kategorisering: OpplaringKategoriseringRequest?,
    ) {
        val kurstyper = queries.opplaering.getKurstyper()
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
        val opplaringKategorisering = OpplaringKategorisering(
            kurstype = kurstypeId,
            bransje = kategorisering?.bransjeId,
            forerkort = kategorisering?.forerkort?.toSet() ?: emptySet(),
            sertifiseringer = kategorisering?.sertifiseringer ?: emptySet(),
            utdanningslop = kategorisering?.utdanningsprogramId?.let { programId ->
                Utdanningslop(
                    utdanningsprogram = programId,
                    utdanninger = kategorisering.larefag?.toSet() ?: emptySet(),
                )
            },
        )
        queries.opplaering.upsert(id, opplaringKategorisering)
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingEnkeltplass) {
        outbox.publish(gjennomforing.id, TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing))
    }

    private fun QueryContext.settOkonomiTilGodkjenning(
        id: UUID,
        behandling: TotrinnskontrollBehandling,
    ): Enkeltplass {
        val totrinnskontroll = Totrinnskontroll.opprett(
            id = behandling.id,
            entityId = id,
            type = TotrinnskontrollType.ENKELTPLASS_OKONOMI,
            behandletAv = behandling.behandletAv,
            behandletBegrunnelse = behandling.begrunnelse,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        outbox.publish(totrinnskontroll)
        return logEndring("Deltaker søkt inn", id, behandling.behandletAv)
    }

    private fun QueryContext.settOkonomiGodkjent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
    ): Validated<Enkeltplass> {
        return okonomi.godkjenn(agent).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
            logEndring("Enkeltplass ble godkjent", id, agent)
        }
    }

    private fun QueryContext.settOkonomiPaVent(
        id: UUID,
        okonomi: Totrinnskontroll,
        agent: Agent,
        begrunnelse: String?,
    ): Validated<Enkeltplass> {
        return okonomi.settPaVent(agent, begrunnelse).mapLeft { it.toFieldErrors() }.map { paVent ->
            queries.totrinnskontroll.upsert(paVent)
            outbox.publish(paVent)
            logEndring("Godkjenning ble satt på vent", id, agent)
        }
    }

    private fun QueryContext.godkjennPrisendring(
        gjennomforingId: UUID,
        prisendring: Totrinnskontroll,
        agent: Agent,
    ): Validated<Enkeltplass> {
        return prisendring.godkjenn(agent).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)

            val pending = requireNotNull(commands.enkeltplassPrisendring.getByGjennomforingId(gjennomforingId)) {
                "Fant ikke prisendring for gjennomforing $gjennomforingId"
            }
            val gammelPrismodellId = queries.gjennomforing.getPrismodell(gjennomforingId)?.id
            commands.gjennomforing.setPrismodellId(gjennomforingId, pending.prismodellId)
            gammelPrismodellId?.let { repository.prismodell.deletePrismodell(it) }
            commands.enkeltplassPrisendring.deleteByTotrinnskontrollId(pending.totrinnskontrollId)

            val oppdatert = repository.gjennomforing.getGjennomforingEnkeltplassOrError(gjennomforingId)
            publishTiltaksgjennomforingV2ToKafka(oppdatert)
            logEndring("Prisendring ble godkjent", gjennomforingId, agent)
        }
    }

    private fun QueryContext.settPrisendringPaVent(
        gjennomforingId: UUID,
        prisendring: Totrinnskontroll,
        agent: Agent,
        begrunnelse: String?,
    ): Validated<Enkeltplass> {
        return prisendring.settPaVent(agent, begrunnelse).mapLeft { it.toFieldErrors() }.map { paVent ->
            queries.totrinnskontroll.upsert(paVent)
            outbox.publish(paVent)
            logEndring("Prisendring ble satt på vent", gjennomforingId, agent)
        }
    }
}

private fun UpsertEnkeltplass.toUpsert(gjennomforing: GjennomforingEnkeltplass? = null) = UpsertArenaEnkeltplass(
    id = id,
    tiltakskode = tiltakskode,
    arrangorId = arrangorId,
    ansvarligEnhet = ansvarligEnhet,
    prismodell = prismodell,
    status = gjennomforing?.status ?: GjennomforingEnkeltplassStatus.UtkastTilPamelding,
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
): UpsertArenaEnkeltplass = UpsertArenaEnkeltplass(
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
    status = toGjennomforingEnkeltplassStatus(status.type),
    // TODO: nullable i stedet for default 100
    deltidsprosent = deltakelsesmengder.lastOrNull()?.deltakelsesprosent?.toDouble() ?: 100.0,
)

private fun toUpsertPrismodell(prismodell: Prismodell): UpsertEnkeltplass.Prismodell = when (prismodell) {
    is Prismodell.AnskaffetEnkeltplass -> UpsertEnkeltplass.Prismodell.Anskaffelse(prismodell.totalbelop.belop)

    is Prismodell.IngenKostnader -> UpsertEnkeltplass.Prismodell.IngenKostnader(
        prismodell.aarsak,
        prismodell.tilleggsopplysninger,
    )

    is Prismodell.TilskuddTilOpplaering -> UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering(
        prismodell.tilskudd.mapValues { it.value.belop },
        prismodell.tilleggsopplysninger,
    )

    is Prismodell.AnnenAvtaltPris,
    is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
    is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
    is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
    is Prismodell.FastSatsPerAvtaltPlassPerManed,
    is Prismodell.FastSatsPerBenyttetPlassPerManed,
    -> error("${prismodell.type} er ikke støttet for enkeltplasser")
}

/**
 * Statusen til en enkeltplass speiler statusen til deltakeren som er knyttet til gjennomføringen.
 */
private fun toGjennomforingEnkeltplassStatus(status: DeltakerStatusType): GjennomforingEnkeltplassStatus = when (status) {
    DeltakerStatusType.UTKAST_TIL_PAMELDING -> GjennomforingEnkeltplassStatus.UtkastTilPamelding

    DeltakerStatusType.SOKT_INN -> GjennomforingEnkeltplassStatus.SoktInn

    DeltakerStatusType.VENTER_PA_OPPSTART -> GjennomforingEnkeltplassStatus.VenterPaOppstart

    DeltakerStatusType.DELTAR -> GjennomforingEnkeltplassStatus.Deltar

    DeltakerStatusType.IKKE_AKTUELL -> GjennomforingEnkeltplassStatus.IkkeAktuell

    DeltakerStatusType.FULLFORT -> GjennomforingEnkeltplassStatus.Fullfort

    DeltakerStatusType.AVBRUTT -> GjennomforingEnkeltplassStatus.Avbrutt

    DeltakerStatusType.AVBRUTT_UTKAST -> GjennomforingEnkeltplassStatus.AvbruttUtkast

    DeltakerStatusType.FEILREGISTRERT -> GjennomforingEnkeltplassStatus.Feilregistrert

    DeltakerStatusType.KLADD,
    DeltakerStatusType.PABEGYNT_REGISTRERING,
    DeltakerStatusType.VURDERES,
    DeltakerStatusType.VENTELISTE,
    DeltakerStatusType.HAR_SLUTTET,
    -> error("Forventet ikke deltaker med status $status for enkeltplasser")
}

private fun harEnkeltplassEndringer(
    opprett: UpsertArenaEnkeltplass,
    gjennomforing: GjennomforingEnkeltplass,
): Boolean = opprett != UpsertArenaEnkeltplass(
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
