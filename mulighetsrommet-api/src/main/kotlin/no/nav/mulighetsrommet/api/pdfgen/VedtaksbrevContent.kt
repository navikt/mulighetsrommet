package no.nav.mulighetsrommet.api.pdfgen

import kotlinx.serialization.Serializable

@Serializable
data class VedtaksbrevContent(
    val deltaker: Deltaker,
    val saksnummer: String,
    val opprettetDato: String,
    val saksbehandler: String,
    val beslutter: String,
    val avsender: String,
    val vedtak: List<Vedtak>,
) {
    @Serializable
    data class Deltaker(
        val fornavn: String,
        val mellomnavn: String,
        val etternavn: String,
        val personident: String,
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
            val fradato: String,
            val tildato: String,
        )
    }
}
