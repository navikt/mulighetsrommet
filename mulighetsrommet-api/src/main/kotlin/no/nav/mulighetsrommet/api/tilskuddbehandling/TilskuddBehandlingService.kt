package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingHandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.UUID
import kotlin.String

class TilskuddBehandlingService(
    private val db: ApiDatabase,
    private val totrinnskontroll: TotrinnskontrollService,
    private val tilsagnService: TilsagnService,
    private val utbetalingService: UtbetalingService,
) {
    fun upsert(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> {
        return TilskuddBehandlingValidator
            .validate(request)
            .map { dbo ->
                db.transaction {
                    queries.tilskuddBehandling.upsert(dbo)
                    totrinnskontroll.opprett(dbo.id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE, navIdent)
                    logEndring("Sendt til attestering", dbo.id, navIdent)
                }
            }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingKompakt> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
                .map {
                    TilskuddBehandlingKompakt(
                        id = it.id,
                        soknadDato = it.soknadDato,
                        periode = it.periode,
                        journalpostId = it.soknadJournalpostId,
                        tilskuddtyper = it.tilskudd.map { tilskudd -> tilskudd.tilskuddOpplaeringType }
                            .toSet(),
                        kostnadssted = it.kostnadssted,
                        status = it.status,
                    )
                }
        }
    }

    fun getDetaljerDto(id: UUID, navIdent: NavIdent): TilskuddBehandlingDetaljerDto? {
        return db.session {
            val behandling = queries.tilskuddBehandling.get(id)
            behandling?.let {
                TilskuddBehandlingDetaljerDto(
                    it,
                    totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE).toDto(),
                    handlinger(it, navIdent),
                )
            }
        }
    }

    suspend fun godkjenn(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, TilskuddBehandlingDto> = try {
        db.transaction {
            val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
                "TilskuddBehandling med id $id ble ikke funnet"
            }
            if (behandling.status.type !== TilskuddBehandlingStatus.TIL_ATTESTERING) {
                return FieldError
                    .of("Tilskuddsbehandling kan ikke godkjennes fordi det har status ${behandling.status.type.beskrivelse}")
                    .nel()
                    .left()
            }

            val opprettelse = totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)
            totrinnskontroll.godkjent(opprettelse, navIdent)
                .map {
                    queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.FERDIG_BEHANDLET)
                    val dto = logEndring("Tilskuddsbehandling attestert", behandling.id, navIdent)
                    utbetalGodkjenteVedtak(dto).getOrElse {
                        throw UtbetalingException(it)
                    }
                }
        }
    } catch (e: UtbetalingException) {
        e.errors.left()
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilskuddBehandlingStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, TilskuddBehandlingDto> = db.transaction {
        val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
            "TilskuddBehandling med id $id ble ikke funnet"
        }
        if (behandling.status.type !== TilskuddBehandlingStatus.TIL_ATTESTERING) {
            return FieldError
                .of("Tilskuddsbehandling kan ikke returneres fordi det har status ${behandling.status.type.beskrivelse}")
                .nel()
                .left()
        }

        val opprettelse = totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)
        totrinnskontroll.avvist(opprettelse, navIdent, aarsaker.map { it.name }, forklaring).map {
            queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.RETURNERT)
            logEndring("Tilskuddsbehandling returnert", behandling.id, navIdent)
        }
    }

    fun handlinger(behandling: TilskuddBehandlingDto, navIdent: NavIdent): Set<TilskuddBehandlingHandling> = db.session {
        val opprettelse = totrinnskontroll.getOrError(behandling.id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)

        return setOfNotNull(
            TilskuddBehandlingHandling.REDIGER.takeIf { behandling.status.type == TilskuddBehandlingStatus.RETURNERT },
            TilskuddBehandlingHandling.ATTESTER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
            TilskuddBehandlingHandling.RETURNER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
        )
            .filter {
                tilgangTilHandling(
                    handling = it,
                    navIdent = navIdent,
                    kostnadssted = behandling.kostnadssted.enhetsnummer,
                    opprettelse = opprettelse,
                )
            }
            .toSet()
    }

    private suspend fun TransactionalQueryContext.utbetalGodkjenteVedtak(behandling: TilskuddBehandlingDto): Either<List<FieldError>, TilskuddBehandlingDto> = either {
        if (behandling.status.type != TilskuddBehandlingStatus.FERDIG_BEHANDLET) {
            return FieldError
                .of("Kan ikke utbetale fordi tilskuddbehandling er i status: ${behandling.status.type}")
                .nel()
                .left()
        }
        behandling.tilskudd
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .forEach { t ->
                when (t.utbetalingMottaker) {
                    TilskuddMottaker.BRUKER ->
                        return FieldError
                            .of("Utbetaling til bruker er ikke implementert ennå")
                            .nel()
                            .left()

                    TilskuddMottaker.ARRANGOR -> {
                        opprettOgGodkjennTilsagn(
                            gjennomforingId = behandling.gjennomforingId,
                            kostnadssted = behandling.kostnadssted.enhetsnummer,
                            periode = behandling.periode,
                            belop = requireNotNull(t.utbetalingBelop) {
                                "Utbetaling beløp var null ved inngivelse av tilskudd til arrangør"
                            },
                            prisbetingelser = null,
                        )
                            .flatMap {
                                opprettOgBetalUtbetaling(
                                    gjennomforingId = behandling.gjennomforingId,
                                    periode = behandling.periode,
                                    belop = t.utbetalingBelop,
                                    kid = t.kid,
                                )
                            }
                            .bind()
                    }
                }
            }

        return behandling.right()
    }

    private fun TransactionalQueryContext.opprettOgGodkjennTilsagn(
        gjennomforingId: UUID,
        kostnadssted: NavEnhetNummer,
        periode: Periode,
        belop: ValutaBelop,
        prisbetingelser: String?,
    ): Either<List<FieldError>, Tilsagn> {
        return tilsagnService.upsertInTx(
            TilsagnRequest(
                id = UUID.randomUUID(),
                type = TilsagnType.TILSAGN,
                gjennomforingId = gjennomforingId,
                kostnadssted = kostnadssted,
                beregning = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    valuta = belop.valuta,
                    prisbetingelser = prisbetingelser,
                    linjer = listOf(
                        TilsagnInputLinjeRequest(
                            id = UUID.randomUUID(),
                            // TODO: Beskrivelse
                            beskrivelse = "Automatisk tilsagn for opplæringstilskudd",
                            pris = belop,
                            antall = 1,
                        ),
                    ),
                ),
                kommentar = null,
                beskrivelse = null,
                periodeStart = periode.start.toString(),
                periodeSlutt = periode.getLastInclusiveDate().toString(),
                // TODO: Er det vits i å lagre deltakeren på tilsagn her?
                deltakere = emptyList(),
            ),
            Tiltaksadministrasjon,
        )
            .flatMap {
                tilsagnService.godkjennTilsagnInTx(it.id, Tiltaksadministrasjon)
            }
    }

    private suspend fun TransactionalQueryContext.opprettOgBetalUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
        belop: ValutaBelop,
        kid: Kid?,
    ): Either<List<FieldError>, AutomatiskUtbetalingResult> {
        return utbetalingService.opprettUtbetalingInTx(
            UpsertUtbetaling.Generering(
                id = UUID.randomUUID(),
                periode = periode,
                gjennomforingId = gjennomforingId,
                beregning = UtbetalingBeregningFri.from(belop),
                tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                kid = kid,
                blokkeringer = emptySet(),

            ),
            Tiltaksadministrasjon,
        )
            .map {
                when (utbetalingService.automatiskUtbetaling(it.id)) {
                    AutomatiskUtbetalingResult.GODKJENT -> AutomatiskUtbetalingResult.GODKJENT
                    else -> throw UtbetalingException(FieldError.of("Feil ved automatisk utbetaling til arrangør: $it").nel())
                }
            }
    }

    fun tilgangTilHandling(
        handling: TilskuddBehandlingHandling,
        navIdent: NavIdent,
        kostnadssted: NavEnhetNummer,
        opprettelse: Totrinnskontroll,
    ): Boolean {
        val ansatt = db.session { queries.ansatt.getByNavIdent(navIdent) }
            ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

        val attestant = ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted))
        val saksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

        return when (handling) {
            TilskuddBehandlingHandling.REDIGER,
            -> saksbehandler

            TilskuddBehandlingHandling.RETURNER,
            -> attestant

            TilskuddBehandlingHandling.ATTESTER -> {
                attestant && opprettelse.behandletAv != ansatt.navIdent
            }
        }
    }

    private fun QueryContext.logEndring(
        operation: String,
        id: UUID,
        endretAv: Agent,
    ): TilskuddBehandlingDto {
        val behandling = queries.tilskuddBehandling.getOrError(id)
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILSKUDD_BEHANDLING,
            operation,
            endretAv,
            id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(behandling)
        }
        return behandling
    }
}
