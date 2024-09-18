package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createHtmlFromTemplateData
import no.nav.pdfgen.core.pdf.createPDFA
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.util.*

fun Route.pdfRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(
        Environment(),
    )

    get("/pdf/{id}") {
        val id = call.parameters.getOrFail<UUID>("id")
        val html = createHtmlFromTemplateData("refusjon-kvittering", "refusjon").toString()
        val pdfBytes: ByteArray = createPDFA(html)

        call.response.headers.append(
            "Content-Disposition",
            "attachment; filename=\"kvittering.pdf\"",
        )
        call.respondBytes(pdfBytes, contentType = io.ktor.http.ContentType.Application.Pdf)
    }
}
