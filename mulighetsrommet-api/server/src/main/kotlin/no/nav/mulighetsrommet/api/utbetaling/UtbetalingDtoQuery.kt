package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptySetOf
import arrow.core.right
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.navansatt.OkonomiBeslutningContext
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
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

class UtbetalingDtoQuery(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
) {
    fun getDetaljer(id: UUID, navIdent: NavIdent): Either<UtbetalingDtoQueryError, UtbetalingDetaljerDto> = db.session {
        val utbetaling = queries.utbetaling.getOrError(id)
        val ansatt = queries.ansatt.getOrError(navIdent)
        if (!harLesetilgang(utbetaling, ansatt)) {
            return manglerLesetilgang()
        }

        val linjer = queries.utbetalingLinje.getByUtbetalingId(id)
        val avbrytelse = utbetaling.avbrytelse?.totrinnskontroll?.let {
            queries.totrinnskontroll.getDtoByIdOrError(it.id)
        }
        val dto = UtbetalingDto.fromUtbetaling(
            utbetaling = utbetaling,
            linjer = linjer,
            avbrytelse = avbrytelse,
        )

        val handlinger = utbetalingHandlinger(utbetaling, ansatt, avbrytelse)

        return UtbetalingDetaljerDto(utbetaling = dto, handlinger = handlinger).right()
    }

    suspend fun getLinjer(
        id: UUID,
        navIdent: NavIdent,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): Either<UtbetalingDtoQueryError, List<UtbetalingLinjeDto>> = db.session {
        val utbetaling = queries.utbetaling.getOrError(id)
        val ansatt = queries.ansatt.getOrError(navIdent)
        if (!harLesetilgang(utbetaling, ansatt)) {
            return manglerLesetilgang()
        }

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

        return (linjer + nyeLinjer).sortedBy { it.tilsagn.bestillingsnummer }.right()
    }

    suspend fun getBeregning(
        id: UUID,
        navIdent: NavIdent,
        filter: BeregningFilter,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): Either<UtbetalingDtoQueryError, UtbetalingBeregningDto> = db.session {
        val utbetaling = queries.utbetaling.getOrError(id)
        if (!harLesetilgang(utbetaling, queries.ansatt.getOrError(navIdent))) {
            return manglerLesetilgang()
        }

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

        return UtbetalingBeregningDto.from(
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
        ).right()
    }

    private fun utbetalingHandlinger(
        utbetaling: Utbetaling,
        ansatt: NavAnsatt,
        avbrytelse: TotrinnskontrollDto?,
    ): Set<UtbetalingHandling> {
        if (!OkonomiAuthorization.erSaksbehandler(ansatt, utbetaling.gjennomforing.type)) {
            return emptySet()
        }

        val tilAvbrytelse = avbrytelse is TotrinnskontrollDto.TilBeslutning
        return setOfNotNull(
            UtbetalingHandling.SEND_TIL_ATTESTERING.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.SLETT.takeIf { utbetaling.kanSlettes() },
            UtbetalingHandling.OPPRETT_KORREKSJON.takeIf { utbetaling.erFerdigBehandlet() && !utbetaling.erKorreksjon() },
            UtbetalingHandling.REDIGER.takeIf { utbetaling.kanRedigeres() },
            UtbetalingHandling.HENT_GODKJENTE_TILSAGN.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.OPPRETT_TILSAGN.takeIf { utbetaling.erTilBehandling() },
            UtbetalingHandling.SEND_TIL_AVBRYTELSE.takeIf { utbetaling.kanSettesTilAvbrytelse() },
            UtbetalingHandling.GODKJENN_AVBRYTELSE.takeIf {
                tilAvbrytelse && avbrytelse.behandling.utfortAv.agent != ansatt.navIdent
            },
            UtbetalingHandling.AVSLA_AVBRYTELSE.takeIf { tilAvbrytelse },
        )
    }

    private fun linjeHandlinger(
        linje: UtbetalingLinje,
        ansatt: NavAnsatt,
        behandletAv: Agent,
        kontekst: OkonomiBeslutningContext,
    ): Set<UtbetalingLinjeHandling> {
        if (linje.status != UtbetalingLinjeStatus.TIL_ATTESTERING) {
            return emptySet()
        }

        val erAttestant = OkonomiAuthorization.erAttestantUtbetaling(ansatt, kontekst)
        val erSaksbehandler = OkonomiAuthorization.erSaksbehandler(ansatt, kontekst.gjennomforingType)
        return setOfNotNull(
            UtbetalingLinjeHandling.ATTESTER.takeIf { erAttestant && behandletAv != ansatt.navIdent },
            UtbetalingLinjeHandling.RETURNER.takeIf { erAttestant || erSaksbehandler },
        )
    }

    private fun harLesetilgang(utbetaling: Utbetaling, ansatt: NavAnsatt): Boolean {
        return OkonomiAuthorization.kanLeseUtbetaling(ansatt, utbetaling.gjennomforing.type)
    }

    private fun manglerLesetilgang() = UtbetalingDtoQueryError.ManglerTilgang("Du mangler lesetilgang til utbetalingen").left()
}
