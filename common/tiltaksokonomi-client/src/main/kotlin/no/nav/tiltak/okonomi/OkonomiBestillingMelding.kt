package no.nav.tiltak.okonomi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
sealed class OkonomiBestillingMelding {

    @Serializable
    @SerialName("BESTILLING")
    data class Bestilling(
        val payload: OpprettBestilling,
    ) : OkonomiBestillingMelding()

    @Serializable
    @SerialName("ANNULLERING")
    data object Annullering : OkonomiBestillingMelding()

    @Serializable
    @SerialName("FAKTURA")
    data class Faktura(
        val payload: OpprettFaktura,
    ) : OkonomiBestillingMelding()
}

@Serializable
data class OpprettBestilling(
    val tiltakskode: Tiltakskode,
    val arrangor: Arrangor,
    val kostnadssted: NavEnhetNummer,
    val bestillingsnummer: String,
    val avtalenummer: String?,
    val belop: Int,
    val periode: Periode,
    val opprettetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
) {
    @Serializable
    data class Arrangor(
        val hovedenhet: Organisasjonsnummer,
        val underenhet: Organisasjonsnummer,
    )
}

@Serializable
data class OpprettFaktura(
    val fakturanummer: String,
    val bestillingsnummer: String,
    val betalingsinformasjon: Betalingsinformasjon,
    val belop: Int,
    val periode: Periode,
    val opprettetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
) {
    @Serializable
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer,
        val kid: Kid?,
    )
}

@Serializable
sealed class OkonomiPart(val part: String) {

    @Serializable
    data class NavAnsatt(val navIdent: NavIdent) : OkonomiPart(navIdent.value)

    @Serializable
    data class System(val kilde: OkonomiSystem) : OkonomiPart(kilde.name)

    companion object {
        fun fromString(value: String): OkonomiPart {
            return try {
                System(OkonomiSystem.valueOf(value))
            } catch (e: IllegalArgumentException) {
                NavAnsatt(NavIdent(value))
            }
        }
    }
}

enum class OkonomiSystem {
    TILTAKSADMINISTRASJON,
}
