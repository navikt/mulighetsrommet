package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AvtaleService(
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    suspend fun create(
        request: OpprettAvtaleRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val ctx = getValidatorCtx(request.id, request.detaljer, request.veilederinformasjon.navEnheter, null).bind()

        val dbo = AvtaleValidator
            .validateCreateAvtale(
                request.copy(
                    veilederinformasjon = request.veilederinformasjon.copy(
                        navEnheter = sanitizeNavEnheter(
                            request.veilederinformasjon.navEnheter,
                        ),
                    ),
                ),
                ctx,
            )
            .bind()

        db.transaction {
            queries.avtale.upsert(dbo)

            dispatchNotificationToNewAdministrators(
                dbo.id,
                dbo.detaljerDbo.navn,
                dbo.detaljerDbo.administratorer,
                navIdent,
            )

            val dto = logEndring("Opprettet avtale", dbo.id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    suspend fun upsertDetaljer(
        avtaleId: UUID,
        request: DetaljerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(avtaleId) ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")
        val ctx = getValidatorCtx(avtaleId, request, null, previous).bind()

        val dbo = AvtaleValidator
            .validateUpdateDetaljer(request, ctx)
            .bind()

        if (AvtaleDboMapper.fromAvtale(previous) == dbo) {
            return@either previous
        }

        db.transaction {
            queries.avtale.updateDetaljer(avtaleId, dbo)
            dispatchNotificationToNewAdministrators(avtaleId, dbo.navn, dbo.administratorer, navIdent)

            val dto = logEndring("Detaljer oppdatert", avtaleId, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    suspend fun getValidatorCtx(
        avtaleId: UUID,
        request: DetaljerRequest,
        navEnheter: List<NavEnhetNummer>?,
        previous: Avtale?,
    ): Either<List<FieldError>, AvtaleValidator.Ctx> = either {
        db.session {
            val tiltakstype = queries.tiltakstype.getByTiltakskode(request.tiltakskode)
            val administratorer = request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
            val navEnheter = (navEnheter ?: emptyList()).mapNotNull { queries.enhet.get(it)?.toDto() }

            val arrangor = request.arrangor?.let {
                val (arrangor, underenheter) = syncArrangorerFromBrreg(
                    it.hovedenhet,
                    it.underenheter,
                ).bind()
                arrangor.copy(underenheter = underenheter)
            }

            val gjennomforinger = queries.gjennomforing.getByAvtale(avtaleId)

            AvtaleValidator.Ctx(
                previous = previous?.let {
                    AvtaleValidator.Ctx.Avtale(
                        status = it.status.type,
                        opphav = it.opphav,
                        opsjonerRegistrert = it.opsjonerRegistrert,
                        opsjonsmodell = it.opsjonsmodell,
                        avtaletype = it.avtaletype,
                        tiltakskode = it.tiltakstype.tiltakskode,
                        gjennomforinger = gjennomforinger.map {
                            AvtaleValidator.Ctx.Gjennomforing(
                                arrangor = it.arrangor,
                                startDato = it.startDato,
                                utdanningslop = it.utdanningslop,
                                status = it.status.type,
                            )
                        },
                        prismodell = it.prismodell,
                    )
                },
                arrangor = arrangor,
                administratorer = administratorer,
                tiltakstype = AvtaleValidator.Ctx.Tiltakstype(
                    navn = tiltakstype.navn,
                    id = tiltakstype.id,
                ),
                navEnheter = navEnheter,
            )
        }
    }

    fun upsertPersonvern(
        avtaleId: UUID,
        request: PersonvernRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(avtaleId)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val dbo = request.toDbo()

        db.transaction {
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
        val previous = get(avtaleId)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val navEnheter = db.session {
            request.navEnheter.mapNotNull {
                queries.enhet.get(it)?.toDto()
            }
        }
        AvtaleValidator.validateNavEnheter(navEnheter).bind()
        val dbo = request.toDbo()

        db.transaction {
            queries.avtale.updateVeilederinfo(previous.id, dbo)

            val dto = logEndring("Veilederinformasjon oppdatert", previous.id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
    }

    fun upsertPrismodell(
        id: UUID,
        request: PrismodellRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(id)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val dbo = AvtaleValidator
            .validatePrismodell(request, previous.tiltakstype.tiltakskode, previous.tiltakstype.navn)
            .bind()

        db.transaction {
            queries.avtale.upsertPrismodell(id, dbo)

            val dto = logEndring("Prismodell oppdatert", id, navIdent)
            schedulePublishGjennomforingerForAvtale(dto)
            dto
        }
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
        val avtale = getOrError(id)

        val errors = buildList {
            when (avtale.status) {
                is AvtaleStatus.Utkast, is AvtaleStatus.Aktiv -> Unit
                is AvtaleStatus.Avbrutt -> add(FieldError.root("Avtalen er allerede avbrutt"))
                is AvtaleStatus.Avsluttet -> add(FieldError.root("Avtalen er allerede avsluttet"))
            }

            val antallAktiveGjennomforinger = queries.gjennomforing.getByAvtale(id).count {
                it.status.type == GjennomforingStatusType.GJENNOMFORES
            }
            if (antallAktiveGjennomforinger > 0) {
                val message = listOf(
                    "Avtalen har",
                    antallAktiveGjennomforinger,
                    if (antallAktiveGjennomforinger > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                    "og kan derfor ikke avbrytes",
                ).joinToString(" ")
                add(FieldError.root(message))
            }
        }
        if (errors.isNotEmpty()) {
            return errors.left()
        }

        queries.avtale.setStatus(
            id = id,
            status = AvtaleStatusType.AVBRUTT,
            tidspunkt = tidspunkt,
            aarsaker = aarsakerOgForklaring.aarsaker,
            forklaring = aarsakerOgForklaring.forklaring,
        )

        logEndring("Avtalen ble avbrutt", id, avbruttAv).right()
    }

    fun registrerOpsjon(
        avtaleId: UUID,
        request: OpprettOpsjonLoggRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val avtale = getOrError(avtaleId)
            requireNotNull(avtale.sluttDato) {
                "Sluttdato på avtalen er null"
            }

            val dbo = AvtaleValidator.validateOpprettOpsjonLoggRequest(
                request,
                avtale,
                navIdent,
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

    // Filtrer vekk underenheter uten fylke
    fun sanitizeNavEnheter(
        navEnheter: List<NavEnhetNummer>,
    ): List<NavEnhetNummer> = db.session {
        return NavEnhetHelpers.buildNavRegioner(
            navEnheter.mapNotNull { queries.enhet.get(it)?.toDto() },
        )
            .flatMap { listOf(it.enhetsnummer) + it.enheter.map { it.enhetsnummer } }
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
            FieldError.ofPointer(
                "/arrangorHovedenhet",
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
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
                avtale.prismodell.type !== PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
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
                AvtaleHandling.REGISTRER_OPSJON,
                AvtaleHandling.DUPLISER,
                AvtaleHandling.REDIGER,
                -> ansatt.hasGenerellRolle(Rolle.AVTALER_SKRIV)
            }
        }
    }
}
