package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.commons.io.output.ByteArrayOutputStream
import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import kotlinx.serialization.Serializable

fun Route.pdfRoutes() {
    post("/pdf") {
        val dbo = call.receive<PdfKvittering>()
        val pdfBytes = generatePdfInMemory(dbo)

        call.response.headers.append("Content-Disposition", "attachment; filename=\"kvittering.pdf\"")
        call.respondBytes(pdfBytes, contentType = io.ktor.http.ContentType.Application.Pdf)
    }
}

@Serializable
private data class Item(
    val title: String,
    val content: String
)

@Serializable
private data class PdfKvittering (
    val content: List<Item>
)

private fun generatePdfInMemory(options: PdfKvittering): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val document = Document()
    PdfWriter.getInstance(document, byteArrayOutputStream)

    document.open()

    for (item in options.content) {
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.BOLD)
        val titleP = Paragraph(item.title, boldFont)
        document.add(titleP)

        val contentP = Paragraph(item.content)

        contentP.firstLineIndent = 30f
        contentP.spacingBefore = 5f
        contentP.spacingAfter = 20f

        document.add(
            contentP
        )
    }

    document.close()

    return byteArrayOutputStream.toByteArray()
}

