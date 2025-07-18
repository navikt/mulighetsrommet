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
    data class Annullering(
        val payload: AnnullerBestilling,
    ) : OkonomiBestillingMelding()

    @Serializable
    @SerialName("FAKTURA")
    data class Faktura(
        val payload: OpprettFaktura,
    ) : OkonomiBestillingMelding()

    @Serializable
    @SerialName("GJOR_OPP_BESTILLING")
    data class GjorOppBestilling(
        val payload: no.nav.tiltak.okonomi.GjorOppBestilling,
    ) : OkonomiBestillingMelding()
}

@Serializable
data class OpprettBestilling(
    val bestillingsnummer: String,
    val tilskuddstype: Tilskuddstype,
    val tiltakskode: Tiltakskode,
    val arrangor: Organisasjonsnummer,
    val kostnadssted: NavEnhetNummer,
    val avtalenummer: String?,
    val belop: Int,
    val periode: Periode,
    val behandletAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
)

enum class Tilskuddstype {
    TILTAK_DRIFTSTILSKUDD,
    TILTAK_INVESTERINGER,
}

@Serializable
data class AnnullerBestilling(
    val bestillingsnummer: String,
    val behandletAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
)

@Serializable
data class GjorOppBestilling(
    val bestillingsnummer: String,
    val behandletAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
)

@Serializable
data class OpprettFaktura(
    val fakturanummer: String,
    val bestillingsnummer: String,
    val betalingsinformasjon: Betalingsinformasjon,
    val belop: Int,
    val periode: Periode,
    val behandletAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    @Serializable(with = LocalDateTimeSerializer::class)
    val besluttetTidspunkt: LocalDateTime,
    val gjorOppBestilling: Boolean,
    val beskrivelse: String?,
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

fun Agent.toOkonomiPart(): OkonomiPart = when (this) {
    is NavIdent -> OkonomiPart.NavAnsatt(this)
    is Tiltaksadministrasjon -> OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
    Arrangor, Arena -> throw IllegalStateException("ugyldig agent")
}
