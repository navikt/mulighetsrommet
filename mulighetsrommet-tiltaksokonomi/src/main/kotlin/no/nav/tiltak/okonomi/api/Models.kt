package no.nav.tiltak.okonomi.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.tiltak.okonomi.db.BestillingStatusType
import no.nav.tiltak.okonomi.db.FakturaStatusType
import java.time.LocalDateTime

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

@Serializable
data class BestillingStatus(
    val bestillingsnummer: String,
    val status: BestillingStatusType,
)

@Serializable
data class SetBestillingStatus(
    val status: BestillingStatusType,
)

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: FakturaStatusType,
)
