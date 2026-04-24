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
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetStengtHosArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingAvtaleService(
    private val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    suspend fun create(
        request: GjennomforingRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, GjennomforingAvtale> = either {
        db.session { queries.gjennomforing.getGjennomforing(request.id) }
            ?.let { raise(listOf(FieldError.of("Gjennomføringen finnes allerede"))) }

        val detaljerResult = validateDetaljer(request.id, request.tiltakstypeId, request.avtaleId, request.detaljer, null, today)
            .bind()
        val veilederinfoResult = validateVeilederinfo(request.avtaleId, request.veilederinformasjon)
            .bind()

        db.transaction {
            queries.gjennomforing.upsert(detaljerResult.gjennomforing)
            persistDetaljer(request.id, detaljerResult, navIdent)
            persistVeilederinfo(request.id, veilederinfoResult, request.veilederinformasjon)

            logEndring("Opprettet gjennomføring", request.id, navIdent)
                .also { updateFreeTextSearch(it) }
                .also { publishToKafka(it) }
        }
    }

    fun updateDetaljer(
        id: UUID,
        request: GjennomforingDetaljerRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, GjennomforingAvtale> = either {
        val previous = db.session { getGjennomforingAvtale(id) }
            ?: raise(listOf(FieldError.root("Gjennomføringen finnes ikke")))

        val detaljerResult = validateDetaljer(id, previous.tiltakstype.id, previous.avtaleId, request, previous, today)
            .bind()

        db.transaction {
            persistDetaljer(id, detaljerResult, navIdent)

            logEndring("Detaljer redigert", id, navIdent)
                .also { updateFreeTextSearch(it) }
                .also { publishToKafka(it) }
        }
    }

    suspend fun updateVeilederinfo(
        id: UUID,
        request: GjennomforingVeilederinfoRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, GjennomforingAvtale> = either {
        val previous = db.session { getGjennomforingAvtale(id) }
            ?: raise(listOf(FieldError.root("Gjennomføringen finnes ikke")))

        val veilederinfoResult = validateVeilederinfo(previous.avtaleId, request)
            .bind()

        db.transaction {
            persistVeilederinfo(id, veilederinfoResult, request)

            logEndring("Informasjon for veiledere redigert", id, navIdent)
                .also { publishToKafka(it) }
        }
    }

    fun updateArenaData(id: UUID, arenadata: Gjennomforing.ArenaData): GjennomforingAvtale = db.transaction {
        val previous = getOrError(id)
        if (previous.arena == arenadata) {
            return previous
        }

        queries.gjennomforing.setArenaData(
            GjennomforingArenaDataDbo(
                id = id,
                tiltaksnummer = arenadata.tiltaksnummer,
                arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
            ),
        )

        val operation = if (previous.arena?.tiltaksnummer == null) {
            "Oppdatert med tiltaksnummer fra Arena"
        } else {
            "Endret i Arena"
        }
        logEndring(operation, id, Arena)
            .also { updateFreeTextSearch(it) }
            .also { publishToKafka(it) }
    }

    fun getGjennomforingAvtale(id: UUID): GjennomforingAvtale? {
        val gjennomforing = db.session { queries.gjennomforing.getGjennomforing(id) } ?: return null
        return when (gjennomforing) {
            is GjennomforingAvtale -> gjennomforing

            is GjennomforingEnkeltplass,
            is GjennomforingArena,
            -> throw IllegalArgumentException("Gjennomføring med id $id er ikke av typen GjennomforingAvtale")
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
    ): Either<List<FieldError>, GjennomforingAvtale> = db.transaction {
        val gjennomforing = getOrError(id)

        GjennomforingValidator
            .validateTilgjengeligForArrangorDato(
                tilgjengeligForArrangorDato,
                gjennomforing.startDato,
            )
            .map { dato ->
                queries.gjennomforing.setTilgjengeligForArrangorDato(id, dato)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                logEndring(operation, id, navIdent).also { publishToKafka(it) }
            }
    }

    fun avsluttGjennomforing(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): GjennomforingAvtale = db.transaction {
        val gjennomforing = getOrError(id)

        check(gjennomforing.status == GjennomforingStatusType.GJENNOMFORES) {
            "Gjennomføringen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Gjennomføringen kan ikke avsluttes før sluttdato"
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = GjennomforingStatusType.AVSLUTTET,
            sluttDato = null,
            aarsaker = null,
            forklaring = null,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        logEndring("Gjennomføringen ble avsluttet", id, endretAv).also { publishToKafka(it) }
    }

    fun avbrytGjennomforing(
        id: UUID,
        avbruttAv: Agent,
        sluttDato: LocalDate,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>,
    ): Either<List<FieldError>, GjennomforingAvtale> = db.transaction {
        val gjennomforing = getOrError(id)

        when (gjennomforing.status) {
            GjennomforingStatusType.GJENNOMFORES -> Unit

            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT ->
                return FieldError.root("Gjennomføringen er allerede avbrutt").nel().left()

            GjennomforingStatusType.AVSLUTTET ->
                return FieldError.root("Gjennomføringen er allerede avsluttet").nel().left()
        }

        val (status, nySluttDato) = if (sluttDato.isBefore(gjennomforing.startDato)) {
            GjennomforingStatusType.AVLYST to null
        } else if (gjennomforing.sluttDato == null || sluttDato.isBefore(gjennomforing.sluttDato)) {
            GjennomforingStatusType.AVBRUTT to sluttDato
        } else {
            throw Exception("Gjennomføring allerede avsluttet")
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = status,
            sluttDato = nySluttDato,
            aarsaker = aarsakerOgForklaring.aarsaker,
            forklaring = aarsakerOgForklaring.forklaring,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        logEndring("Gjennomføringen ble avbrutt", id, avbruttAv)
            .also { publishToKafka(it) }
            .right()
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, agent: Agent): Unit = db.transaction {
        queries.gjennomforing.setApentForPamelding(id, apentForPamelding)

        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        logEndring(operation, id, agent).also { publishToKafka(it) }
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, GjennomforingAvtale> = db.transaction {
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
            logEndring(operation, id, navIdent).also { publishToKafka(it) }
        }
    }

    fun deleteStengtHosArrangor(id: UUID, periodeId: Int, navIdent: NavIdent) = db.transaction {
        queries.gjennomforing.deleteStengtHosArrangor(periodeId)

        logEndring("Fjernet periode med stengt hos arrangør", id, navIdent).also { publishToKafka(it) }
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

    private fun validateDetaljer(
        id: UUID,
        tiltakstypeId: UUID,
        avtaleId: UUID,
        request: GjennomforingDetaljerRequest,
        previous: GjennomforingAvtale?,
        today: LocalDate,
    ): Either<List<FieldError>, GjennomforingValidator.DetaljerResult> {
        val ctx = getValidatorCtx(id, avtaleId, request, previous, today)
        return GjennomforingValidator.validateDetaljer(id, tiltakstypeId, avtaleId, request, ctx)
    }

    private suspend fun validateVeilederinfo(
        avtaleId: UUID,
        request: GjennomforingVeilederinfoRequest,
    ): Either<List<FieldError>, GjennomforingValidator.VeilederinfoResult> {
        val avtale = db.session { queries.avtale.getOrError(avtaleId) }
        val kontaktpersoner = db.session {
            request.kontaktpersoner.mapNotNull { queries.ansatt.getByNavIdent(it.navIdent) }
        }
        request.kontaktpersoner.forEach {
            navAnsattService.addUserToKontaktpersoner(it.navIdent)
        }
        return GjennomforingValidator.validateVeilederinfo(request, avtale, kontaktpersoner)
    }

    private fun QueryContext.persistDetaljer(
        id: UUID,
        result: GjennomforingValidator.DetaljerResult,
        navIdent: NavIdent,
    ) {
        queries.gjennomforing.updateDetaljer(result.detaljer)
        setAdministratorer(id, result.administratorer, navIdent, result.gjennomforing.navn)
        queries.gjennomforing.setArrangorKontaktpersoner(id, result.arrangorKontaktpersoner)
        queries.gjennomforing.setUtdanningslop(id, result.utdanningslop)
        queries.gjennomforing.setAmoKategorisering(id, result.amoKategorisering)
    }

    private fun QueryContext.persistVeilederinfo(
        id: UUID,
        result: GjennomforingValidator.VeilederinfoResult,
        request: GjennomforingVeilederinfoRequest,
    ) {
        queries.gjennomforing.setNavEnheter(id, result.navEnheter)
        queries.gjennomforing.setRedaksjoneltInnhold(id, request.beskrivelse, request.faneinnhold)
        queries.gjennomforing.setKontaktpersoner(id, result.kontaktpersoner)
    }

    private fun getValidatorCtx(
        id: UUID,
        avtaleId: UUID,
        request: GjennomforingDetaljerRequest,
        previous: GjennomforingAvtale?,
        today: LocalDate,
    ): GjennomforingValidator.Ctx = db.session {
        val avtale = queries.avtale.getOrError(avtaleId)
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
        val arrangor = request.arrangorId?.let { queries.arrangor.getById(it) }
        val antallDeltakere = queries.deltaker.getByGjennomforingId(id).size
        val status = resolveStatus(previous?.status, request, today)
        return GjennomforingValidator.Ctx(
            previous = previous?.let {
                GjennomforingValidator.Ctx.Gjennomforing(
                    avtaleId = it.avtaleId,
                    oppstart = it.oppstart,
                    arrangorId = it.arrangor.id,
                    status = it.status,
                    sluttDato = it.sluttDato,
                    pameldingType = it.pameldingType,
                    arena = it.arena,
                )
            },
            avtale = avtale,
            arrangor = arrangor,
            administratorer = administratorer,
            antallDeltakere = antallDeltakere,
            status = status,
        )
    }

    private fun resolveStatus(
        previous: GjennomforingStatusType?,
        request: GjennomforingDetaljerRequest,
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

    fun updateFreeTextSearch(id: UUID) = db.transaction {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(id)
        updateFreeTextSearch(gjennomforing)
    }

    private fun QueryContext.updateFreeTextSearch(gjennomforing: GjennomforingAvtale) {
        val fts = listOf(gjennomforing.navn, gjennomforing.arrangor.navn) +
            gjennomforing.lopenummer.toFreeTextSearch() +
            gjennomforing.arena?.tiltaksnummer?.toFreeTextSearch().orEmpty() +
            gjennomforing.tiltakstype.navn

        queries.gjennomforing.setFreeTextSearch(gjennomforing.id, fts)
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingAvtale {
        return queries.gjennomforing.getGjennomforingAvtaleOrError(id)
    }

    // TODO: valider slettede admins her i stedet
    private fun QueryContext.setAdministratorer(
        id: UUID,
        administratorer: Set<NavIdent>,
        navIdent: NavIdent,
        navn: String,
    ) {
        val currentAdministratorer = queries.gjennomforing.getAdministratorer(id).orEmpty().map { it.navIdent }.toSet()

        queries.gjennomforing.setAdministratorer(id, administratorer)

        val administratorsToNotify = (administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull()
            ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på gjennomføringen \"${navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        gjennomforingId: UUID,
        endretAv: Agent,
    ): GjennomforingAvtale {
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

    private fun QueryContext.publishToKafka(gjennomforing: GjennomforingAvtale) {
        val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
        val gjennomforingV2 = TiltaksgjennomforingV2Mapper.fromGjennomforingAvtale(gjennomforing, detaljer)
        val recordV2 = StoredProducerRecord(
            config.gjennomforingV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(TiltaksgjennomforingV2Dto.serializer(), gjennomforingV2).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(recordV2)
    }
}
