package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.api.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetStengtHosArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingGruppetiltakDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDtoMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompaktDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompaktEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompaktGruppetiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
    private val tiltakstypeService: TiltakstypeService,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    suspend fun upsert(
        request: GjennomforingRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, GjennomforingGruppetiltak> = either {
        val previous = getGruppetiltak(request.id)
        val ctx = getValidatorCtx(request, previous, today)

        val dbo = GjennomforingValidator
            .validate(
                request.copy(
                    veilederinformasjon = request.veilederinformasjon.copy(
                        navKontorer = sanitizeNavEnheter(
                            request.veilederinformasjon.navRegioner,
                            request.veilederinformasjon.navKontorer,
                        ),
                    ),
                ),
                ctx,
            )
            .onRight { dbo ->
                dbo.kontaktpersoner.forEach {
                    navAnsattService.addUserToKontaktpersoner(it.navIdent)
                }
            }
            .bind()

        if (previous != null && isEqual(previous, dbo)) {
            return@either previous
        }

        db.transaction {
            queries.gjennomforing.upsertGruppetiltak(dbo)

            dispatchNotificationToNewAdministrators(dbo, navIdent)

            val operation = if (ctx.previous == null) {
                "Opprettet gjennomføring"
            } else {
                "Redigerte gjennomføring"
            }
            val dto = logEndring(operation, dbo.id, navIdent)

            queries.gjennomforing.setFreeTextSearch(dbo.id, listOf(dbo.navn, dto.arrangor.navn))

            publishToKafka(dto)
            dto
        }
    }

    fun getValidatorCtx(
        request: GjennomforingRequest,
        previous: GjennomforingGruppetiltak?,
        today: LocalDate,
    ): GjennomforingValidator.Ctx = db.session {
        val avtale = queries.avtale.getOrError(request.avtaleId)
        val kontaktpersoner = request.kontaktpersoner.mapNotNull { queries.ansatt.getByNavIdent(it.navIdent) }
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
        val arrangor = request.arrangorId?.let { queries.arrangor.getById(it) }
        val antallDeltakere = queries.deltaker.getByGjennomforingId(request.id).size
        val status = resolveStatus(previous?.status?.type, request, today)
        return GjennomforingValidator.Ctx(
            previous = previous?.let {
                GjennomforingValidator.Ctx.Gjennomforing(
                    avtaleId = it.avtaleId,
                    oppstart = it.oppstart,
                    arrangorId = it.arrangor.id,
                    status = it.status.type,
                    sluttDato = it.sluttDato,
                    pameldingType = it.pameldingType,
                )
            },
            avtale = avtale,
            arrangor = arrangor,
            kontaktpersoner = kontaktpersoner,
            administratorer = administratorer,
            antallDeltakere = antallDeltakere,
            status = status,
        )
    }

    fun get(id: UUID): Gjennomforing? = db.session {
        getGruppetiltak(id) ?: queries.gjennomforing.getEnkeltplass(id)
    }

    fun getGruppetiltak(id: UUID): GjennomforingGruppetiltak? = db.session {
        queries.gjennomforing.getGruppetiltak(id)
    }

    fun getAll(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<GjennomforingKompaktDto> = db.session {
        val tiltakstyper = filter.tiltakstypeIder.ifEmpty {
            tiltakstypeService
                .getAll(TiltakstypeFilter(features = setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON)))
                .map { it.id }
        }
        queries.gjennomforing.getAll(
            pagination,
            search = filter.search,
            navEnheter = filter.navEnheter,
            tiltakstypeIder = tiltakstyper,
            statuser = filter.statuser,
            sortering = filter.sortering,
            avtaleId = filter.avtaleId,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            koordinatorNavIdent = filter.koordinatorNavIdent,
            publisert = filter.publisert,
            sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
        ).let { (totalCount, items) ->
            val data = items.map {
                when (it) {
                    is GjennomforingKompaktGruppetiltak -> GjennomforingKompaktDto(
                        id = it.id,
                        navn = it.navn,
                        lopenummer = it.lopenummer,
                        startDato = it.startDato,
                        sluttDato = it.sluttDato,
                        status = GjennomforingDtoMapper.fromGjennomforingStatus(it.status),
                        arrangor = it.arrangor,
                        tiltakstype = it.tiltakstype,
                        publisert = it.publisert,
                        kontorstruktur = it.kontorstruktur,
                    )

                    is GjennomforingKompaktEnkeltplass -> GjennomforingKompaktDto(
                        id = it.id,
                        navn = it.tiltakstype.navn,
                        lopenummer = it.lopenummer,
                        startDato = it.startDato,
                        sluttDato = it.sluttDato,
                        status = GjennomforingDtoMapper.fromGjennomforingStatus(it.status),
                        arrangor = it.arrangor,
                        tiltakstype = it.tiltakstype,
                        publisert = false,
                        kontorstruktur = listOf(),
                    )
                }
            }
            PaginatedResponse.of(pagination, totalCount, data)
        }
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent): Unit = db.transaction {
        queries.gjennomforing.setPublisert(id, publisert)
        val operation = if (publisert) {
            "Tiltak publisert"
        } else {
            "Tiltak avpublisert"
        }
        logEndring(operation, id, navIdent)
    }

    fun setTilgjengeligForArrangorDato(
        id: UUID,
        tilgjengeligForArrangorDato: LocalDate?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> = db.transaction {
        val gjennomforing = getOrError(id)

        GjennomforingValidator
            .validateTilgjengeligForArrangorDato(
                tilgjengeligForArrangorDato,
                gjennomforing.startDato,
            )
            .map {
                queries.gjennomforing.setTilgjengeligForArrangorDato(id, it)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                val dto = logEndring(operation, id, navIdent)
                publishToKafka(dto)
            }
    }

    fun avsluttGjennomforing(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): GjennomforingGruppetiltak = db.transaction {
        val gjennomforing = getOrError(id)

        check(gjennomforing.status is GjennomforingStatus.Gjennomfores) {
            "Gjennomføringen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Gjennomføringen kan ikke avsluttes før sluttdato"
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = GjennomforingStatusType.AVSLUTTET,
            tidspunkt = avsluttetTidspunkt,
            aarsaker = null,
            forklaring = null,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = logEndring("Gjennomføringen ble avsluttet", id, endretAv)
        publishToKafka(dto)
        dto
    }

    fun avbrytGjennomforing(
        id: UUID,
        avbruttAv: Agent,
        tidspunkt: LocalDateTime,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>,
    ): Either<List<FieldError>, GjennomforingGruppetiltak> = db.transaction {
        val gjennomforing = getOrError(id)

        when (gjennomforing.status) {
            is GjennomforingStatus.Gjennomfores -> Unit

            is GjennomforingStatus.Avlyst, is GjennomforingStatus.Avbrutt ->
                return FieldError.root("Gjennomføringen er allerede avbrutt").nel().left()

            is GjennomforingStatus.Avsluttet ->
                return FieldError.root("Gjennomføringen er allerede avsluttet").nel().left()
        }

        val tidspunktForStart = gjennomforing.startDato.atStartOfDay()
        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        val status = if (tidspunkt.isBefore(tidspunktForStart)) {
            GjennomforingStatusType.AVLYST
        } else if (tidspunktForSlutt == null || tidspunkt.isBefore(tidspunktForSlutt)) {
            GjennomforingStatusType.AVBRUTT
        } else {
            throw Exception("Gjennomføring allerede avsluttet")
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = status,
            tidspunkt = tidspunkt,
            aarsaker = aarsakerOgForklaring.aarsaker,
            forklaring = aarsakerOgForklaring.forklaring,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = logEndring("Gjennomføringen ble avbrutt", id, avbruttAv)
        publishToKafka(dto)
        dto.right()
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, agent: Agent): Unit = db.transaction {
        queries.gjennomforing.setApentForPamelding(id, apentForPamelding)

        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        val dto = logEndring(operation, id, agent)
        publishToKafka(dto)
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, GjennomforingGruppetiltak> = db.transaction {
        return query {
            queries.gjennomforing.setStengtHosArrangor(id, periode, beskrivelse)
        }.mapLeft {
            if (it is IntegrityConstraintViolation.ExclusionViolation) {
                FieldError.of(
                    "Perioden kan ikke overlappe med andre perioder",
                    SetStengtHosArrangorRequest::periodeStart,
                ).nel()
            } else {
                throw it.error
            }
        }.map {
            val operation = listOf(
                "Registrerte stengt hos arrangør i perioden",
                periode.start.formaterDatoTilEuropeiskDatoformat(),
                "-",
                periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
            ).joinToString(separator = " ")
            val dto = logEndring(operation, id, navIdent)
            publishToKafka(dto)
            dto
        }
    }

    fun deleteStengtHosArrangor(id: UUID, periodeId: Int, navIdent: NavIdent) = db.transaction {
        queries.gjennomforing.deleteStengtHosArrangor(periodeId)

        val dto = logEndring("Fjernet periode med stengt hos arrangør", id, navIdent)
        publishToKafka(dto)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        return queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, id)
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        navIdent: NavIdent,
    ): Unit = db.transaction {
        queries.gjennomforing.frikobleKontaktpersonFraGjennomforing(
            kontaktpersonId = kontaktpersonId,
            gjennomforingId = gjennomforingId,
        )

        logEndring("Kontaktperson ble fjernet fra gjennomføringen", gjennomforingId, navIdent)
    }

    private fun resolveStatus(
        previous: GjennomforingStatusType?,
        request: GjennomforingRequest,
        today: LocalDate,
    ): GjennomforingStatusType {
        return when (previous) {
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> previous

            else -> if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
                GjennomforingStatusType.GJENNOMFORES
            } else {
                GjennomforingStatusType.AVSLUTTET
            }
        }
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingGruppetiltak {
        return queries.gjennomforing.getGruppetiltakOrError(id)
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        dbo: GjennomforingGruppetiltakDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = getGruppetiltak(dbo.id)?.administratorer?.map { it.navIdent }?.toSet()
            ?: setOf()

        val administratorsToNotify = (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull()
            ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på gjennomføringen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        gjennomforingId: UUID,
        endretAv: Agent,
    ): GjennomforingGruppetiltak {
        val gjennomforing = getOrError(gjennomforingId)
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            operation,
            endretAv,
            gjennomforingId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(gjennomforing)
        }
        return gjennomforing
    }

    private fun QueryContext.publishToKafka(gjennomforing: GjennomforingGruppetiltak) {
        val gjennomforingV2: TiltaksgjennomforingV2Dto = TiltaksgjennomforingV2Mapper.fromGjennomforing(gjennomforing)
        val recordV2 = StoredProducerRecord(
            config.gjennomforingV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(TiltaksgjennomforingV2Dto.serializer(), gjennomforingV2).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(recordV2)
    }

    // Filtrer vekk underenheter uten fylke
    fun sanitizeNavEnheter(
        navRegioner: List<NavEnhetNummer>,
        navKontorer: List<NavEnhetNummer>,
    ): List<NavEnhetNummer> = db.session {
        return NavEnhetHelpers.buildNavRegioner(
            (navRegioner + navKontorer).mapNotNull { queries.enhet.get(it)?.toDto() },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } }
    }

    fun handlinger(gjennomforing: Gjennomforing, ansatt: NavAnsatt): Set<GjennomforingHandling> = when (gjennomforing) {
        is GjennomforingEnkeltplass -> setOf()

        is GjennomforingGruppetiltak -> {
            val statusGjennomfores = gjennomforing.status is GjennomforingStatus.Gjennomfores

            return setOfNotNull(
                GjennomforingHandling.PUBLISER.takeIf { statusGjennomfores },
                GjennomforingHandling.AVBRYT.takeIf { statusGjennomfores },
                GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING.takeIf { statusGjennomfores },
                GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR.takeIf { statusGjennomfores },
                GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR.takeIf { statusGjennomfores },
                GjennomforingHandling.REDIGER.takeIf { statusGjennomfores },
                GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER.takeIf {
                    gjennomforing.tiltakstype.tiltakskode == Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
                },

                GjennomforingHandling.DUPLISER,
                GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING,
                GjennomforingHandling.OPPRETT_TILSAGN,
                GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
            )
                .filter { tilgangTilHandling(it, ansatt) }
                .toSet()
        }
    }

    companion object {
        fun tilgangTilHandling(handling: GjennomforingHandling, ansatt: NavAnsatt): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            val oppfolgerGjennomforing = ansatt.hasGenerellRolle(Rolle.OPPFOLGER_GJENNOMFORING)
            val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                GjennomforingHandling.PUBLISER -> skrivGjennomforing
                GjennomforingHandling.AVBRYT -> skrivGjennomforing
                GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING -> skrivGjennomforing || oppfolgerGjennomforing
                GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR -> skrivGjennomforing || oppfolgerGjennomforing
                GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR -> skrivGjennomforing
                GjennomforingHandling.DUPLISER -> skrivGjennomforing
                GjennomforingHandling.REDIGER -> skrivGjennomforing
                GjennomforingHandling.OPPRETT_TILSAGN -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_EKSTRATILSAGN -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING -> saksbehandlerOkonomi
            }
        }
    }
}

private fun isEqual(
    previous: GjennomforingGruppetiltak,
    dbo: GjennomforingGruppetiltakDbo,
): Boolean = dbo == GjennomforingGruppetiltakDbo(
    id = previous.id,
    navn = previous.navn,
    tiltakstypeId = previous.tiltakstype.id,
    arrangorId = previous.arrangor.id,
    arrangorKontaktpersoner = previous.arrangor.kontaktpersoner.map { it.id },
    startDato = previous.startDato,
    sluttDato = previous.sluttDato,
    status = previous.status.type,
    antallPlasser = previous.antallPlasser,
    avtaleId = checkNotNull(previous.avtaleId) { "Forventet at avtale var definert!" },
    administratorer = previous.administratorer.map { it.navIdent },
    navEnheter = previous.kontorstruktur
        .flatMap { (region, kontorer) ->
            kontorer.map { kontor -> kontor.enhetsnummer } + region.enhetsnummer
        }
        .toSet(),
    oppstart = previous.oppstart,
    kontaktpersoner = previous.kontaktpersoner.map {
        GjennomforingKontaktpersonDbo(
            navIdent = it.navIdent,
            beskrivelse = it.beskrivelse,
        )
    },
    oppmoteSted = previous.oppmoteSted,
    faneinnhold = previous.faneinnhold,
    beskrivelse = previous.beskrivelse,
    deltidsprosent = previous.deltidsprosent,
    estimertVentetidVerdi = previous.estimertVentetid?.verdi,
    estimertVentetidEnhet = previous.estimertVentetid?.enhet,
    tilgjengeligForArrangorDato = previous.tilgjengeligForArrangorDato,
    amoKategorisering = previous.amoKategorisering,
    utdanningslop = previous.utdanningslop?.toDbo(),
    pameldingType = previous.pameldingType,
    prismodellId = checkNotNull(previous.prismodell?.id) { "Forventet at prismodell var definert!" },
)
