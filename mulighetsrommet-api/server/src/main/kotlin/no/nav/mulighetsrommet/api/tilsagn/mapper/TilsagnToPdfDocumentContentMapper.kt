package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.Signature
import no.nav.mulighetsrommet.api.pdfgen.TopSection
import no.nav.mulighetsrommet.api.tilsagn.task.TilsagnsbrevInnhold
import no.nav.mulighetsrommet.model.ValutaBelop
import java.text.NumberFormat
import java.util.Locale

object TilsagnToPdfDocumentContentMapper {
    fun toTilsagnsbrev(
        innhold: TilsagnsbrevInnhold,
        visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson: Boolean = false,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Bekreftelse på bestilling",
        subject = "Bekreftelsebrev til ${innhold.arrangor.navn}",
        description = "Detaljer om tilsagn for gjennomføring av ${innhold.tilsagn.tiltakstype.navn}",
        author = "Nav",
    ) {
        topSection(
            TopSection(
                publicExemption = true,
                addressedTo = innhold.arrangor.navn,
                date = innhold.besluttetTidspunkt.toLocalDate().toString(),
                reference = innhold.tilsagn.gjennomforing.lopenummer.value,
            ),
        )

        mainSection("Bekreftelse på bestilling")

        section("Nav bekrefter bestilling av følgende:") {
            descriptionList {
                text(
                    "Tiltaket",
                    innhold.tilsagn.gjennomforing.navn,
                )
                text(
                    "Deltakeren",
                    when {
                        visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson -> formaterDeltakerPersonalia(innhold.personalia)

                        innhold.personalia.gradering == Gradering.SKJERMING -> "Skjermet"

                        innhold.personalia.gradering in setOf(
                            Gradering.STRENGT_FORTROLIG_UTLAND,
                            Gradering.STRENGT_FORTROLIG_ADRESSE,
                            Gradering.FORTROLIG_ADRESSE,
                        ) -> "Adressebeskyttet"

                        else -> formaterDeltakerPersonalia(innhold.personalia)
                    },
                )
                text("Utbetalingsperioden", innhold.tilsagn.periode.formatPeriode())
                text("Avtalt pris", formatCurrency(innhold.tilsagn.beregning.output.pris))
            }
        }

        section("Hvordan kan dere få utbetalt pengene?")
        {
            paragraph { regular("Dere må sende inn faktura og eventuelle vedlegg med førsteside til Nav.") }
            paragraph { regular("Førstesiden finner dere på nav.no/samarbeidspartner/soknader/tiltaksarrangor.") }
        }
        section("Fakturaen skal inneholde følgende informasjon:", level = 3)
        {
            itemList {
                item("Saksnummer, som dere finner øverst i dette brevet")
                item("Navnet på kurset eller utdanningen")
                item("Gjennomføringsperioden")
                item("30 dagers kredittid, hvis ikke annet er avtalt")
                item("Pris som faktureres")
            }
        }

        section("Følgende informasjon er registrert hos Nav:", level = 3) {
            descriptionList {
                text("Bedriftsnummer", innhold.arrangor.organisasjonsnummer)
                text("Kontonummer", innhold.kontonummer)
            }
            paragraph {
                regular("Hvis kontonummeret er feil eller mangler, må dere oppdatere det på nav.no/arbeidsgiver/endre-kontonummer.")
            }
        }

        signature(
            Signature(
                saksbehandler = innhold.saksbehandler,
                beslutter = innhold.beslutter,
                enhet = innhold.tilsagn.kostnadssted.navn,
            ),
        )
    }

    private fun formaterDeltakerPersonalia(personalia: Personalia): String {
        return personalia.norskIdent()?.let { "${personalia.navn()} (${it.value})" } ?: personalia.navn()
    }
}

private fun formatCurrency(pris: ValutaBelop): String {
    val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("no-NO"))
    return "${formatter.format(pris.belop)} ${pris.valuta.name}"
}
