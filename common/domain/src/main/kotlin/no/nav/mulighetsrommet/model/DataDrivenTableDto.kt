package no.nav.mulighetsrommet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DataElement.Text.Format

@Serializable
class DataDrivenTableDto(
    val columns: List<Column>,
    val rows: List<Map<String, DataElement?>>,
) {
    @Serializable
    data class Column(
        val key: String,
        val label: String,
        val sortable: Boolean = true,
        val align: Align = Align.LEFT,
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
            @SerialName("date")
            DATE,

            @SerialName("nok")
            NOK,

            @SerialName("number")
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
            @SerialName("neutral")
            NEUTRAL,

            @SerialName("success")
            SUCCESS,

            @SerialName("warning")
            WARNING,

            @SerialName("error")
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

    companion object {
        fun text(value: Any) = Text(value.toString())

        fun nok(value: Number) = Text(value.toString(), Format.NOK)

        fun number(value: Number) = Text(value.toString(), Format.NUMBER)

        fun periode(periode: no.nav.mulighetsrommet.model.Periode) = Periode(
            start = periode.start,
            slutt = periode.getLastInclusiveDate(),
        )
    }
}
