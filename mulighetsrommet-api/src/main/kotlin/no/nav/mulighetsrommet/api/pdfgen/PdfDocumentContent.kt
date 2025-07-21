package no.nav.mulighetsrommet.api.pdfgen

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
) {
    @Serializable
    data class Header(
        val text: String,
        val level: Int,
    )

    @Serializable
    data class Block(
        val description: String? = null,
        val values: List<String> = listOf(),
        val entries: List<Entry> = listOf(),
        val table: Table? = null,
    )

    @Serializable
    data class Entry(
        val label: String,
        val value: String?,
        val format: Format? = null,
    )

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

    enum class Format {
        NOK,
        DATE,
        PERCENT,
        STATUS_SUCCESS,
    }
}
