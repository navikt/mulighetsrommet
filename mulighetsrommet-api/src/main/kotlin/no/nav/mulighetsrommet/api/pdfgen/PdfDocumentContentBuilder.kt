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

    fun section(title: String, level: Int = 4, init: SectionBuilder.() -> Unit = {}) {
        val builder = SectionBuilder(Section.Header(title, level))
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

class SectionBuilder(private val header: Section.Header) {
    private val blocks = mutableListOf<Section.Block>()

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

    fun itemList(init: ValueListBlockBuilder.() -> Unit) {
        val builder = ValueListBlockBuilder()
        builder.init()
        blocks.add(builder.build())
    }

    fun build() = Section(header, blocks)
}

class DescriptionListBlockBuilder {
    private val entries = mutableListOf<Section.Entry>()
    var description: String? = null

    fun entry(label: String, value: Any?, format: Section.Format? = null) {
        entries.add(Section.Entry(label, value?.toString(), format))
    }

    fun build(): Section.Block = Section.Block(
        description = description,
        entries = entries,
    )
}

class ValueListBlockBuilder {
    var description: String? = null
    private val values = mutableListOf<String>()

    fun item(value: String) {
        values.add(value)
    }

    fun build(): Section.Block = Section.Block(
        description = description,
        values = values,
    )
}

class TableBlockBuilder {
    private val columns = mutableListOf<Section.Table.Column>()
    private val rows = mutableListOf<Section.Table.Row>()

    fun column(name: String, align: Section.Table.Column.Align = Section.Table.Column.Align.LEFT) {
        columns.add(Section.Table.Column(name, align))
    }

    fun row(vararg cells: Section.Table.Cell) {
        rows.add(Section.Table.Row(cells.toList()))
    }

    fun build(): Section.Block = Section.Block(
        table = Section.Table(columns, rows),
    )
}
