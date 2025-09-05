package no.nav.mulighetsrommet.utdanning

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramMedUtdanninger
import org.koin.ktor.ext.inject
import java.util.*

fun Route.utdanningRoutes() {
    val db: ApiDatabase by inject()

    route("utdanninger") {
        get({
            description = "Hent alle utdanningsprogrammer"
            tags = setOf("Utdanning")
            operationId = "getUtdanningsprogrammer"
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste over utdanningsprogrammer"
                    body<List<UtdanningsprogramMedUtdanningerDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utdanninger = db
                .session { queries.utdanning.getUtdanningsprogrammer() }
                .map { toDto(it) }

            call.respond(utdanninger)
        }
    }
}

private fun toDto(programMedUtdanninger: UtdanningsprogramMedUtdanninger): UtdanningsprogramMedUtdanningerDto {
    val (utdanningsprogram, utdanninger) = programMedUtdanninger
    return UtdanningsprogramMedUtdanningerDto(
        UtdanningsprogramMedUtdanningerDto.Utdanningsprogram(utdanningsprogram.id, utdanningsprogram.navn),
        utdanninger.map { utdanning -> UtdanningsprogramMedUtdanningerDto.Utdanning(utdanning.id, utdanning.navn) },
    )
}

@Serializable
data class UtdanningsprogramMedUtdanningerDto(
    val utdanningsprogram: Utdanningsprogram,
    val utdanninger: List<Utdanning>,
) {
    @Serializable
    data class Utdanningsprogram(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )

    @Serializable
    data class Utdanning(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
    )
}
