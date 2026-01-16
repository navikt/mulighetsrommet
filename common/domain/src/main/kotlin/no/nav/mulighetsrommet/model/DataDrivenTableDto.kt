package no.nav.mulighetsrommet.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.DataElement.Text.Format
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
class DataDrivenTableDto(
    val columns: List<Column>,
    val rows: List<Row>,
) {
    @Serializable
    data class Column(
        val key: String,
        val label: String?,
        val sortable: Boolean = true,
        val align: Align = Align.LEFT,
    ) {
        @Serializable
        enum class Align {
            @SerialName("left")
            LEFT,

            @SerialName("center")
            CENTER,

            @SerialName("right")
            RIGHT,
        }
    }

    @Serializable
    data class Row(
        val cells: Map<String, DataElement?>,
        // Kan utvides til å kunne være flere forskjellige ting? F. eks DataDetails
        val content: TimelineDto? = null,
    )
}

@Serializable
data class DataDetails(
    val header: String? = null,
    val entries: List<LabeledDataElement>,
)

@Serializable
data class LabeledDataElement(
    val type: LabeledDataElementType,
    val label: String,
    val value: DataElement?,
) {
    companion object {
        fun text(label: String, value: String?, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.text(value),
        )

        fun nok(label: String, value: Number, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.nok(value),
        )

        fun currency(
            label: String,
            value: Number?,
            valuta: Valuta,
            type: LabeledDataElementType = LabeledDataElementType.INLINE,
        ) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.currency(value, valuta),
        )

        fun date(label: String, value: LocalDate?, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.date(value),
        )

        fun number(label: String, value: Number, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.number(value),
        )

        fun periode(label: String, periode: Periode, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(
            type = type,
            label = label,
            value = DataElement.periode(periode),
        )
    }
}

enum class LabeledDataElementType {
    INLINE,
    MULTILINE,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DataElement {
    @Serializable
    @SerialName("DATA_ELEMENT_TEXT")
    data class Text(
        val value: String?,
        val format: Format?,
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
    @SerialName("DATA_ELEMENT_CURRENCY")
    data class CurrencyValue(
        val value: String?,
        val currency: String,
    ) : DataElement()

    @SerialName("DATA_ELEMENT_STATUS")
    @Serializable
    data class Status(
        val value: String,
        val variant: Variant,
        val description: String? = null,
    ) : DataElement() {
        enum class Variant {
            @SerialName("blank")
            BLANK,

            @SerialName("neutral")
            NEUTRAL,

            @SerialName("alt")
            ALT,

            @SerialName("alt-1")
            ALT_1,

            @SerialName("alt-2")
            ALT_2,

            @SerialName("alt-3")
            ALT_3,

            @SerialName("info")
            INFO,

            @SerialName("success")
            SUCCESS,

            @SerialName("warning")
            WARNING,

            @SerialName("error")
            ERROR,

            @SerialName("error-border")
            ERROR_BORDER,

            @SerialName("error-border-strikethrough")
            ERROR_BORDER_STRIKETHROUGH,
        }
    }

    @SerialName("DATA_ELEMENT_LINK")
    @Serializable
    data class Link(
        val text: String,
        val href: String,
    ) : DataElement() {
        val digest = this.hashCode().toHexString()
    }

    @SerialName("DATA_ELEMENT_MULTI_LINK_MODAL")
    @Serializable
    data class MultiLinkModal(
        val buttonText: String,
        val modalContent: ModalContent,
    ) : DataElement() {
        @Serializable
        data class ModalContent(
            val header: String,
            val description: String,
            val links: List<Link>,
        )
    }

    @SerialName("DATA_ELEMENT_PERIODE")
    @Serializable
    data class Periode(
        val start: String,
        val slutt: String,
    ) : DataElement()

    @Serializable
    @SerialName("DATA_ELEMENT_MATH_OPERATOR")
    data class MathOperator(
        val operator: Type,
    ) : DataElement() {
        enum class Type {
            @SerialName("plus")
            PLUS,

            @SerialName("minus")
            MINUS,

            @SerialName("multiply")
            MULTIPLY,

            @SerialName("divide")
            DIVIDE,

            @SerialName("equals")
            EQUALS,
        }
    }

    fun label(label: String, type: LabeledDataElementType = LabeledDataElementType.INLINE) = LabeledDataElement(type, label, this)

    companion object {
        fun text(value: String?) = Text(value, null)

        fun nok(value: Number?) = Text(value?.toString(), Format.NOK)

        fun currency(value: Number?, valuta: Valuta) = CurrencyValue(value?.toString(), valuta.name)

        fun date(value: LocalDate?) = Text(value?.toString(), Format.DATE)

        fun number(value: Number) = Text(value.toString(), Format.NUMBER)

        fun periode(periode: no.nav.mulighetsrommet.model.Periode) = DataElement.Periode(
            start = periode.start.formaterDatoTilEuropeiskDatoformat(),
            slutt = periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
        )
    }
}

const val EUROPEAN_DATE_PATTERN = "dd.MM.yyyy"

fun LocalDate.formaterDatoTilEuropeiskDatoformat(): String {
    return format(DateTimeFormatter.ofPattern(EUROPEAN_DATE_PATTERN))
}
