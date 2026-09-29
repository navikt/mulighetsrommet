package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDeltakerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDetaljerDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class TilsagnDtoQuery(
    private val db: ApiDatabase,
    private val tilsagnService: TilsagnService,
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
            TilsagnDetaljerDto(
                tilsagn = TilsagnDto.from(tilsagn),
                beregning = TilsagnBeregningDto.from(tilsagn.beregning),
                opprettelse = queries.totrinnskontroll.getDtoOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE),
                annullering = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_ANNULLERING),
                tilOppgjor = queries.totrinnskontroll.getDto(id, TotrinnskontrollType.TILSAGN_OPPGJOR),
                handlinger = tilsagnService.handlinger(tilsagn, ansatt),
                deltakere = tilsagn.deltakere.map {
                    TilsagnDeltakerDto.from(it, personalia.find { p -> p.deltakerId == it.deltakerId })
                },
            ).right()
        }
    }
}
