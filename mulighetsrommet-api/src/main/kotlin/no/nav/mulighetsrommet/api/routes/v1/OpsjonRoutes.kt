package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.OpsjonLoggService
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.opsjonRoutes() {
    val opsjonLoggService: OpsjonLoggService by inject()

    route("api/v1/intern/opsjoner") {
        authenticate(AuthProvider.AZURE_AD_AVTALER_SKRIV.name, strategy = AuthenticationStrategy.Required) {
            post {
                val request = call.receive<OpsjonLoggRequest>()
                val userId = getNavIdent()
                val opsjonLoggEntry = OpsjonLoggEntry(
                    avtaleId = request.avtaleId,
                    sluttdato = request.nySluttdato,
                    forrigeSluttdato = request.forrigeSluttdato,
                    status = request.status,
                    registrertAv = userId,
                )

                opsjonLoggService.lagreOpsjonLoggEntry(opsjonLoggEntry)
                call.respond(HttpStatusCode.OK)
            }

            delete {
                val request = call.receive<SlettOpsjonLoggRequest>()
                val userId = getNavIdent()
                opsjonLoggService.delete(
                    opsjonLoggEntryId = request.id,
                    avtaleId = request.avtaleId,
                    slettesAv = userId,
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

@Serializable
data class OpsjonLoggRequest(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val nySluttdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonsLoggStatus,
) {
    enum class OpsjonsLoggStatus {
        OPSJON_UTLØST,
        SKAL_IKKE_UTLØSE_OPSJON,
        PÅGÅENDE_OPSJONSPROSESS,
    }
}

@Serializable
data class SlettOpsjonLoggRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
)
