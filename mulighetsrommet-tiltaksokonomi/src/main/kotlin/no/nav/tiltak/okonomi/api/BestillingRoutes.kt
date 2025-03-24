package no.nav.tiltak.okonomi.api

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tiltak.okonomi.BestillingStatus
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.service.OkonomiService

@Resource("$API_BASE_PATH/bestilling")
class Bestilling {

    @Resource("{id}")
    class Id(val parent: Bestilling = Bestilling(), val id: String)
}

fun Routing.bestillingRoutes(
    db: OkonomiDatabase,
    okonomi: OkonomiService,
) = authenticate {
    post<Bestilling> {
        val bestilling = call.receive<OpprettBestilling>()

        okonomi.opprettBestilling(bestilling)
            .onLeft {
                application.log.warn("Feil ved opprettelse av bestilling", it)
                call.respond(HttpStatusCode.InternalServerError)
            }
            .onRight {
                call.respond(HttpStatusCode.Created)
            }
    }

    get<Bestilling.Id> { bestilling ->
        val status = db.session {
            queries.bestilling.getByBestillingsnummer(bestilling.id)?.let {
                BestillingStatus(
                    bestillingsnummer = it.bestillingsnummer,
                    status = it.status,
                )
            }
        }

        if (status == null) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(status)
        }
    }
}
