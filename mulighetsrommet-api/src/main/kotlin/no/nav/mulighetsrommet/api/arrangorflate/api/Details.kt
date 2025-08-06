package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class Details(
    val entries: List<DetailsEntry>,
)

@Serializable
data class DetailsEntry(
    val key: String,
    val value: String,
    val format: DetailsFormat? = null,
) {
    companion object {
        fun nok(key: String, value: Int): DetailsEntry {
            return DetailsEntry(key, value.toString(), format = DetailsFormat.NOK)
        }

        fun number(key: String, value: Number): DetailsEntry {
            return DetailsEntry(key, value.toString())
        }

        fun periode(key: String, value: Periode): DetailsEntry {
            val start = value.start.formaterDatoTilEuropeiskDatoformat()
            val slutt = value.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
            return DetailsEntry(key, "$start - $slutt")
        }
    }
}

enum class DetailsFormat {
    NOK,
}
