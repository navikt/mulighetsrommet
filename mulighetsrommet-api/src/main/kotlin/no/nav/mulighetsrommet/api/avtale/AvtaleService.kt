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
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator.ValidatePrismodellerContext
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AvtaleService(
    private val config: Config,
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    data class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    suspend fun create(
        request: OpprettAvtaleRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val createAvtaleContext = db.session {
            getValidatorCtx(
                request = request.detaljer,
                navEnheter = request.veilederinformasjon.navEnheter,
                previous = null,
            ).bind()
        }
        val avtaleDbo = AvtaleValidator.validateCreateAvtale(request, createAvtaleContext).bind()

        val createPrismodellerContext = ValidatePrismodellerContext(
            avtaletype = avtaleDbo.detaljerDbo.avtaletype,
            tiltakskode = request.detaljer.tiltakskode,
            tiltakstypeNavn = createAvtaleContext.tiltakstype.navn,
            avtaleStartDato = avtaleDbo.detaljerDbo.startDato,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
            bruktePrismodeller = setOf(),
        )
        val prismodeller = AvtaleValidator.validatePrismodeller(request.prismodeller, createPrismodellerContext).bind()

        db.transaction {
            prismodeller.forEach { queries.prismodell.upsert(it) }
            queries.avtale.create(avtaleDbo)

            dispatchNotificationToNewAdministrators(
                avtaleDbo.id,
                avtaleDbo.detaljerDbo.navn,
                avtaleDbo.detaljerDbo.administratorer,
                navIdent,
            )

            val dto = logEndring("Opprettet avtale", avtaleDbo.id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
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
                opphav = avtale.opphav,
                opsjonerRegistrert = avtale.opsjonerRegistrert,
                opsjonsmodell = avtale.opsjonsmodell,
                avtaletype = avtale.avtaletype,
                tiltakskode = avtale.tiltakstype.tiltakskode,
                gjennomforinger = gjennomforinger.map {
                    AvtaleValidator.Ctx.Gjennomforing(
                        arrangor = it.arrangor,
                        startDato = it.startDato,
                        utdanningslop = it.utdanningslop,
                        status = it.status.type,
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

            val dbo = AvtaleValidator
                .validateUpdateDetaljer(request, context)
                .bind()

            if (AvtaleDboMapper.fromAvtale(avtale) == dbo) {
                return@either avtale
            }

            queries.avtale.updateDetaljer(avtaleId, dbo)
            dispatchNotificationToNewAdministrators(avtaleId, dbo.navn, dbo.administratorer, navIdent)

            val dto = logEndring("Detaljer oppdatert", avtaleId, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    fun upsertPersonvern(
        avtaleId: UUID,
        request: PersonvernRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val previous = getOrError(avtaleId)
            val dbo = request.toDbo()

            queries.avtale.updatePersonvern(previous.id, dbo)

            val dto = logEndring("Personvern oppdatert", previous.id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
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
            val dbo = VeilederinformasjonDbo(
                redaksjoneltInnhold = RedaksjoneltInnholdDbo(
                    beskrivelse = request.beskrivelse,
                    faneinnhold = request.faneinnhold,
                ),
                navEnheter = validatedNavEnheter,
            )

            queries.avtale.updateVeilederinfo(previous.id, dbo)

            val dto = logEndring("Veilederinformasjon oppdatert", previous.id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    fun upsertPrismodell(
        id: UUID,
        request: List<PrismodellRequest>,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(id)
        val gjennomforinger = queries.gjennomforing.getByAvtale(id)
        val context = ValidatePrismodellerContext(
            avtaletype = avtale.avtaletype,
            tiltakskode = avtale.tiltakstype.tiltakskode,
            tiltakstypeNavn = avtale.tiltakstype.navn,
            avtaleStartDato = avtale.startDato,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
            bruktePrismodeller = gjennomforinger.map { it.prismodell.id }.toSet(),
        )
        AvtaleValidator.validatePrismodeller(request, context).map { prismodeller ->
            prismodeller.forEach { prismodell ->
                queries.prismodell.upsert(prismodell)
                queries.avtale.upsertPrismodell(id, prismodell.id)
            }

            val prismodellerIds = prismodeller.map { it.id }.toSet()
            avtale.prismodeller.filter { it.id !in prismodellerIds }.forEach { prismodell ->
                queries.avtale.deletePrismodell(avtale.id, prismodell.id)
                queries.prismodell.deletePrismodell(prismodell.id)
            }

            val dto = logEndring("Prismodell oppdatert", id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    fun upsertRammedetaljer(
        id: UUID,
        request: RammedetaljerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> = db.transaction {
        val avtale = getOrError(id)

        RammedetaljerValidator.validateRammedetaljer(
            context = RammedetaljerValidator.Ctx(avtale.id, avtale.prismodeller),
            request,
        ).map { rammedetalerDbo ->
            queries.rammedetaljer.upsert(rammedetalerDbo)
            logEndring("Rammedetaljer oppdatert", id, navIdent)
            Unit
        }
    }

    fun deleteRammedetaljer(
        id: UUID,
        navIdent: NavIdent,
    ): Avtale = db.transaction {
        queries.rammedetaljer.delete(id)
        logEndring("Rammedetaljer slettet", id, navIdent)
    }

    fun get(id: UUID): Avtale? = db.session {
        queries.avtale.get(id)
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
                is AvtaleStatus.Avbrutt -> error { FieldError.root("Avtalen er allerede avbrutt") }
                is AvtaleStatus.Avsluttet -> error { FieldError.root("Avtalen er allerede avsluttet") }
            }

            val antallAktiveGjennomforinger = queries.gjennomforing.getByAvtale(id).count {
                it.status.type == GjennomforingStatusType.GJENNOMFORES
            }
            validate(antallAktiveGjennomforinger == 0) {
                val message = listOf(
                    "Avtalen har",
                    antallAktiveGjennomforinger,
                    if (antallAktiveGjennomforinger > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                    "og kan derfor ikke avbrytes",
                ).joinToString(" ")
                FieldError.root(message)
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
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val avtale = getOrError(avtaleId)

            val dbo = AvtaleValidator.validateOpprettOpsjonLoggRequest(
                AvtaleValidator.ValidateOpprettOpsjonContext(avtale, navIdent),
                request,
            ).bind()

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
    ): Unit = db.transaction {
        queries.avtale.frikobleKontaktpersonFraAvtale(kontaktpersonId = kontaktpersonId, avtaleId = avtaleId)

        logEndring("Kontaktperson ble fjernet fra avtalen", avtaleId, navIdent)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.AVTALE, id)
    }

    private fun schedulePublishGjennomforingerForAvtale(dto: Avtale) {
        gjennomforingPublisher.schedule(
            input = InitialLoadGjennomforinger.Input(avtaleId = dto.id),
            id = dto.id,
            startTime = Instant.now().plus(30, ChronoUnit.SECONDS),
        )
    }

    private suspend fun QueryContext.getValidatorCtx(
        request: DetaljerRequest,
        navEnheter: List<NavEnhetNummer>,
        previous: AvtaleValidator.Ctx.Avtale?,
    ): Either<List<FieldError>, AvtaleValidator.Ctx> = either {
        val tiltakstype = queries.tiltakstype.getByTiltakskode(request.tiltakskode)
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
        val navEnheter = navEnheter.mapNotNull { queries.enhet.get(it)?.toDto() }

        val arrangor = request.arrangor?.let {
            val (arrangor, underenheter) = syncArrangorerFromBrreg(it.hovedenhet, it.underenheter).bind()
            arrangor.copy(underenheter = underenheter)
        }

        val systembestemtPrismodell = queries.prismodell.getBySystemId(request.tiltakskode.name)

        AvtaleValidator.Ctx(
            previous = previous,
            arrangor = arrangor,
            administratorer = administratorer,
            tiltakstype = AvtaleValidator.Ctx.Tiltakstype(
                navn = tiltakstype.navn,
                id = tiltakstype.id,
            ),
            navEnheter = navEnheter,
            systembestemtPrismodell = systembestemtPrismodell?.id,
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
        return queries.avtale.getOrError(id)
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        avtaleId: UUID,
        avtalenavn: String,
        administratorer: List<NavIdent>,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = get(avtaleId)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify =
            (administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på avtalen \"${avtalenavn}\"",
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
        val dto = queries.avtale.getOrError(avtaleId)
        queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation,
            endretAv,
            avtaleId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
        return dto
    }

    private suspend fun syncArrangorerFromBrreg(
        orgnr: Organisasjonsnummer,
        underenheterOrgnummere: List<Organisasjonsnummer>,
    ): Either<List<FieldError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(orgnr).bind()
        val underenheter = underenheterOrgnummere.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<List<FieldError>, ArrangorDto> = arrangorService
        .getArrangorOrSyncFromBrreg(orgnr)
        .mapLeft {
            FieldError.of(
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                OpprettAvtaleRequest::detaljer,
                DetaljerRequest::arrangor,
                DetaljerRequest.Arrangor::hovedenhet,
            ).nel()
        }

    fun handlinger(avtale: Avtale, ansatt: NavAnsatt): Set<AvtaleHandling> {
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
                when (avtale.status) {
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
                avtale.opsjonsmodell.opsjonMaksVarighet != null
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
                -> ansatt.hasGenerellRolle(Rolle.AVTALER_SKRIV)
            }
        }
    }
}
