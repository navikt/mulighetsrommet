package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.admin.arrangor.TilsagnsbrevArrangorMelding
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

data class TilsagnsbrevMeldingSnapshot(
    val tilsagnId: UUID,
    val tiltakstypeNavn: String,
    val bestillingsnummer: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val arrangorNavn: String,
)

object TilsagnsbrevMeldingMapper {
    fun tilArrangorMelding(
        tilsagn: TilsagnsbrevMeldingSnapshot,
        pdf: ByteArray,
    ): TilsagnsbrevArrangorMelding = TilsagnsbrevArrangorMelding(
        mottakerOrganisasjonsnummer = tilsagn.arrangorOrganisasjonsnummer,
        sendersReferanse = tilsagn.bestillingsnummer,
        innhold = TilsagnsbrevArrangorMelding.Innhold(
            tittel = "Tilsagnsbrev ${tilsagn.tiltakstypeNavn} ${tilsagn.bestillingsnummer}",
            brevtekst = "Se vedlagt tilsagnsbrev for ${tilsagn.tiltakstypeNavn}.",
        ),
        varsel = TilsagnsbrevArrangorMelding.Varsel(
            emne = "Tilsagnsbrev for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig i Altinn",
            tekst = """
                Tilsagnsbrev for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig. Logg inn i Altinn for å se innholdet.

                Vennlig hilsen Nav
            """.trimIndent(),
        ),
        vedlegg = TilsagnsbrevArrangorMelding.Vedlegg(
            navn = "Tilsagnsbrev-${tilsagn.tiltakstypeNavn}.pdf",
            innhold = pdf,
        ),
        idempotentKey = tilsagn.tilsagnId,
    )
}
