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
import no.nav.tiltak.okonomi.FakturaStatus
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.service.OkonomiService

@Resource("$API_BASE_PATH/faktura")
class Faktura {

    @Resource("{id}")
    class Id(val parent: Faktura = Faktura(), val id: String)
}

fun Routing.fakturaRoutes(
    db: OkonomiDatabase,
    okonomi: OkonomiService,
) = authenticate {
    post<Faktura> {
        val body = call.receive<OpprettFaktura>()

        okonomi.opprettFaktura(body)
            .onLeft {
                application.log.warn("Feil ved opprettelse av faktura", it)
                call.respond(HttpStatusCode.InternalServerError)
            }
            .onRight {
                call.respond(HttpStatusCode.Created)
            }
    }

    get<Faktura.Id> { faktura ->
        val status = db.session {
            queries.faktura.getByFakturanummer(faktura.id)?.let {
                FakturaStatus(
                    fakturanummer = it.fakturanummer,
                    status = FakturaStatusType.SENDT,
                    fakturaStatusSistOppdatert = it.fakturaStatusSistOppdatert,
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
