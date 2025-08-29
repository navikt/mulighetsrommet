package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilsagnRoutesGetAll() {
    val db: ApiDatabase by inject()

    get {
        val gjennomforingId: UUID? by call.queryParameters
        val status = call.queryParameters.getAll("statuser")
            ?.map { TilsagnStatus.valueOf(it) }

        val tilsagn = db.session {
            queries.tilsagn.getAll(gjennomforingId = gjennomforingId, statuser = status)
        }
        val table = toTilsagnDataTable(tilsagn)

        call.respond(table)
    }
}

private fun toTilsagnDataTable(tilsagn: List<Tilsagn>): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = listOf(
            DataDrivenTableDto.Column("bestillingsnummer", "Tilsagnsnummer"),
            DataDrivenTableDto.Column("periodeStart", "Periodestart"),
            DataDrivenTableDto.Column("periodeSlutt", "Periodeslutt"),
            DataDrivenTableDto.Column("type", "Tilsagntype"),
            DataDrivenTableDto.Column("kostnadssted", "Kostnadssted"),
            DataDrivenTableDto.Column(
                "antallPlasser",
                "Antall plasser",
                align = DataDrivenTableDto.Column.Align.RIGHT,
            ),
            DataDrivenTableDto.Column("belop", "Totalbeløp", align = DataDrivenTableDto.Column.Align.RIGHT),
            DataDrivenTableDto.Column("status", "Status", align = DataDrivenTableDto.Column.Align.RIGHT),
            DataDrivenTableDto.Column("action", null, sortable = false),
        ),
        rows = tilsagn.map { tilsagn ->
            mapOf(
                "bestillingsnummer" to DataElement.text(tilsagn.bestilling.bestillingsnummer),
                "periodeStart" to DataElement.date(tilsagn.periode.start),
                "periodeSlutt" to DataElement.date(tilsagn.periode.getLastInclusiveDate()),
                "type" to DataElement.text(tilsagn.type.displayName()),
                "kostnadssted" to DataElement.text(tilsagn.kostnadssted.navn),
                "antallPlasser" to when (tilsagn.beregning) {
                    is TilsagnBeregningFri -> null
                    is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> DataElement.number(tilsagn.beregning.input.antallPlasser)
                    is TilsagnBeregningPrisPerManedsverk -> DataElement.number(tilsagn.beregning.input.antallPlasser)
                    is TilsagnBeregningPrisPerUkesverk -> DataElement.number(tilsagn.beregning.input.antallPlasser)
                    is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> DataElement.number(tilsagn.beregning.input.antallPlasser)
                },
                "belop" to DataElement.nok(tilsagn.beregning.output.belop),
                "status" to toTilsagnStatusTag(tilsagn.status),
                "action" to toTilsagnAction(tilsagn),
            )
        },
    )
}

private fun toTilsagnAction(tilsagn: Tilsagn): DataElement.Link {
    val text = when (tilsagn.status) {
        TilsagnStatus.GODKJENT,
        TilsagnStatus.OPPGJORT,
        TilsagnStatus.ANNULLERT,
        -> "Detaljer"

        TilsagnStatus.TIL_GODKJENNING,
        TilsagnStatus.RETURNERT,
        TilsagnStatus.TIL_ANNULLERING,
        TilsagnStatus.TIL_OPPGJOR,
        -> "Behandle"
    }
    return DataElement.Link(
        text = text,
        href = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
    )
}

private fun toTilsagnStatusTag(status: TilsagnStatus): DataElement.Status {
    val variant = when (status) {
        TilsagnStatus.TIL_GODKJENNING -> DataElement.Status.Variant.WARNING
        TilsagnStatus.GODKJENT -> DataElement.Status.Variant.SUCCESS
        TilsagnStatus.RETURNERT -> DataElement.Status.Variant.ERROR
        TilsagnStatus.TIL_ANNULLERING -> DataElement.Status.Variant.ERROR_BORDER
        TilsagnStatus.ANNULLERT -> DataElement.Status.Variant.ERROR_BORDER_STRIKETHROUGH
        TilsagnStatus.TIL_OPPGJOR -> DataElement.Status.Variant.ERROR_BORDER
        TilsagnStatus.OPPGJORT -> DataElement.Status.Variant.NEUTRAL
    }
    return DataElement.Status(status.navn(), variant)
}
