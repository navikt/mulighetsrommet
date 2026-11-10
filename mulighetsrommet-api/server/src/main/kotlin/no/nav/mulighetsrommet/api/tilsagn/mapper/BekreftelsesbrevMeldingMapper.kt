package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.admin.arrangor.BekreftelsesbrevArrangorMelding
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.util.UUID

data class BekreftelsesbrevMeldingSnapshot(
    val tilsagnId: UUID,
    val tiltakstypeNavn: String,
    val bestillingsnummer: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val arrangorNavn: String,
    val lopenummer: Tiltaksnummer,
)

object BekreftelsesbrevMeldingMapper {
    fun tilArrangorMelding(
        tilsagn: BekreftelsesbrevMeldingSnapshot,
        pdf: ByteArray,
    ): BekreftelsesbrevArrangorMelding = BekreftelsesbrevArrangorMelding(
        mottakerOrganisasjonsnummer = tilsagn.arrangorOrganisasjonsnummer,
        sendersReferanse = tilsagn.bestillingsnummer,
        innhold = BekreftelsesbrevArrangorMelding.Innhold(
            tittel = "Bekreftelse på bestilling ${tilsagn.tiltakstypeNavn} ${tilsagn.lopenummer.value}",
            brevtekst = "Se vedlagt bekreftelsesbrev for ${tilsagn.tiltakstypeNavn}.",
        ),
        varsel = BekreftelsesbrevArrangorMelding.Varsel(
            emne = "Bekreftelse på bestilling for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig i Altinn",
            tekst = """

                Bekreftelse på bestilling for ${tilsagn.arrangorOrganisasjonsnummer} ${tilsagn.arrangorNavn} er tilgjengelig. Logg inn i Altinn for å se innholdet.

                Vennlig hilsen Nav
            """.trimIndent(),
        ),
        vedlegg = BekreftelsesbrevArrangorMelding.Vedlegg(
            navn = "Bekreftelsesbrev-${tilsagn.tiltakstypeNavn}.pdf",
            innhold = pdf,
        ),
        idempotentKey = tilsagn.tilsagnId,
    )
}
