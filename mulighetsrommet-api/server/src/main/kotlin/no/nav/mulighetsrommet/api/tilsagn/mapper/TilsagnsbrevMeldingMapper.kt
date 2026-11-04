package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.admin.arrangor.TilsagnsbrevArrangorMelding
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.util.UUID

data class TilsagnsbrevMeldingSnapshot(
    val tilsagnId: UUID,
    val tiltakstypeNavn: String,
    val bestillingsnummer: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val arrangorNavn: String,
    val lopenummer: Tiltaksnummer
)

object TilsagnsbrevMeldingMapper {
    fun tilArrangorMelding(
        tilsagn: TilsagnsbrevMeldingSnapshot,
        pdf: ByteArray,
    ): TilsagnsbrevArrangorMelding = TilsagnsbrevArrangorMelding(
        mottakerOrganisasjonsnummer = tilsagn.arrangorOrganisasjonsnummer,
        sendersReferanse = tilsagn.bestillingsnummer,
        innhold = TilsagnsbrevArrangorMelding.Innhold(
            tittel = "Bekreftelse på bestilling ${tilsagn.tiltakstypeNavn} ${tilsagn.lopenummer.value}",
            brevtekst = "Se vedlagt bekreftelsesbrev for ${tilsagn.tiltakstypeNavn}.",
        ),
        varsel = TilsagnsbrevArrangorMelding.Varsel(
            emne = "Bekreftelse på bestilling for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig i Altinn",
            tekst = """

                Bekreftelse på bestilling for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig. Logg inn i Altinn for å se innholdet.

                Vennlig hilsen Nav
            """.trimIndent(),
        ),
        vedlegg = TilsagnsbrevArrangorMelding.Vedlegg(
            navn = "Bekreftelsesbrev-${tilsagn.tiltakstypeNavn}.pdf",
            innhold = pdf,
        ),
        idempotentKey = tilsagn.tilsagnId,
    )
}
