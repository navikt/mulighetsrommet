package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideRequest
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.VeilederJoyrideRepository
import no.nav.mulighetsrommet.api.services.NavVeilederService
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.apache.commons.io.output.ByteArrayOutputStream
import org.koin.ktor.ext.inject
import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo

fun Route.pdfRoutes() {
    val veilederService: NavVeilederService by inject()
    val veilederJoyrideRepository: VeilederJoyrideRepository by inject()

    post("/pdf") {
        val dbo = call.receive<PdfKvittering>()
        val pdfBytes = generatePdfInMemory(dbo)

        call.response.headers.append("Content-Disposition", "attachment; filename=\"kvittering.pdf\"")
        call.respondBytes(pdfBytes, contentType = io.ktor.http.ContentType.Application.Pdf)
    }
}

@Serializable
data class Item(
    val title: String,
    val content: String
)

@Serializable
data class PdfKvittering (
    val content: List<Item>
)

fun generatePdfInMemory(options: PdfKvittering): ByteArray {
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
        contentP.spacingBefore = 5f   // Space before this paragraph
        contentP.spacingAfter = 20f

        document.add(
            contentP
        )
    }

    // Close the document
    document.close()

    // Return the PDF content as a byte array
    return byteArrayOutputStream.toByteArray()
}

