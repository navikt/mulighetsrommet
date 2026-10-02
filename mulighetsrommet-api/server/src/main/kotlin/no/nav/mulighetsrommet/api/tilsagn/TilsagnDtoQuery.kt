package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptySetOf
import arrow.core.right
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDeltakerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDetaljerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class TilsagnDtoQuery(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
) {
    suspend fun getDetaljer(
        id: UUID,
        navIdent: NavIdent,
        onBehalfOf: PersonaliaService.OnBehalfOf.NavAnsatt,
    ): Either<TilsagnDtoQueryError, TilsagnDetaljerDto> {
        val tilsagn = db.session { queries.tilsagn.get(id) }
            ?: return TilsagnDtoQueryError.IkkeFunnet("Tilsagn med id=$id finnes ikke").left()

        val ansatt = db.session { queries.ansatt.getOrError(navIdent) }
        if (!OkonomiAuthorization.kanLeseTilsagn(ansatt, tilsagn.gjennomforing.type)) {
            return TilsagnDtoQueryError.ManglerTilgang("Du mangler lesetilgang til tilsagnet").left()
        }

        val personalia = personaliaService.getPersonalia(tilsagn.deltakere.map { it.deltakerId }, onBehalfOf)

        return db.session {
            val opprettelse = queries.totrinnskontroll.getDtoOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            val annullering = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_ANNULLERING)
            val tilOppgjor = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_OPPGJOR)
            TilsagnDetaljerDto(
                tilsagn = TilsagnDto.from(tilsagn),
                beregning = TilsagnBeregningDto.from(tilsagn.beregning),
                opprettelse = opprettelse,
                annullering = annullering,
                tilOppgjor = tilOppgjor,
                handlinger = handlinger(tilsagn, ansatt, opprettelse, annullering, tilOppgjor),
                deltakere = tilsagn.deltakere.map {
                    TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                },
            ).right()
        }
    }

    private fun handlinger(
        tilsagn: Tilsagn,
        ansatt: NavAnsatt,
        opprettelse: TotrinnskontrollDto,
        annullering: TotrinnskontrollDto?,
        tilOppgjor: TotrinnskontrollDto?,
    ): Set<TilsagnHandling> {
        val erSaksbehandler = OkonomiAuthorization.erSaksbehandler(ansatt, tilsagn.gjennomforing.type)
        val erBeslutter = OkonomiAuthorization.erBeslutterTilsagn(
            ansatt,
            OkonomiBeslutningContext(
                tilsagn.gjennomforing.type,
                nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
            ),
        )
        return when (tilsagn.status) {
            TilsagnStatus.RETURNERT -> setOfNotNull(
                TilsagnHandling.REDIGER.takeIf { erSaksbehandler },
                TilsagnHandling.SLETT.takeIf { erSaksbehandler },
            )

            TilsagnStatus.GODKJENT -> setOfNotNull(
                TilsagnHandling.ANNULLER.takeIf { erSaksbehandler && tilsagn.belopBrukt.belop == 0 },
                TilsagnHandling.GJOR_OPP.takeIf { erSaksbehandler && tilsagn.belopBrukt.belop > 0 },
            )

            TilsagnStatus.TIL_GODKJENNING -> setOfNotNull(
                TilsagnHandling.GODKJENN.takeIf {
                    erBeslutter && opprettelse.behandling.utfortAv.agent != ansatt.navIdent
                },
                TilsagnHandling.RETURNER.takeIf { erSaksbehandler || erBeslutter },
            )

            TilsagnStatus.TIL_ANNULLERING -> setOfNotNull(
                TilsagnHandling.GODKJENN_ANNULLERING.takeIf {
                    erBeslutter && annullering?.behandling?.utfortAv?.agent != ansatt.navIdent
                },
                TilsagnHandling.AVSLA_ANNULLERING.takeIf { erSaksbehandler || erBeslutter },
            )

            TilsagnStatus.TIL_OPPGJOR -> setOfNotNull(
                TilsagnHandling.GODKJENN_OPPGJOR.takeIf {
                    erBeslutter && tilOppgjor?.behandling?.utfortAv?.agent != ansatt.navIdent
                },
                TilsagnHandling.AVSLA_OPPGJOR.takeIf { erSaksbehandler || erBeslutter },
            )

            TilsagnStatus.ANNULLERT, TilsagnStatus.OPPGJORT -> emptySet()
        }
    }
}
