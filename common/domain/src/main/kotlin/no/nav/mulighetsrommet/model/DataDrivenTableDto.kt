package no.nav.mulighetsrommet.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.DataElement.Text.Format
import java.time.LocalDate

@Serializable
class DataDrivenTableDto(
    val columns: List<Column>,
    val rows: List<Map<String, DataElement?>>,
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

            @SerialName("right")
            RIGHT,
        }
    }
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
)

enum class LabeledDataElementType {
    INLINE,
    MULTILINE,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DataElement {

    @Serializable
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
    data class Status(
        val value: String,
        val variant: Variant,
        val description: String? = null,
    ) : DataElement() {
        enum class Variant {
            @SerialName("neutral")
            NEUTRAL,

            @SerialName("alt")
            ALT,

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

    @Serializable
    data class Link(
        val text: String,
        val href: String,
    ) : DataElement()

    @Serializable
    data class MultiLinkModal(
        val buttonText: String,
        val modalContent: ModalContent
    ) : DataElement() {
        @Serializable
        data class ModalContent(
            val header: String,
            val description: String,
            val links: List<Link>,
        )
    }

    @Serializable
    data class Periode(
        val start: String,
        val slutt: String,
    ) : DataElement()

    @Serializable
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

        fun nok(value: Number) = Text(value.toString(), Format.NOK)

        fun date(value: LocalDate?) = Text(value?.toString(), Format.DATE)

        fun number(value: Number) = Text(value.toString(), Format.NUMBER)

        fun periode(periode: no.nav.mulighetsrommet.model.Periode) = Periode(
            start = periode.start,
            slutt = periode.getLastInclusiveDate(),
        )
    }
}
