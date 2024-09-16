package no.nav.mulighetsrommet.api.routes.v1

import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Header
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import org.apache.commons.io.output.ByteArrayOutputStream
import java.util.*

/*
 * @todo: Hente refusjonskrav serverside
 * @todo: Finne ut av standard for PDF generering, utforske hva andre gjør
 * @todo: Finne ut av formatering, header / footer / logo
 * @todo: finne ut av hvordan å hente refusjonskrav fra DB
 */
fun Route.pdfRoutes() {
    get("/pdf/{id}") {
        val id = call.parameters.getOrFail<UUID>("id")

        // val dbo = call.receive<PdfKvittering>()
        val mockPdf = PdfKvittering(
            general = ItemGroup(
                title = null,
                items = listOf(
                    Item(title = "Tiltaksnavn", content = "AFT - Gruppe AFT Fredrikstad"),
                    Item(title = "Title", content = "Content"),
                ),
            ),
            tilsangsDetaljer = ItemGroup(
                title = null,
                items = listOf(
                    Item(title = "Tiltaksnavn", content = "AFT - Gruppe AFT Fredrikstad"),
                    Item(title = "Title", content = "Content"),
                ),
            ),
            refusjonsKrav = ItemGroup(
                title = null,
                items = listOf(
                    Item(title = "Tiltaksnavn", content = "AFT - Gruppe AFT Fredrikstad"),
                    Item(title = "Title", content = "Content"),
                ),
            ),
            betalingsInformasjon = ItemGroup(
                title = null,
                items = listOf(
                    Item(title = "Tiltaksnavn", content = "AFT - Gruppe AFT Fredrikstad"),
                    Item(title = "Title", content = "Content"),
                ),
            ),
        )

        val pdfBytes = generatePdfInMemory(mockPdf)

        call.response.headers.append("Content-Disposition", "attachment; filename=\"kvittering.pdf\"")
        call.respondBytes(pdfBytes, contentType = io.ktor.http.ContentType.Application.Pdf)
    }
}

@Serializable
private data class Item(
    val title: String,
    val content: String,
)

@Serializable
private data class ItemGroup(
    val title: String?,
    val items: List<Item>,
)

@Serializable
private data class PdfKvittering(
    val general: ItemGroup,
    val tilsangsDetaljer: ItemGroup,
    val refusjonsKrav: ItemGroup,
    val betalingsInformasjon: ItemGroup,
)

val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.BOLD)

private fun generateRow(item: Item): Pair<Paragraph, Paragraph> {
    val titleParagraph = Paragraph(item.title, boldFont)
    val contentParagraph = Paragraph(item.content)

    contentParagraph.firstLineIndent = 30f
    contentParagraph.spacingBefore = 5f
    contentParagraph.spacingAfter = 20f

    return Pair(titleParagraph, contentParagraph)
}

private fun generateGroup(
    document: Document,
    group: ItemGroup,
) {
    val groupTitle = Paragraph(group.title)

    document.add(groupTitle)

    group.items.forEach { item ->
        val paragraphs = generateRow(item)
        document.add(paragraphs.first)
        document.add(paragraphs.second)
    }
}

class HeaderEvent : PdfPageEventHelper() {
    override fun onEndPage(writer: PdfWriter, document: Document) {
        val cb: PdfContentByte = writer.directContent
        val font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f) // Define font

        // Create a paragraph for the header
        val header = Paragraph("Header Text", font)

        // Set the position for the header text (centered at the top)
        val x = (document.pageSize.width / 2) // Horizontal center
        val y = document.pageSize.height - document.topMargin() + 20 // Top of the page

        // Add header text
        cb.beginText()
        cb.setFontAndSize(font.baseFont, 12f)
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, header.content, x, y, 0f)
        cb.endText()
    }
}

private fun generatePdfInMemory(options: PdfKvittering): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val document = Document()

    val writer = PdfWriter.getInstance(document, byteArrayOutputStream)
    writer.pageEvent = HeaderEvent()

    document.open()

    val header = Header("NAV.no", "This is the haeder")

    document.add(header)

    generateGroup(document, options.general)
    generateGroup(document, options.tilsangsDetaljer)
    generateGroup(document, options.refusjonsKrav)
    generateGroup(document, options.betalingsInformasjon)

    document.close()

    return byteArrayOutputStream.toByteArray()
}
