package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable

@Serializable
data class EstimertVentetid(
    val verdi: Int,
    val enhet: Enhet,
) {
    enum class Enhet {
        UKE,
        MANED,
    }

    fun formatToString(): String {
        val pluralizedEnhet = when (enhet) {
            Enhet.UKE -> pluralize(verdi, "uke", "uker")
            Enhet.MANED -> pluralize(verdi, "måned", "måneder")
        }
        return "$verdi $pluralizedEnhet"
    }
}

private fun pluralize(count: Int, singular: String, plural: String): String {
    return if (count == 1) singular else plural
}
