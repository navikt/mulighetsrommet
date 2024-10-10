package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.plugins.getPid
import no.nav.mulighetsrommet.api.services.ArrangorRolleService
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createHtmlFromTemplateData
import no.nav.pdfgen.core.pdf.createPDFA
import org.koin.ktor.ext.inject
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.time.LocalDate
import java.util.*

fun Route.refusjonRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(
        Environment(),
    )
    val service: RefusjonService by inject()
    val arrangorRolleService: ArrangorRolleService by inject()

    route("/api/v1/intern/refusjon") {
        get("/krav") {
            val roller = arrangorRolleService.getRoller(getPid())
            if (roller.isEmpty()) {
                return@get call.respond(HttpStatusCode.Unauthorized)
            }

            call.respond(service.getByArrangorIds(roller.map { it.arrangorId }))
        }

        get("/kvittering/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val html = createHtmlFromTemplateData("refusjon-kvittering", "refusjon").toString()
            val pdfBytes: ByteArray = createPDFA(html)

            call.response.headers.append(
                "Content-Disposition",
                "attachment; filename=\"kvittering.pdf\"",
            )
            call.respondBytes(pdfBytes, contentType = io.ktor.http.ContentType.Application.Pdf)
        }

        get("/krav/{id}") {
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
data class RefusjonskravDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksgjennomforing: Gjennomforing,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val beregning: Prismodell.RefusjonskravBeregning,
    val arrangor: Arrangor,
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
}
