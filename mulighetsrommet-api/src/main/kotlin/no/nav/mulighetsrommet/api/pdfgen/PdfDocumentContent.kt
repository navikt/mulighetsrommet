package no.nav.mulighetsrommet.api.pdfgen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PdfDocumentContent(
    val title: String,
    val subject: String,
    val description: String,
    val author: String,
    val sections: List<Section>,
) {
    companion object {
        fun create(
            title: String,
            subject: String,
            description: String,
            author: String,
            init: PdfDocumentContentBuilder.() -> Unit,
        ): PdfDocumentContent {
            val builder = PdfDocumentContentBuilder(title, subject, description, author)
            builder.init()
            return builder.build()
        }
    }
}

@Serializable
data class Section(
    val title: Header,
    val blocks: List<Block> = listOf(),
)

@Serializable
data class Header(
    val text: String,
    val level: Int,
)

@Serializable
sealed class Block {
    abstract val description: String?
}

@Serializable
@SerialName("description-list")
data class DescriptionListBlock(
    override val description: String? = null,
    val entries: List<Entry> = listOf(),
) : Block() {
    @Serializable
    data class Entry(
        val label: String,
        val value: String?,
        val format: Format? = null,
    )
}

@Serializable
@SerialName("item-list")
data class ItemListBlock(
    override val description: String? = null,
    val items: List<String> = listOf(),
) : Block()

@Serializable
@SerialName("table")
data class TableBlock(
    override val description: String? = null,
    val table: Table? = null,
) : Block() {
    @Serializable
    data class Table(
        val columns: List<Column>,
        val rows: List<Row>,
    ) {
        @Serializable
        data class Column(
            val title: String,
            val align: Align = Align.LEFT,
        ) {
            enum class Align {
                LEFT,
                RIGHT,
            }
        }

        @Serializable
        data class Row(
            val cells: List<Cell>,
        )

        @Serializable
        data class Cell(
            val value: String?,
            val format: Format? = null,
        )
    }
}

enum class Format {
    NOK,
    DATE,
    PERCENT,
    STATUS_SUCCESS,
}
