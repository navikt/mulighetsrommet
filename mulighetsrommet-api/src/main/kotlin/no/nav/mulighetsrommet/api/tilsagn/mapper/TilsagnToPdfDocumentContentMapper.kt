package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.pdfgen.Format
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.model.DataElement

object TilsagnToPdfDocumentContentMapper {
    fun toTilsagnsbrev(
        tilsagn: Tilsagn,
        deltaker: DeltakerPersonalia,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Tilsagnsbrev",
        subject = "Tilsagnsbrev til ${tilsagn.arrangor.navn}",
        description = "Detaljer om tilsagn for gjennomføring av ${tilsagn.tiltakstype.navn}",
        author = "Nav",
    ) {
        mainSection("Detaljer om tilsagn")

        addTilsagnSection(tilsagn)
        addDeltakerSection(deltaker)
    }

    private fun PdfDocumentContentBuilder.addTilsagnSection(tilsagn: Tilsagn) {
        require(tilsagn.beregning is TilsagnBeregningFri) { "Tilsagnsbrev støttes bare for frimodellen" }
        section("Tilsagn") {
            descriptionList {
                text("Tilsagnsperiode", tilsagn.periode.formatPeriode())
            }

            descriptionList {
                money("Beløp", tilsagn.beregning.output.pris)
            }
        }
    }

    private fun PdfDocumentContentBuilder.addDeltakerSection(
        personalia: DeltakerPersonalia,
    ) {
        section("Deltaker") {
            descriptionList {
                text(
                    "Navn",
                    if (personalia.erSkjermet) "Skjermet" else personalia.navn,
                )
                text(
                    "Fødselsnr.",
                    if (personalia.erSkjermet) null else personalia.norskIdent.value,
                )
            }
        }
    }
}

private fun DataElement.Text.Format.toPdfDocumentContentFormat(): Format? = when (this) {
    DataElement.Text.Format.DATE -> Format.DATE
    DataElement.Text.Format.NUMBER -> null
}

private fun DataElement.toPdfDocumentValue(): String? = when (this) {
    is DataElement.Link -> this.text
    is DataElement.MathOperator -> this.operator.toString()
    is DataElement.MultiLinkModal -> this.buttonText
    is DataElement.Periode -> "${this.start} - ${this.slutt}"
    is DataElement.Status -> this.value
    is DataElement.Text -> this.value
    is DataElement.MoneyAmount -> this.value
}
