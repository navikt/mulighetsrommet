package no.nav.mulighetsrommet.api.refusjon

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravKompakt
import org.koin.ktor.ext.inject
import java.util.*

fun Route.refusjonRoutes() {
    val refusjonskravRepository: RefusjonskravRepository by inject()

    route("/tiltaksgjennomforinger/{id}/refusjonskrav") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskravRepository.getByGjennomforing(id, statuser = emptyList())
                    .map { RefusjonKravKompakt.fromRefusjonskravDto(it) }

                call.respond(krav)
            }
        }
    }
}
