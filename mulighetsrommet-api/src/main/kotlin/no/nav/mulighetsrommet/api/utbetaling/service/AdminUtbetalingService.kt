package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingLinjerRequest
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.util.UUID

class AdminUtbetalingService(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
) {
    suspend fun opprettUtbetaling(
        opprett: UpsertUtbetaling,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        if (opprett is UpsertUtbetaling.Anskaffelse && opprett.journalpostId == null) {
            val arrangor = checkNotNull(queries.arrangor.getByGjennomforingId(opprett.gjennomforingId))
            if (!arrangor.erUtenlandsk) {
                return FieldError.of("Journalpost-ID er påkrevd", UpsertUtbetaling.Anskaffelse::journalpostId)
                    .nel()
                    .left()
            }
        }

        utbetalingService.opprettUtbetaling(opprett, agent)
    }

    suspend fun redigerUtbetaling(
        rediger: UpsertUtbetaling,
        agent: Agent,
    ): Validated<Utbetaling> = db.transaction {
        utbetalingService.redigerUtbetaling(rediger, agent)
    }

    fun opprettUtbetalingLinjer(
        request: OpprettUtbetalingLinjerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.opprettUtbetalingLinjer(request, navIdent)
    }

    fun godkjennUtbetalingLinje(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.godkjennUtbetalingLinje(id, navIdent)
    }

    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        utbetalingService.returnerUtbetalingLinje(id, aarsaker, forklaring, navIdent)
    }

    fun slettKorreksjon(id: UUID): Either<List<FieldError>, Unit> = db.transaction {
        utbetalingService.slettKorreksjon(id)
    }

    fun republishFaktura(fakturanummer: String): UtbetalingLinje = db.transaction {
        utbetalingService.republishFaktura(fakturanummer)
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = db.transaction {
        utbetalingService.oppdaterFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)
    }

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.transaction {
        utbetalingService.getAdvarsler(utbetaling)
    }
}
