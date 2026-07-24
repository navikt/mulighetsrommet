package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorIfMissing
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.navenhet.toDto
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator.ValidatePrismodellerContext
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.tiltak.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.validation.validation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AvtaleService(
    private val config: Config,
    private val db: ApiDatabase,
    private val syncArrangor: SyncArrangorUseCase,
    private val tiltakstypeService: TiltakstypeService,
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    data class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    suspend fun create(
        request: OpprettAvtaleRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        if (tiltakstypeService.erUtfaset(request.detaljer.tiltakskode)) {
            raise(FieldError.of("Avtaler kan ikke opprettes for denne tiltakstypen fordi den er utfaset").nel())
        } else if (!request.detaljer.tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_AVTALER)) {
            raise(FieldError.of("Avtaler kan ikke opprettes for denne tiltakstypen").nel())
        }

        val createAvtaleContext = db.session {
            getValidatorCtx(
                request = request.detaljer,
                navEnheter = request.veilederinformasjon.navEnheter,
                previous = null,
            ).bind()
        }
        val detaljer = AvtaleValidator.validateCreateAvtale(request, createAvtaleContext).bind()

        val systembestemtPrismodell = if (request.detaljer.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            db.session { queries.prismodell.getBySystemId(request.detaljer.tiltakskode.name) }
        } else {
            null
        }
        val createPrismodellerContext = ValidatePrismodellerContext(
            avtaletype = detaljer.avtaletype,
            tiltakskode = request.detaljer.tiltakskode,
            tiltakstypeNavn = createAvtaleContext.tiltakstype.navn,
            avtaleStartDato = detaljer.startDato,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
            bruktePrismodeller = setOf(),
            systembestemtPrismodell = systembestemtPrismodell,
        )
        val prismodeller = AvtaleValidator.validatePrismodeller(request.prismodeller, createPrismodellerContext).bind()

        db.transaction {
            val avtale = request.toAvtale(detaljer, prismodeller)
            repository.avtale.save(avtale)

            dispatchNotificationToNewAdministrators(
                forrige = null,
                neste = avtale,
                endretAv = navIdent,
            )

            logEndring("Opprettet avtale", avtale.id, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    suspend fun upsertDetaljer(
        avtaleId: UUID,
        request: DetaljerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val avtale = getOrError(avtaleId)

            val gjennomforinger = queries.gjennomforing.getByAvtale(avtaleId)
            val previous = AvtaleValidator.Ctx.Avtale(
                status = avtale.status.type,
                opsjoner = avtale.opsjoner,
                avtaletype = avtale.avtaletype,
                tiltakskode = avtale.tiltakskode,
                gjennomforinger = gjennomforinger.map {
                    AvtaleValidator.Ctx.Gjennomforing(
                        arrangor = it.arrangor,
                        startDato = it.startDato,
                        utdanningslop = queries.opplaering.get(it.id)?.utdanningslop,
                        status = it.status,
                        prismodellId = it.prismodell.id,
                    )
                },
                prismodeller = avtale.prismodeller,
            )
            val context = getValidatorCtx(
                request = request,
                navEnheter = listOf(),
                previous = previous,
            ).bind()

            val detaljer = AvtaleValidator
                .validateUpdateDetaljer(request, context)
                .bind()

            val oppdatertAvtale = avtale.toUpdatedAvtale(detaljer)
            if (avtale == oppdatertAvtale) {
                return@either avtale
            }

            repository.avtale.save(oppdatertAvtale)
            dispatchNotificationToNewAdministrators(
                forrige = avtale,
                neste = oppdatertAvtale,
                endretAv = navIdent,
            )

            logEndring("Detaljer oppdatert", avtaleId, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    fun upsertPersonvern(
        avtaleId: UUID,
        request: PersonvernRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        if ((request.annetChecked ?: false) && request.annetBeskrivelse.isNullOrBlank()) {
            raise(listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")))
        }
        if ((request.annetBeskrivelse?.length ?: 0) > 300) {
            raise(listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse kan maks være 300 tegn")))
        }
        db.transaction {
            val previous = getOrError(avtaleId)
            val personvern = request.toAvtalePersonvern()
            repository.avtale.save(previous.copy(personvern = personvern))

            logEndring("Personvern oppdatert", previous.id, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    fun upsertVeilederinfo(
        avtaleId: UUID,
        request: VeilederinfoRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val previous = getOrError(avtaleId)

            val navEnheter = request.navEnheter.mapNotNull {
                queries.enhet.get(it)?.toDto()
            }
            val validatedNavEnheter = AvtaleValidator.validateNavEnheter(navEnheter).bind()
            val veilederinfo = Avtale.VeilederInfo(
                beskrivelse = request.beskrivelse,
                faneinnhold = request.faneinnhold,
                navEnheter = validatedNavEnheter,
            )

            repository.avtale.save(previous.copy(veilederinfo = veilederinfo))

            logEndring("Veilederinformasjon oppdatert", previous.id, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    fun upsertPrismodell(
        id: UUID,
        request: List<PrismodellRequest>,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(id)

        if (avtale.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            return FieldError.of("Prismodell kan ikke endres for forhåndsgodkjente avtaler").nel().left()
        }

        val gjennomforinger = queries.gjennomforing.getByAvtale(id)
        val tiltakstype = queries.tiltakstype.getByTiltakskode(avtale.tiltakskode)
        // TODO: forenkle context - f.eks. hele tiltakstype + avtale i context?
        val context = ValidatePrismodellerContext(
            avtaletype = avtale.avtaletype,
            tiltakskode = avtale.tiltakskode,
            tiltakstypeNavn = tiltakstype.navn,
            avtaleStartDato = avtale.startDato,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
            bruktePrismodeller = gjennomforinger.map { it.prismodell.id }.toSet(),
            systembestemtPrismodell = null,
        )

        AvtaleValidator.validatePrismodeller(request, context).map { prismodeller ->
            repository.avtale.save(avtale.copy(prismodeller = prismodeller))

            logEndring("Prismodell oppdatert", id, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    fun upsertRammedetaljer(
        id: UUID,
        request: RammedetaljerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(id)
        if (request.totalRamme == null && request.utbetaltArena == null) {
            return@transaction deleteRammedetaljer(id, navIdent).right()
        }
        RammedetaljerValidator.validateRammedetaljer(
            context = RammedetaljerValidator.Ctx(avtale.id, avtale.prismodeller),
            request,
        ).map { rammedetalerDbo ->
            queries.rammedetaljer.upsert(rammedetalerDbo)
            logEndring("Rammedetaljer oppdatert", id, navIdent)
        }
    }

    fun deleteRammedetaljer(
        id: UUID,
        navIdent: NavIdent,
    ): Avtale = db.transaction {
        queries.rammedetaljer.delete(id)
        logEndring("Rammedetaljer slettet", id, navIdent)
    }

    fun avsluttAvtale(id: UUID, avsluttetTidspunkt: LocalDateTime, endretAv: Agent) = db.transaction {
        val avtale = getOrError(id)

        check(avtale.status == AvtaleStatus.Aktiv) {
            "Avtalen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = avtale.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Avtalen kan ikke avsluttes før sluttdato"
        }

        queries.avtale.setStatus(id, AvtaleStatusType.AVSLUTTET, null, null, null)

        logEndring("Avtalen ble avsluttet", id, endretAv)
    }

    fun avbrytAvtale(
        id: UUID,
        avbruttAv: NavIdent,
        tidspunkt: LocalDateTime,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytAvtaleAarsak>,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        validation {
            val avtale = getOrError(id)
            when (avtale.status) {
                is AvtaleStatus.Utkast, is AvtaleStatus.Aktiv -> Unit
                is AvtaleStatus.Avbrutt -> error { FieldError.of("Avtalen er allerede avbrutt") }
                is AvtaleStatus.Avsluttet -> error { FieldError.of("Avtalen er allerede avsluttet") }
            }

            val antallAktiveGjennomforinger = queries.gjennomforing.getByAvtale(id).count {
                it.status == GjennomforingStatusType.GJENNOMFORES
            }
            validate(antallAktiveGjennomforinger == 0) {
                val message = listOf(
                    "Avtalen har",
                    antallAktiveGjennomforinger,
                    if (antallAktiveGjennomforinger > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                    "og kan derfor ikke avbrytes",
                ).joinToString(" ")
                FieldError.of(message)
            }
        }.map {
            queries.avtale.setStatus(
                id = id,
                status = AvtaleStatusType.AVBRUTT,
                tidspunkt = tidspunkt,
                aarsaker = aarsakerOgForklaring.aarsaker,
                forklaring = aarsakerOgForklaring.forklaring,
            )

            logEndring("Avtalen ble avbrutt", id, avbruttAv)
        }
    }

    fun registrerOpsjon(
        avtaleId: UUID,
        request: OpprettOpsjonLoggRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(avtaleId)

        AvtaleValidator.validateOpprettOpsjonLoggRequest(
            AvtaleValidator.ValidateOpprettOpsjonContext(avtale, navIdent),
            request,
        ).map { dbo ->
            queries.opsjoner.insert(dbo)

            if (dbo.sluttDato != null) {
                updateAvtaleVarighet(avtaleId, dbo.sluttDato, today)
            }

            val operation = when (request.type) {
                OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
                OpprettOpsjonLoggRequest.Type.ETT_AAR,
                -> "Opsjon registrert"

                OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            }
            logEndring(operation, avtaleId, navIdent)
        }
    }

    fun slettOpsjon(
        avtaleId: UUID,
        opsjonId: UUID,
        slettesAv: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<FieldError, Avtale> = db.transaction {
        val opsjoner = queries.opsjoner.getByAvtaleId(avtaleId)

        val sisteOpsjon = opsjoner.firstOrNull()
        if (sisteOpsjon == null || sisteOpsjon.id != opsjonId) {
            return FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen").left()
        }

        if (sisteOpsjon.status == OpsjonLoggStatus.OPSJON_UTLOST) {
            val nySluttDato = sisteOpsjon.forrigeSluttDato
            updateAvtaleVarighet(avtaleId, nySluttDato, today)
        }

        queries.opsjoner.delete(opsjonId)

        logEndring("Opsjon slettet", avtaleId, slettesAv).right()
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
        navIdent: NavIdent,
    ): Avtale = db.transaction {
        queries.avtale.frikobleKontaktpersonFraAvtale(kontaktpersonId = kontaktpersonId, avtaleId = avtaleId)

        logEndring("Kontaktperson ble fjernet fra avtalen", avtaleId, navIdent)
    }

    private fun schedulePublishGjennomforingerForAvtale(avtale: Avtale) {
        gjennomforingPublisher.schedule(
            input = InitialLoadGjennomforinger.Input(avtaleId = avtale.id),
            id = avtale.id,
            startTime = Instant.now().plus(30, ChronoUnit.SECONDS),
        )
    }

    private suspend fun QueryContext.getValidatorCtx(
        request: DetaljerRequest,
        navEnheter: List<NavEnhetNummer>,
        previous: AvtaleValidator.Ctx.Avtale?,
    ): Either<List<FieldError>, AvtaleValidator.Ctx> = either {
        val tiltakstype = queries.tiltakstype.getByTiltakskode(request.tiltakskode)
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.get(it) }
        val navEnheter = navEnheter.mapNotNull { queries.enhet.get(it)?.toDto() }

        val arrangor = request.arrangor?.let {
            val (hovedenhet, underenheter) = syncArrangorerFromBrreg(it.hovedenhet, it.underenheter).bind()
            AvtaleValidator.Ctx.AvtaleArrangor(hovedenhet, underenheter)
        }

        val kategorisering = AvtaleValidator.Ctx.Kategorisering(
            kurstyper = queries.opplaering.getKurstyper(),
            bransjer = queries.opplaering.getBransjer(),
            forerkort = queries.opplaering.getForerkortKlasser(),
            innholdElementer = queries.opplaering.getInnholdElementer(),
            utdanninger = queries.opplaering.getUtdanningslop(),
        )

        AvtaleValidator.Ctx(
            previous = previous,
            arrangor = arrangor,
            administratorer = administratorer,
            tiltakstype = AvtaleValidator.Ctx.Tiltakstype(
                navn = tiltakstype.navn,
                tiltakskode = tiltakstype.tiltakskode,
            ),
            navEnheter = navEnheter,
            kategorisering = kategorisering,
        )
    }

    private fun QueryContext.updateAvtaleVarighet(avtaleId: UUID, nySluttDato: LocalDate, today: LocalDate) {
        queries.avtale.setSluttDato(avtaleId, nySluttDato)

        val currentStatus = getOrError(avtaleId).status.type
        val newStatus = when (currentStatus) {
            AvtaleStatusType.UTKAST, AvtaleStatusType.AVBRUTT -> currentStatus

            AvtaleStatusType.AKTIV, AvtaleStatusType.AVSLUTTET -> if (!nySluttDato.isBefore(today)) {
                AvtaleStatusType.AKTIV
            } else {
                AvtaleStatusType.AVSLUTTET
            }
        }
        if (newStatus != currentStatus) {
            queries.avtale.setStatus(avtaleId, newStatus, null, null, null)
        }
    }

    private fun QueryContext.getOrError(id: UUID): Avtale {
        return repository.avtale.getOrError(id)
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        forrige: Avtale?,
        neste: Avtale,
        endretAv: NavIdent,
    ) {
        val administratorsToNotify = (neste.administratorer - forrige?.administratorer.orEmpty() - endretAv)
            .toNonEmptyListOrNull()
            ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på avtalen \"${neste.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        avtaleId: UUID,
        endretAv: Agent,
    ): Avtale {
        val avtale = repository.avtale.getOrError(avtaleId)
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.AVTALE,
            operation,
            endretAv,
            avtaleId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(avtale)
        }
        return avtale
    }

    private suspend fun syncArrangorerFromBrreg(
        orgnr: Organisasjonsnummer,
        underenheterOrgnummere: List<Organisasjonsnummer>,
    ): Either<List<FieldError>, Pair<Arrangor, List<Arrangor>>> = either {
        val arrangor = syncArrangorFromBrreg(orgnr).bind()
        val underenheter = underenheterOrgnummere.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<List<FieldError>, Arrangor> = syncArrangor.execute(SyncArrangorIfMissing(orgnr)).mapLeft {
        FieldError.of(
            "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
            OpprettAvtaleRequest::detaljer,
            DetaljerRequest::arrangor,
            DetaljerRequest.Arrangor::hovedenhet,
        ).nel()
    }

    fun handlinger(avtaleId: UUID, ansatt: NavAnsatt): Set<AvtaleHandling> {
        val avtale = db.session { repository.avtale.get(avtaleId) } ?: return emptySet()
        return setOfNotNull(
            AvtaleHandling.AVBRYT.takeIf {
                when (avtale.status) {
                    AvtaleStatus.Utkast,
                    AvtaleStatus.Aktiv,
                    -> true

                    is AvtaleStatus.Avbrutt,
                    AvtaleStatus.Avsluttet,
                    -> false
                }
            },
            AvtaleHandling.OPPRETT_GJENNOMFORING.takeIf {
                !tiltakstypeService.erUtfaset(avtale.tiltakskode) && when (avtale.status) {
                    AvtaleStatus.Aktiv -> true

                    is AvtaleStatus.Avbrutt,
                    AvtaleStatus.Avsluttet,
                    AvtaleStatus.Utkast,
                    -> false
                }
            },
            AvtaleHandling.OPPDATER_PRIS.takeIf {
                avtale.prismodeller.any { it.type != PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK }
            },
            AvtaleHandling.OPPDATER_RAMMEDETALJER.takeIf {
                avtale.prismodeller.any { it.type != PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK }
            },
            AvtaleHandling.REGISTRER_OPSJON.takeIf {
                avtale.opsjoner.modell.opsjonMaksVarighet != null
            },
            AvtaleHandling.DUPLISER,
            AvtaleHandling.REDIGER,
        )
            .filter {
                tilgangTilHandling(it, ansatt)
            }
            .toSet()
    }

    companion object {
        fun tilgangTilHandling(handling: AvtaleHandling, ansatt: NavAnsatt): Boolean {
            return when (handling) {
                AvtaleHandling.OPPRETT_GJENNOMFORING -> ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)

                AvtaleHandling.AVBRYT,
                AvtaleHandling.OPPDATER_PRIS,
                AvtaleHandling.OPPDATER_RAMMEDETALJER,
                AvtaleHandling.REGISTRER_OPSJON,
                AvtaleHandling.DUPLISER,
                AvtaleHandling.REDIGER,
                AvtaleHandling.OPPRETT,
                -> ansatt.hasGenerellRolle(Rolle.AVTALER_SKRIV)
            }
        }
    }
}

private fun OpprettAvtaleRequest.toAvtale(
    detaljer: AvtaleValidator.ValidatedDetaljer,
    prismodeller: List<Prismodell>,
): Avtale = Avtale(
    id = id,
    tiltakskode = detaljer.tiltakskode,
    navn = detaljer.navn,
    avtalenummer = null,
    sakarkivNummer = detaljer.sakarkivNummer,
    arrangor = detaljer.arrangor,
    startDato = detaljer.startDato,
    sluttDato = detaljer.sluttDato,
    avtaletype = detaljer.avtaletype,
    status = detaljer.status.toAvtaleStatus(),
    administratorer = detaljer.administratorer.toSet(),
    veilederinfo = Avtale.VeilederInfo(
        beskrivelse = veilederinformasjon.beskrivelse,
        faneinnhold = veilederinformasjon.faneinnhold,
        navEnheter = veilederinformasjon.navEnheter.toSet(),
    ),
    personvern = personvern.toAvtalePersonvern(),
    opplaring = detaljer.opplaring,
    opsjoner = Avtale.Opsjoner(detaljer.opsjonsmodell, emptyList()),
    prismodeller = prismodeller,
)

private fun Avtale.toUpdatedAvtale(detaljer: AvtaleValidator.ValidatedDetaljer): Avtale = Avtale(
    id = id,
    tiltakskode = tiltakskode,
    navn = detaljer.navn,
    avtalenummer = avtalenummer,
    sakarkivNummer = detaljer.sakarkivNummer,
    arrangor = detaljer.arrangor,
    startDato = detaljer.startDato,
    sluttDato = detaljer.sluttDato,
    avtaletype = detaljer.avtaletype,
    status = detaljer.status.toAvtaleStatus(status),
    administratorer = detaljer.administratorer.toSet(),
    veilederinfo = veilederinfo,
    personvern = personvern,
    opplaring = detaljer.opplaring,
    opsjoner = Avtale.Opsjoner(detaljer.opsjonsmodell, opsjoner.registreringer),
    prismodeller = prismodeller,
)

private fun PersonvernRequest.toAvtalePersonvern(): Avtale.Personvern {
    val typer = buildSet {
        addAll(personopplysninger)
        if (annetChecked == true) {
            add(Personopplysning.Type.ANNET)
        }
    }
    return Avtale.Personvern(
        personopplysninger = typer,
        annetBeskrivelse = annetBeskrivelse?.takeIf { Personopplysning.Type.ANNET in typer },
        erBekreftet = personvernBekreftet,
    )
}

private fun AvtaleStatusType.toAvtaleStatus(previous: AvtaleStatus): AvtaleStatus = when (this) {
    AvtaleStatusType.AVBRUTT -> previous
    else -> toAvtaleStatus()
}

private fun AvtaleStatusType.toAvtaleStatus(): AvtaleStatus = when (this) {
    AvtaleStatusType.UTKAST -> AvtaleStatus.Utkast
    AvtaleStatusType.AKTIV -> AvtaleStatus.Aktiv
    AvtaleStatusType.AVSLUTTET -> AvtaleStatus.Avsluttet
    AvtaleStatusType.AVBRUTT -> error("Avbrutt status må opprettes med årsaker og tidspunkt")
}
