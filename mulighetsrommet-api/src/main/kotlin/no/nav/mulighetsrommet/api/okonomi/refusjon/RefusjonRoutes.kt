package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregning
import no.nav.mulighetsrommet.api.plugins.getPid
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createHtmlFromTemplateData
import no.nav.pdfgen.core.pdf.createPDFA
import org.koin.ktor.ext.inject
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.util.*

fun Route.refusjonRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(
        Environment(),
    )
    val service: RefusjonService by inject()

    route("/api/v1/intern/refusjon") {
        post("/krav") {
            val request = call.receive<GetRefusjonskravRequest>()
            val norskIdent = getPid()

            call.respond(service.getByOrgnr(request.orgnr))
        }

        get("/kvittering/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val html = createHtmlFromTemplateData("refusjon-kvittering", "refusjon").toString()
            val pdfBytes: ByteArray = createPDFA(html)

            call.response.headers.append(
                "Content-Disposition",
                "attachment; filename=\"kvittering.pdf\"",
            )
            call.respondBytes(pdfBytes, contentType = ContentType.Application.Pdf)
        }
        get("/krav/{id}") {
            // val orgnr = Organisasjonsnummer(call.parameters.getOrFail<String>("orgnr"))
            val id = call.parameters.getOrFail<UUID>("id")
            val krav = service.getById(id)

            if (krav != null) {
                call.respond(krav)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

@Serializable
data class GetRefusjonskravRequest(
    val orgnr: List<Organisasjonsnummer>,
)

@Serializable
data class RefusjonskravDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksgjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val beregning: RefusjonKravBeregning,
    val tiltakstype: Tiltakstype,
) {
    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: String,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class Tiltakstype(
        val navn: String,
    )
}
