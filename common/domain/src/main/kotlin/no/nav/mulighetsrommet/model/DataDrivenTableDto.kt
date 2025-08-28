package no.nav.mulighetsrommet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DataDrivenTableDto(
    val columns: List<Column>,
    val rows: List<Map<String, DataElement?>>,
) {
    @Serializable
    data class Column(
        val key: String,
        val label: String,
        val sortable: Boolean,
        val align: Align,
    ) {
        @Serializable
        enum class Align {
            @SerialName("left")
            LEFT,

            @SerialName("right")
            RIGHT,
        }
    }
}

@Serializable
sealed class DataElement {

    @Serializable
    @SerialName("text")
    data class Text(
        val value: String,
        val format: Format? = null,
    ) : DataElement() {
        enum class Format {
            DATE,
            NOK,
            NUMBER,
        }
    }

    @Serializable
    @SerialName("status")
    data class Status(
        val value: String,
        val variant: Variant,
    ) : DataElement() {
        enum class Variant {
            NEUTRAL,
            SUCCESS,
            WARNING,
            ERROR,
        }
    }

    @Serializable
    @SerialName("link")
    data class Link(
        val text: String,
        val href: String,
    ) : DataElement()

    @Serializable
    @SerialName("periode")
    data class Periode(
        val start: String,
        val slutt: String,
    ) : DataElement()
}
