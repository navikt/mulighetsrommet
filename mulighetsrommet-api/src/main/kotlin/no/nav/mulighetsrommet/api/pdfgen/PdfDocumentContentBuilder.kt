package no.nav.mulighetsrommet.api.pdfgen

class PdfDocumentContentBuilder(
    private val title: String,
    private val subject: String,
    private val description: String,
    private val author: String,
) {
    private val sections = mutableListOf<Section>()

    fun mainSection(title: String, init: SectionBuilder.() -> Unit = {}) {
        section(title, level = 1, init)
    }

    fun section(title: String, level: Int = 2, init: SectionBuilder.() -> Unit = {}) {
        val builder = SectionBuilder(Header(title, level))
        builder.init()
        sections.add(builder.build())
    }

    fun build() = PdfDocumentContent(
        title = title,
        subject = subject,
        description = description,
        author = author,
        sections = sections,
    )
}

class SectionBuilder(private val header: Header) {
    private val blocks = mutableListOf<Block>()

    fun descriptionList(init: DescriptionListBlockBuilder.() -> Unit) {
        val builder = DescriptionListBlockBuilder()
        builder.init()
        blocks.add(builder.build())
    }

    fun table(init: TableBlockBuilder.() -> Unit) {
        val builder = TableBlockBuilder()
        builder.init()
        blocks.add(builder.build())
    }

    fun itemList(init: ItemListBlockBuilder.() -> Unit) {
        val builder = ItemListBlockBuilder()
        builder.init()
        blocks.add(builder.build())
    }

    fun build() = Section(header, blocks)
}

class DescriptionListBlockBuilder {
    var description: String? = null
    private val entries = mutableListOf<DescriptionListBlock.Entry>()

    fun entry(label: String, value: Any?, format: Format? = null) {
        entries.add(DescriptionListBlock.Entry(label, value?.toString(), format))
    }

    fun build(): DescriptionListBlock = DescriptionListBlock(
        description = description,
        entries = entries,
    )
}

class ItemListBlockBuilder {
    var description: String? = null
    private val items = mutableListOf<String>()

    fun item(value: String) {
        items.add(value)
    }

    fun build(): ItemListBlock = ItemListBlock(
        description = description,
        items = items,
    )
}

class TableBlockBuilder {
    var description: String? = null
    private val columns = mutableListOf<TableBlock.Table.Column>()
    private val rows = mutableListOf<TableBlock.Table.Row>()

    fun column(name: String, align: TableBlock.Table.Column.Align = TableBlock.Table.Column.Align.LEFT) {
        columns.add(TableBlock.Table.Column(name, align))
    }

    fun row(vararg cells: TableBlock.Table.Cell) {
        rows.add(TableBlock.Table.Row(cells.toList()))
    }

    fun build(): TableBlock = TableBlock(
        description = description,
        table = TableBlock.Table(columns, rows),
    )
}
