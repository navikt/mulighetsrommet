package no.nav.mulighetsrommet.api.pdfgen

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class VedtaksbrevContent(
    val deltaker: Deltaker,
    val saksnummer: Tiltaksnummer,
    @Serializable(with = LocalDateSerializer::class)
    val opprettetDato: LocalDate,
    val saksbehandler: String,
    val beslutter: String,
    val enhet: String,
    val vedtak: List<Vedtak>,
) {
    @Serializable
    data class Deltaker(
        val navn: String,
        val norskIdent: NorskIdent?,
    )

    @Serializable
    data class Vedtak(
        val utfall: String,
        val tilskuddType: String,
        val tilskuddBelop: Int,
        val valuta: String,
        val periode: Periode,
        val kommentar: String,
    ) {
        @Serializable
        data class Periode(
            @Serializable(with = LocalDateSerializer::class)
            val fradato: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tildato: LocalDate,
        )
    }
}
