package no.nav.mulighetsrommet.api.okonomi.refusjon

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createHtmlFromTemplateData
import no.nav.pdfgen.core.pdf.createPDFA
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.util.*

fun Route.refusjonRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(
        Environment(),
    )

    route("/api/v1/intern/refusjon") {
        get("/{orgnr}/krav") {
            val orgnr = call.parameters.getOrFail<String>("orgnr")

            call.respond(
                listOf(
                    RefusjonskravDto(
                        id = UUID.randomUUID(),
                        belop = "308 530",
                        fristForGodkjenning = "31.08.2024",
                        kravnr = "6",
                        periode = "01.06.2024 - 30.06.2024",
                        status = RefusjonskravStatus.KLAR_FOR_INNSENDING,
                        tiltaksnr = "2024/123456",
                    ),
                    RefusjonskravDto(
                        id = UUID.randomUUID(),
                        belop = "123 000",
                        fristForGodkjenning = "31.07.2024",
                        kravnr = "5",
                        periode = "01.05.2024 - 31.05.2024",
                        status = RefusjonskravStatus.NARMER_SEG_FRIST,
                        tiltaksnr = "2024/123456",
                    ),
                    RefusjonskravDto(
                        id = UUID.randomUUID(),
                        belop = "85 000",
                        fristForGodkjenning = "30.06.2024",
                        kravnr = "4",
                        periode = "01.01.2024 - 31.01.2024",
                        status = RefusjonskravStatus.ATTESTERT,
                        tiltaksnr = "2024/123456",
                    ),
                ),
            )
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
    }
}

enum class RefusjonskravStatus {
    ATTESTERT,
    KLAR_FOR_INNSENDING,
    NARMER_SEG_FRIST,
}

@Serializable
data class RefusjonskravDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksnr: String,
    val kravnr: String,
    val periode: String,
    val belop: String,
    val fristForGodkjenning: String,
    val status: RefusjonskravStatus,
)
