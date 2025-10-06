package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.*
import arrow.core.raise.either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.MrExceptions
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.api.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetStengtHosArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDtoMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val topic: String,
    )

    suspend fun upsert(
        request: GjennomforingRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, Gjennomforing> = either {
        val previous = get(request.id)
        val ctx = getValidatorCtx(request, previous, today)

        val dbo = GjennomforingValidator
            .validate(request.copy(navEnheter = sanitizeNavEnheter(request.navEnheter)), ctx)
            .onRight { dbo ->
                dbo.kontaktpersoner.forEach {
                    navAnsattService.addUserToKontaktpersoner(it.navIdent)
                }
            }
            .bind()

        if (previous != null && GjennomforingDboMapper.fromGjennomforing(previous) == dbo) {
            return@either previous
        }

        db.transaction {
            queries.gjennomforing.upsert(dbo)

            dispatchNotificationToNewAdministrators(dbo, navIdent)

            val dto = getOrError(dbo.id)
            val operation = if (ctx.previous == null) {
                "Opprettet gjennomføring"
            } else {
                "Redigerte gjennomføring"
            }
            logEndring(operation, dto, navIdent)
            publishToKafka(dto)

            dto
        }
    }

    fun getValidatorCtx(
        request: GjennomforingRequest,
        previous: Gjennomforing?,
        today: LocalDate,
    ): GjennomforingValidator.Ctx = db.session {
        val avtale = requireNotNull(db.session { queries.avtale.get(request.avtaleId) }) {
            "Gjennomføringen manglet avtale"
        }

        val kontaktpersoner = db.session {
            request.kontaktpersoner.mapNotNull { queries.ansatt.getByNavIdent(it.navIdent) }
        }
        val administratorer = db.session {
            request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
        }
        val arrangor = db.session { queries.arrangor.getById(request.arrangorId) }
        val antallDeltakere = db.session {
            queries.deltaker.getAll(pagination = Pagination.of(1, 1), gjennomforingId = request.id).size
        }
        val status = resolveStatus(previous?.status?.type, request, today)

        return GjennomforingValidator.Ctx(
            previous = previous?.let {
                GjennomforingValidator.Ctx.Gjennomforing(
                    avtaleId = it.avtaleId,
                    oppstart = it.oppstart,
                    arrangorId = it.arrangor.id,
                    status = it.status.type,
                    sluttDato = it.sluttDato,
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
        queries.gjennomforing.get(id)
    }

    fun getAll(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<GjennomforingDto> = db.session {
        queries.gjennomforing.getAll(
            pagination,
            search = filter.search,
            navEnheter = filter.navEnheter,
            tiltakstypeIder = filter.tiltakstypeIder,
            statuser = filter.statuser,
            sortering = filter.sortering,
            avtaleId = filter.avtaleId,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            koordinatorNavIdent = filter.koordinatorNavIdent,
            publisert = filter.publisert,
            sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
        ).let { (totalCount, items) ->
            val data = items.map { GjennomforingDtoMapper.fromGjennomforing(it) }
            PaginatedResponse.of(pagination, totalCount, data)
        }
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent): Unit = db.transaction {
        queries.gjennomforing.setPublisert(id, publisert)
        val dto = getOrError(id)
        val operation = if (publisert) {
            "Tiltak publisert"
        } else {
            "Tiltak avpublisert"
        }
        logEndring(operation, dto, navIdent)
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
                val dto = getOrError(id)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                logEndring(operation, dto, navIdent)
                publishToKafka(dto)
            }
    }

    fun avsluttGjennomforing(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): Gjennomforing = db.transaction {
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

        val dto = getOrError(id)
        logEndring("Gjennomføringen ble avsluttet", dto, endretAv)
        publishToKafka(dto)
        dto
    }

    fun avbrytGjennomforing(
        id: UUID,
        avbruttAv: Agent,
        tidspunkt: LocalDateTime,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>,
    ): Either<List<FieldError>, Gjennomforing> = db.transaction {
        val gjennomforing = getOrError(id)

        val errors = buildList {
            when (gjennomforing.status) {
                is GjennomforingStatus.Gjennomfores -> Unit

                is GjennomforingStatus.Avlyst, is GjennomforingStatus.Avbrutt ->
                    add(FieldError.root("Gjennomføringen er allerede avbrutt"))

                is GjennomforingStatus.Avsluttet ->
                    add(FieldError.root("Gjennomføringen er allerede avsluttet"))
            }
        }
        if (errors.isNotEmpty()) {
            return errors.left()
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

        val dto = getOrError(id)
        logEndring("Gjennomføringen ble avbrutt", dto, avbruttAv)
        publishToKafka(dto)

        dto.right()
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, agent: Agent): Unit = db.transaction {
        queries.gjennomforing.setApentForPamelding(id, apentForPamelding)

        val dto = getOrError(id)
        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        logEndring(operation, dto, agent)
        publishToKafka(dto)
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, Gjennomforing> = db.transaction {
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
            val dto = getOrError(id)
            val operation = listOf(
                "Registrerte stengt hos arrangør i perioden",
                periode.start.formaterDatoTilEuropeiskDatoformat(),
                "-",
                periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
            ).joinToString(separator = " ")
            logEndring(operation, dto, navIdent)
            publishToKafka(dto)
            dto
        }
    }

    fun deleteStengtHosArrangor(id: UUID, periodeId: Int, navIdent: NavIdent) = db.transaction {
        queries.gjennomforing.deleteStengtHosArrangor(periodeId)

        val dto = getOrError(id)
        val operation = "Fjernet periode med stengt hos arrangør"
        logEndring(operation, dto, navIdent)
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

        val gjennomforing = getOrError(gjennomforingId)
        logEndring(
            "Kontaktperson ble fjernet fra gjennomføringen via arrangørsidene",
            gjennomforing,
            navIdent,
        )
    }

    fun handlinger(gjennomforing: Gjennomforing, navIdent: NavIdent): Set<GjennomforingHandling> {
        val ansatt = db.session { queries.ansatt.getByNavIdent(navIdent) }
            ?: throw MrExceptions.navAnsattNotFound(navIdent)

        val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
        val oppfolgerGjennomforing = ansatt.hasGenerellRolle(Rolle.OPPFOLGER_GJENNOMFORING)
        val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
        val statusGjennomfores = gjennomforing.status is GjennomforingStatus.Gjennomfores

        return setOfNotNull(
            GjennomforingHandling.PUBLISER.takeIf {
                statusGjennomfores && skrivGjennomforing
            },
            GjennomforingHandling.AVBRYT.takeIf {
                statusGjennomfores && skrivGjennomforing
            },
            GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING.takeIf {
                statusGjennomfores && (skrivGjennomforing || oppfolgerGjennomforing)
            },
            GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR.takeIf {
                statusGjennomfores && (skrivGjennomforing || oppfolgerGjennomforing)
            },
            GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR.takeIf {
                statusGjennomfores && skrivGjennomforing
            },
            GjennomforingHandling.DUPLISER.takeIf {
                skrivGjennomforing
            },
            GjennomforingHandling.REDIGER.takeIf {
                statusGjennomfores && skrivGjennomforing
            },
            GjennomforingHandling.OPPRETT_TILSAGN.takeIf {
                saksbehandlerOkonomi
            },
            GjennomforingHandling.OPPRETT_EKSTRATILSAGN.takeIf {
                saksbehandlerOkonomi
            },
            GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER.takeIf {
                saksbehandlerOkonomi && gjennomforing.tiltakstype.tiltakskode == Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            },
            GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING.takeIf {
                saksbehandlerOkonomi
            },
        )
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

    private fun QueryContext.getOrError(id: UUID): Gjennomforing {
        val gjennomforing = queries.gjennomforing.get(id)
        return requireNotNull(gjennomforing) { "Gjennomføringen med id=$id finnes ikke" }
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        dbo: GjennomforingDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet()
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
        dto: Gjennomforing,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.publishToKafka(dto: Gjennomforing) {
        val eksternDto = TiltaksgjennomforingV1Mapper.fromGjennomforing(dto)

        val record = StoredProducerRecord(
            config.topic,
            eksternDto.id.toString().toByteArray(),
            Json.encodeToString(eksternDto).toByteArray(),
            null,
        )

        queries.kafkaProducerRecord.storeRecord(record)
    }

    fun sanitizeNavEnheter(navEnheter: Set<NavEnhetNummer>): Set<NavEnhetNummer> = db.session {
        // Filtrer vekk underenheter uten fylke
        return NavEnhetHelpers.buildNavRegioner(
            navEnheter.mapNotNull { queries.enhet.get(it)?.toDto() },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } + it.enhetsnummer }
            .toSet()
    }
}
