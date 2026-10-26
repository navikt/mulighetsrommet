package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorIfMissing
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.avtale.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.domain.avtale.Avtale
import no.nav.mulighetsrommet.api.domain.avtale.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.avtale.OppdaterAvtaledetaljerCommand
import no.nav.mulighetsrommet.api.domain.avtale.OpprettAvtaleCommand
import no.nav.mulighetsrommet.api.domain.avtale.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.avtale.RegistrerOpsjonCommand
import no.nav.mulighetsrommet.api.domain.avtale.Vurderingsdato
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.validation.FieldError
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
                previous = null,
            ).bind()
        }
        val detaljer = AvtaleValidator.validateCreateAvtale(request, createAvtaleContext).bind()

        val prisinfo = if (detaljer.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            val prismodell = db.session { queries.prismodell.getBySystemId(request.detaljer.tiltakskode.name) }
            if (prismodell == null) {
                raise(
                    FieldError.of(
                        "Systembestemt prismodell mangler for forhåndsgodkjent avtale",
                        OpprettAvtaleRequest::prismodeller,
                    ).nel(),
                )
            }
            Avtale.Prisinfo.Systembestemt(prismodell)
        } else {
            val context = AvtaleValidator.PrismodellParseContext(
                tiltakskode = detaljer.tiltakskode,
                avtaleStartDato = detaljer.startDato,
                gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
            )
            val prismodeller = AvtaleValidator.parsePrismodeller(request.prismodeller, context).bind()
            Avtale.Prisinfo.Egendefinert.of(detaljer.tiltakskode, prismodeller).bind()
        }

        val personvern = request.personvern.toAvtalePersonvern().bind()

        val veilederinfo = request.veilederinformasjon.toVeilederinfo().bind()

        db.transaction {
            val avtale = request.toAvtale(detaljer, prisinfo, personvern, veilederinfo).bind()
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
                tiltakskode = avtale.tiltakskode,
                gjennomforinger = gjennomforinger.map {
                    AvtaleValidator.Ctx.Gjennomforing(
                        arrangor = it.arrangor,
                        startDato = it.startDato,
                        utdanningslop = queries.opplaering.get(it.id)?.utdanningslop,
                        status = it.status.type,
                        prismodellId = it.prismodell.id,
                    )
                },
            )
            val context = getValidatorCtx(
                request = request,
                previous = previous,
            ).bind()

            val detaljer = AvtaleValidator
                .validateUpdateDetaljer(request, context)
                .bind()

            val oppdatertAvtale = avtale.toUpdatedAvtale(detaljer).bind()
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
        val personvern = request.toAvtalePersonvern().bind()

        db.transaction {
            val previous = getOrError(avtaleId)
            repository.avtale.save(previous.medPersonvern(personvern))

            logEndring("Personvern oppdatert", previous.id, navIdent)
                .also { schedulePublishGjennomforingerForAvtale(it) }
        }
    }

    fun upsertVeilederinfo(
        avtaleId: UUID,
        request: VeilederinfoRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val veilederinfo = request.toVeilederinfo().bind()

        db.transaction {
            val previous = getOrError(avtaleId)

            repository.avtale.save(previous.medVeilederinfo(veilederinfo))

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

        val context = AvtaleValidator.PrismodellParseContext(
            tiltakskode = avtale.tiltakskode,
            avtaleStartDato = avtale.startDato,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode,
        )
        AvtaleValidator.parsePrismodeller(request, context).flatMap { prismodeller ->
            val bruktePrismodeller = queries.gjennomforing.getByAvtale(id).map { it.prismodell.id }.toSet()
            if (bruktePrismodeller.any { id -> prismodeller.none { it.id == id } }) {
                FieldError.of(
                    "Prismodell kan ikke fjernes fordi en eller flere gjennomføringer er koblet til prismodellen",
                    OpprettAvtaleRequest::prismodeller,
                ).nel().left()
            } else {
                avtale.medPrismodeller(prismodeller)
            }
        }.map { oppdatert ->
            repository.avtale.save(oppdatert)

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
        avtale.medRammedetaljer(request.totalRamme, request.utbetaltArena).map { oppdatert ->
            repository.avtale.save(oppdatert)
            logEndring("Rammedetaljer oppdatert", id, navIdent)
        }
    }

    fun avsluttAvtale(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        getOrError(id).avslutt(avsluttetTidspunkt).map { oppdatert ->
            repository.avtale.save(oppdatert)

            logEndring("Avtalen ble avsluttet", id, endretAv)
        }
    }

    fun avbrytAvtale(
        id: UUID,
        avbruttAv: NavIdent,
        tidspunkt: LocalDateTime,
        aarsakerOgBegrunnelse: AarsakerOgBegrunnelseRequest<AvbrytAvtaleAarsak>,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        either {
            val avtale = getOrError(id)

            val antallAktiveGjennomforinger = queries.gjennomforing.getByAvtale(id).count {
                it.status is GjennomforingAvtaleStatus.Gjennomfores
            }
            ensure(antallAktiveGjennomforinger == 0) {
                val message = listOf(
                    "Avtalen har",
                    antallAktiveGjennomforinger,
                    if (antallAktiveGjennomforinger > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                    "og kan derfor ikke avbrytes",
                ).joinToString(" ")
                FieldError.of(message).nel()
            }

            avtale.avbryt(tidspunkt, aarsakerOgBegrunnelse.aarsaker, aarsakerOgBegrunnelse.begrunnelse).bind()
        }.map { oppdatert ->
            repository.avtale.save(oppdatert)

            logEndring("Avtalen ble avbrutt", id, avbruttAv)
        }
    }

    fun registrerOpsjon(
        avtaleId: UUID,
        request: OpprettOpsjonLoggRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, Avtale> = db.transaction {
        either {
            val avtale = getOrError(avtaleId)

            val command = request.toRegistrerOpsjonCommand(avtale, navIdent).bind()
            val oppdatert = avtale.registrerOpsjon(command, Vurderingsdato(today)).bind()

            repository.avtale.save(oppdatert)

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
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(avtaleId)

        avtale.slettOpsjon(opsjonId, Vurderingsdato(today)).map { oppdatert ->
            repository.avtale.save(oppdatert)

            logEndring("Opsjon slettet", avtaleId, slettesAv)
        }
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
        previous: AvtaleValidator.Ctx.Avtale?,
    ): Either<List<FieldError>, AvtaleValidator.Ctx> = either {
        val tiltakstype = queries.tiltakstype.getByTiltakskode(request.tiltakskode)
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.get(it) }

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
            kategorisering = kategorisering,
        )
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

    private fun VeilederinfoRequest.toVeilederinfo(): Either<List<FieldError>, Avtale.VeilederInfo> {
        val navEnheter = db.session {
            navEnheter.mapNotNull { queries.enhet.get(it) }.toSet()
        }
        return Avtale.VeilederInfo.of(beskrivelse, faneinnhold, navEnheter)
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
                avtale.prisinfo is Avtale.Prisinfo.Egendefinert
            },
            AvtaleHandling.OPPDATER_RAMMEDETALJER.takeIf {
                avtale.prisinfo is Avtale.Prisinfo.Egendefinert
            },
            AvtaleHandling.REGISTRER_OPSJON.takeIf {
                avtale.opsjoner.modell is Opsjonsmodell.MedOpsjonsmulighet
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
    prisinfo: Avtale.Prisinfo,
    personvern: Avtale.Personvern,
    veilederinfo: Avtale.VeilederInfo,
): Either<List<FieldError>, Avtale> = Avtale.opprett(
    command = OpprettAvtaleCommand(
        id = id,
        tiltakskode = detaljer.tiltakskode,
        navn = detaljer.navn,
        sakarkivNummer = detaljer.sakarkivNummer,
        arrangor = detaljer.arrangor,
        startDato = detaljer.startDato,
        sluttDato = detaljer.sluttDato,
        opsjonsmodell = detaljer.opsjonsmodell,
        avtaletype = detaljer.avtaletype,
        administratorer = detaljer.administratorer.toSet(),
        veilederinfo = veilederinfo,
        personvern = personvern,
        opplaring = detaljer.opplaring,
        prisinfo = prisinfo,
    ),
    vurderingsdato = Vurderingsdato(LocalDate.now()),
)

private fun Avtale.toUpdatedAvtale(
    detaljer: AvtaleValidator.ValidatedDetaljer,
): Either<List<FieldError>, Avtale> = oppdaterDetaljer(
    command = OppdaterAvtaledetaljerCommand(
        navn = detaljer.navn,
        sakarkivNummer = detaljer.sakarkivNummer,
        arrangor = detaljer.arrangor,
        startDato = detaljer.startDato,
        sluttDato = detaljer.sluttDato,
        opsjonsmodell = detaljer.opsjonsmodell,
        avtaletype = detaljer.avtaletype,
        administratorer = detaljer.administratorer.toSet(),
        opplaring = detaljer.opplaring,
    ),
    vurderingsdato = Vurderingsdato(LocalDate.now()),
)

private fun PersonvernRequest.toAvtalePersonvern(): Either<List<FieldError>, Avtale.Personvern> {
    val typer = buildSet {
        addAll(personopplysninger)
        if (annetChecked == true) {
            add(Personopplysning.Type.ANNET)
        } else {
            remove(Personopplysning.Type.ANNET)
        }
    }
    return Avtale.Personvern.of(
        personopplysninger = typer,
        annetBeskrivelse = annetBeskrivelse,
        erBekreftet = personvernBekreftet,
    )
}

private fun OpprettOpsjonLoggRequest.toRegistrerOpsjonCommand(
    avtale: Avtale,
    navIdent: NavIdent,
): Either<List<FieldError>, RegistrerOpsjonCommand> = validation {
    when (type) {
        OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE -> {
            requireNotNull(nySluttDato) {
                FieldError.of("Ny sluttdato må være satt", OpprettOpsjonLoggRequest::nySluttDato)
            }
            RegistrerOpsjonCommand.UtloseOpsjon(nySluttDato, navIdent)
        }

        OpprettOpsjonLoggRequest.Type.ETT_AAR -> {
            val sluttDato = avtale.sluttDato
            requireNotNull(sluttDato) {
                FieldError.of(
                    "Opsjon kan ikke utløses fordi avtalen mangler sluttdato",
                    OpprettOpsjonLoggRequest::type,
                )
            }
            RegistrerOpsjonCommand.UtloseOpsjon(sluttDato.plusYears(1), navIdent)
        }

        OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON ->
            RegistrerOpsjonCommand.SkalIkkeUtloseOpsjon(navIdent)
    }
}
