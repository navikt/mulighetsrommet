package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.api.pdfgen.Deltaker
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.SectionBuilder
import no.nav.mulighetsrommet.api.pdfgen.Signature
import no.nav.mulighetsrommet.api.pdfgen.TopSection
import no.nav.mulighetsrommet.api.tilsagn.task.TilsagnsbrevInnhold
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ValutaBelop
import java.text.NumberFormat
import java.util.Locale

object TilsagnToPdfDocumentContentMapper {
    fun toTilsagnsbrev(
        innhold: TilsagnsbrevInnhold,
        visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson: Boolean = false,
    ): PdfDocumentContent = PdfDocumentContent.create(
        title = "Tilsagnsbrev",
        subject = "Tilsagnsbrev til ${innhold.arrangor.navn}",
        description = "Detaljer om tilsagn for gjennomføring av ${innhold.tilsagn.tiltakstype.navn}",
        author = "Nav",
    ) {
        topSection(
            TopSection(
                publicExemption = true,
                addressedTo = "Brev til ${innhold.arrangor.navn}",
                date = innhold.besluttetTidspunkt.toLocalDate().toString(),
                reference = "Ref. ${innhold.tilsagn.bestilling.bestillingsnummer}",
                deltaker =
                when {
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson -> Deltaker(
                        navn = innhold.personalia.navn(),
                        norskIdent = innhold.personalia.norskIdent()?.value,
                    )

                    innhold.personalia.gradering == Gradering.SKJERMING -> Deltaker("Skjermet")

                    innhold.personalia.gradering in setOf(
                        Gradering.STRENGT_FORTROLIG_UTLAND,
                        Gradering.STRENGT_FORTROLIG_ADRESSE,
                        Gradering.FORTROLIG_ADRESSE,
                    ) -> Deltaker("Adressebeskyttet")

                    else -> Deltaker(
                        navn = innhold.personalia.navn(),
                        norskIdent = innhold.personalia.norskIdent()?.value,
                    )
                },
            ),
        )

        mainSection("Bekreftelse på bestilling") {
            paragraph { regular("Nav og dere har blitt enige om dette:") }
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
                text("Støtten fra Nav", "Opptil ${formatCurrency(innhold.tilsagn.beregning.output.pris)}")
            }
        }

        section("Hvordan kan dere få utbetalt pengene?")
        {
            addInvoiceInfo()
            paragraph { regular("Vi kan kontrollere om pengene som blir utbetalt blir brukt riktig.") }
            paragraph { regular("Følgende informasjon er registrert hos Nav:") }
            descriptionList {
                text("Bedriftsnummer", innhold.arrangor.organisasjonsnummer)
                text("Kontonummer", innhold.kontonummer)
            }
            paragraph {
                regular("Hvis kontonummeret er feil, må dere oppdatere det via Navs hjemmeside under ")
                bold("Arbeidsgiver")
                regular(" og ")
                bold("Endre kontonummer")
                regular(".")
            }
        }

        signature(
            Signature(
                saksbehandler = innhold.saksbehandler.personNavn(),
                beslutter = innhold.beslutter.personNavn(),
                enhet = innhold.tilsagn.kostnadssted.navn,
            ),
        )
    }

    private fun formaterDeltakerPersonalia(personalia: Personalia): String {
        return personalia.norskIdent()?.let { "${personalia.navn()} (${it.value})" } ?: personalia.navn()
    }

    private fun AgentDto.personNavn(): String? = when (agent) {
        is NavIdent -> navn
        else -> null
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
