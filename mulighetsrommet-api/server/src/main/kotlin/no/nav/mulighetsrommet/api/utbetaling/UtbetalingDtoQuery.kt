package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.nonEmptySetOf
import arrow.core.raise.context.Raise
import arrow.core.raise.context.either
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorizationError
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.domain.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDeltakerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.api.BeregningFilter
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingDetaljerDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeStatusDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarselDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.hentDeltakerAdvarslerForUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

class UtbetalingDtoQuery(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
) {
    fun getDetaljer(id: UUID, navIdent: NavIdent): Either<UtbetalingDtoQueryError, UtbetalingDetaljerDto> = db.session {
        either {
            val utbetaling = queries.utbetaling.getOrError(id)
            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforing.id)
            val ansatt = queries.ansatt.getOrError(navIdent)

            harLesetilgang(utbetaling, ansatt)

            val linjer = queries.utbetalingLinje.getByUtbetalingId(id)
            val avbrytelse = utbetaling.avbrytelse?.totrinnskontroll?.let {
                queries.totrinnskontroll.getDtoByIdOrError(it.id)
            }
            val dto = UtbetalingDto.fromUtbetaling(
                utbetaling = utbetaling,
                linjer = linjer,
                avbrytelse = avbrytelse,
            )

            val handlinger =
                utbetalingHandlinger(utbetaling, ansatt, avbrytelse, gjennomforing.ansvarligEnhet.enhetsnummer)

            UtbetalingDetaljerDto(utbetaling = dto, handlinger = handlinger)
        }
    }

    suspend fun getLinjer(
        id: UUID,
        navIdent: NavIdent,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): Either<UtbetalingDtoQueryError, List<UtbetalingLinjeDto>> = db.session {
        either {
            val utbetaling = queries.utbetaling.getOrError(id)
            val ansatt = queries.ansatt.getOrError(navIdent)

            harLesetilgang(utbetaling, ansatt)

            val linjer = queries.utbetalingLinje.getByUtbetalingId(id).map { linje ->
                val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId)

                val opprettelse = queries.totrinnskontroll
                    .getDtoOrError(linje.id, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE)

                val personalia = personaliaService.getPersonalia(
                    tilsagn.deltakere.map { it.deltakerId },
                    onBehalfOf,
                )
                val deltakere = tilsagn.deltakere.map {
                    TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                }
                UtbetalingLinjeDto(
                    id = linje.id,
                    gjorOppTilsagn = linje.gjorOppTilsagn,
                    pris = linje.pris,
                    status = UtbetalingLinjeStatusDto.fromUtbetalingLinjeStatus(linje.status),
                    tilsagn = TilsagnDto.from(tilsagn),
                    deltakere = deltakere,
                    opprettelse = opprettelse,
                    handlinger = linjeHandlinger(
                        linje = linje,
                        ansatt = ansatt,
                        behandletAv = opprettelse.behandling.utfortAv.agent,
                        kontekst = OkonomiBeslutningContext(
                            utbetaling.gjennomforing.type,
                            nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
                        ),
                    ),
                )
            }

            val nyeLinjer = queries.tilsagn
                .getAll(
                    statuser = listOf(TilsagnStatus.GODKJENT),
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periodeIntersectsWith = utbetaling.periode,
                    typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                    valuta = utbetaling.valuta,
                )
                .filter { tilsagn -> linjer.none { it.tilsagn.id == tilsagn.id } }
                .map { tilsagn ->
                    val deltakerIds = tilsagn.deltakere.map { it.deltakerId }
                    val personalia = personaliaService.getPersonalia(deltakerIds, onBehalfOf)
                    val deltakere = tilsagn.deltakere.map {
                        TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                    }

                    UtbetalingLinjeDto(
                        id = UUID.randomUUID(),
                        tilsagn = TilsagnDto.from(tilsagn),
                        deltakere = deltakere,
                        status = null,
                        pris = ValutaBelop(0, utbetaling.valuta),
                        gjorOppTilsagn = false,
                        opprettelse = null,
                        handlinger = emptySet(),
                    )
                }

            (linjer + nyeLinjer).sortedBy { it.tilsagn.bestillingsnummer }
        }
    }

    suspend fun getBeregning(
        id: UUID,
        navIdent: NavIdent,
        filter: BeregningFilter,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): Either<UtbetalingDtoQueryError, UtbetalingBeregningDto> = db.session {
        either {
            val utbetaling = queries.utbetaling.getOrError(id)

            harLesetilgang(utbetaling, queries.ansatt.getOrError(navIdent))

            val deltakelser = utbetaling.beregning.deltakelsePerioder().associateBy { it.deltakelseId }
            val personalia = personaliaService.getPersonalia(deltakelser.keys.toList(), onBehalfOf)

            val enheter = personalia.flatMap {
                listOfNotNull(it.oppfolgingEnhet(), it.region())
            }
            val kontorstruktur = Kontorstruktur.fromNavEnheter(enheter)

            val deltakelsePersoner = personalia
                .filter { filter.navEnheter.isEmpty() || it.oppfolgingEnhet()?.enhetsnummer in filter.navEnheter }
                .associateBy { it.deltakerId }

            val advarsler = hentDeltakerAdvarslerForUtbetaling(
                status = utbetaling.status,
                gjennomforingId = utbetaling.gjennomforing.id,
                periode = utbetaling.periode,
                beregning = utbetaling.beregning,
            )

            UtbetalingBeregningDto.from(
                utbetaling.beregning,
                deltakelsePersoner,
                kontorstruktur,
                utbetalingPeriode = utbetaling.periode,
                advarsler = advarsler.map { advarsel ->
                    DeltakerAdvarselDto.from(
                        advarsel,
                        deltakelsePersoner[advarsel.deltakerId]?.navn(),
                        deltakelsePersoner[advarsel.deltakerId]?.norskIdent(),
                    )
                },
            )
        }
    }

    private fun utbetalingHandlinger(
        utbetaling: Utbetaling,
        ansatt: NavAnsatt,
        avbrytelse: TotrinnskontrollDto?,
        ansvarligEnhet: NavEnhetNummer,
    ): Set<UtbetalingHandling> {
        val erSaksbehandler = OkonomiAuthorization.erSaksbehandler(ansatt, utbetaling.gjennomforing.type).isRight()
        val erAttestant = OkonomiAuthorization.erAttestantUtbetaling(
            ansatt,
            OkonomiBeslutningContext(
                gjennomforingType = utbetaling.gjennomforing.type,
                kostnadssteder = nonEmptySetOf(ansvarligEnhet),
            ),
        ).isRight()
        val tilAvbrytelse = avbrytelse is TotrinnskontrollDto.TilBeslutning
        return setOfNotNull(
            UtbetalingHandling.SEND_TIL_GODKJENNING.takeIf { erSaksbehandler && utbetaling.erTilBehandling() },
            UtbetalingHandling.SLETT.takeIf { erSaksbehandler && utbetaling.kanSlettes() },
            UtbetalingHandling.OPPRETT_KORREKSJON.takeIf {
                erSaksbehandler && utbetaling.erFerdigBehandlet() && !utbetaling.erKorreksjon()
            },
            UtbetalingHandling.REDIGER.takeIf { erSaksbehandler && utbetaling.kanRedigeres() },
            UtbetalingHandling.HENT_GODKJENTE_TILSAGN.takeIf { erSaksbehandler && utbetaling.erTilBehandling() },
            UtbetalingHandling.OPPRETT_TILSAGN.takeIf { erSaksbehandler && utbetaling.erTilBehandling() },
            UtbetalingHandling.SEND_TIL_AVBRYTELSE.takeIf { erSaksbehandler && utbetaling.kanSettesTilAvbrytelse() },
            UtbetalingHandling.GODKJENN_AVBRYTELSE.takeIf {
                tilAvbrytelse && erAttestant && avbrytelse.behandling.utfortAv.agent != ansatt.navIdent
            },
            UtbetalingHandling.AVSLA_AVBRYTELSE.takeIf { erSaksbehandler && tilAvbrytelse },
        )
    }

    private fun linjeHandlinger(
        linje: UtbetalingLinje,
        ansatt: NavAnsatt,
        behandletAv: Agent,
        kontekst: OkonomiBeslutningContext,
    ): Set<UtbetalingLinjeHandling> {
        if (linje.status != UtbetalingLinjeStatus.TIL_GODKJENNING) {
            return emptySet()
        }

        val erBeslutter = OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst).isRight()
        val erSaksbehandler = OkonomiAuthorization.erSaksbehandler(ansatt, kontekst.gjennomforingType).isRight()
        return setOfNotNull(
            UtbetalingLinjeHandling.GODKJENN.takeIf { erBeslutter && behandletAv != ansatt.navIdent },
            UtbetalingLinjeHandling.RETURNER.takeIf { erBeslutter || erSaksbehandler },
        )
    }

    context(errors: Raise<UtbetalingDtoQueryError>)
    private fun harLesetilgang(utbetaling: Utbetaling, ansatt: NavAnsatt) {
        OkonomiAuthorization.kanLeseUtbetaling(ansatt, utbetaling.gjennomforing.type).onLeft {
            errors.raise(it.toUtbetalingDtoQueryError(ansatt.navIdent))
        }
    }

    private fun OkonomiAuthorizationError.toUtbetalingDtoQueryError(navIdent: NavIdent): UtbetalingDtoQueryError = when (this) {
        is OkonomiAuthorizationError.ManglerTilgang -> UtbetalingDtoQueryError.ManglerTilgang("Du mangler tilgang til utbetalingen", navIdent, requiredRoles)
        is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> UtbetalingDtoQueryError.ManglerTilgang("Du mangler kontorspesifikk tilgagn til utbetalingen", navIdent, requiredRoles)
    }
}
