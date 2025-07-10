package no.nav.mulighetsrommet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class DataDrivenTableDto(
    val columns: List<Column>,
    val rows: List<Map<String, JsonElement>>,
) {
    @Serializable
    data class Column(
        val key: String,
        val label: String,
        val sortable: Boolean,
        val align: Align,
        val format: Format?,
    ) {
        @Serializable
        enum class Align {
            @SerialName("left")
            LEFT,

            @SerialName("right")
            RIGHT,
        }

        @Serializable
        enum class Format {
            DATE,
            NOK,
        }
    }
}
