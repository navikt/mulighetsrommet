package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.Regards
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import java.time.LocalDate

object TilskuddVedtakToPdfDocumentContentMapper {

    fun toVedtakPdfContent(
        tilskuddBehandling: TilskuddBehandlingDto,
        personalia: Personalia,
        gjennomforing: GjennomforingEnkeltplass,
    ): PdfDocumentContent = (
        PdfDocumentContent.create(
            title = "Vedtak om opplæringstilskudd",
            subject = "Vedtak om opplæringstilskudd for ${personalia.navn()}",
            description = "Vedtak om opplæringstilskudd for ${personalia.navn()}",
            author = "Tiltaksadministrasjon", // Burde være saksbehandler eller beslutter her kanskje?
        ) {
            section("") {
                descriptionList {
                    text("Navn", personalia.navn())
                    text("Fødselsnummer", personalia.norskIdent())
                    text("Dato", LocalDate.now().toString())
                }
            }

            mainSection("Vedtak for tilskudd til opplæring") {
                section("") {
                    descriptionList {
                        text("Tiltak", gjennomforing.navn)
                        text("Løpenummer", gjennomforing.lopenummer)
                        text("Arrangør", gjennomforing.arrangor.navn)
                    }
                }
                tilskuddBehandling.tilskudd.forEach {
                    section("${it.vedtakResultat.type.name} - ${it.tilskuddOpplaeringType.name}")
                    descriptionList {
                        money("Tilskuddsbeløp", it.soknadBelop)
                    }
                }
            }
            regards(
                Regards(
                    "Med vennlig hilsen",
                    "Saksbehandler",
                    listOf("Beslutter"),
                ),
            )
        }
        )
}
