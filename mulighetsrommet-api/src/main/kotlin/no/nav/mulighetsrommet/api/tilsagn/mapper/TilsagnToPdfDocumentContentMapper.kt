package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.Regards
import no.nav.mulighetsrommet.api.pdfgen.SectionBuilder
import no.nav.mulighetsrommet.api.pdfgen.TopSection
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.ValutaBelop
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

object TilsagnToPdfDocumentContentMapper {
    fun toTilsagnsbrev(
        tilsagn: Tilsagn,
        kontonummer: Kontonummer,
        deltaker: DeltakerPersonalia,
        behandlere: List<String> = emptyList(),
        referanseDato: LocalDate = LocalDate.now(),
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Tilsagnsbrev",
        subject = "Tilsagnsbrev til ${tilsagn.arrangor.navn}",
        description = "Detaljer om tilsagn for gjennomføring av ${tilsagn.tiltakstype.navn}",
        author = "Nav",
    ) {
        topSection(
            TopSection(
                publicExemption = true,
                addressedTo = "Brev til ${tilsagn.arrangor.navn}",
                date = referanseDato.toString(),
                reference = "Ref. ${tilsagn.bestilling.bestillingsnummer}",
            ),
        )

        mainSection("Bekreftelse på bestilling") {
            paragraph { regular("Nav og dere har blitt enige om dette:") }
            descriptionList {
                text(
                    "Tiltaket",
                    tilsagn.gjennomforing.navn,
                )
                text("Deltakeren", "${deltaker.navn} (${deltaker.norskIdent.value})")
                text("Utbetalingsperioden", tilsagn.periode.formatPeriode())
                text("Støtten fra Nav", "Opptil ${formatCurrency(tilsagn.beregning.output.pris)}")
            }
        }

        section("Hvordan kan dere få utbetalt pengene?") {
            addInvoiceInfo()
            paragraph { regular("Vi kan kontrollere om pengene som blir utbetalt blir brukt riktig.") }
            paragraph { regular("Følgende informasjon er registrert hos NAV:") }
            descriptionList {
                text("Bedriftsnummer", tilsagn.arrangor.organisasjonsnummer)
                text("Kontonummer", kontonummer)
            }
            paragraph {
                regular("Hvis kontonummeret er feil, må dere oppdatere det via Navs hjemmeside under ")
                bold("Arbeidsgiver")
                regular(" og ")
                bold("Endre kontonummer")
                regular(".")
            }
        }

        regards(
            Regards(
                "Hilsen",
                "Nav Arbeidsmarkedstiltak",
                behandlere,
            ),
        )
    }

    private fun SectionBuilder.addInvoiceInfo() {
        paragraph {
            regular("Gå inn på Navs hjemmesider, velg ")
            bold("Samarbeidspartner")
            regular(", ")
            bold("Tiltaksarrangør")
            regular(" og ")
            bold("Skjema og søknad")
            regular(". Velg så ")
            bold("Opplæring")
            regular(" og ")
            bold("Faktura")
            regular(". Send inn faktura til Nav med førsteside.")
        }
    }
}

private fun formatCurrency(pris: ValutaBelop): String {
    val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("no-NO"))
    return "${formatter.format(pris.belop)} ${pris.valuta.name}"
}
