package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class Details(
    val entries: List<DetailEntry>,
)

@Serializable
data class DetailEntry(
    val key: String,
    val value: String,
    val format: DetailFormat? = null,
) {
    companion object {
        fun nok(key: String, value: Int): DetailEntry {
            return DetailEntry(key, value.toString(), format = DetailFormat.NOK)
        }

        fun number(key: String, value: Number): DetailEntry {
            return DetailEntry(key, value.toString())
        }

        fun periode(key: String, value: Periode): DetailEntry {
            val start = value.start.formaterDatoTilEuropeiskDatoformat()
            val slutt = value.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()
            return DetailEntry(key, "$start - $slutt")
        }
    }
}

enum class DetailFormat {
    NOK,
}
