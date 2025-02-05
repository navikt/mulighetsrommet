package no.nav.mulighetsrommet.tiltak.okonomi

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.tiltak.okonomi.db.OkonomiDatabase
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsService

private const val API_BASE_PATH = "/api/v1/okonomi"

@Resource("$API_BASE_PATH/bestilling")
class Bestilling {

    @Resource("{id}")
    class Id(val parent: Bestilling = Bestilling(), val id: String) {

        @Resource("status")
        class Status(val parent: Id)
    }
}

@Resource("$API_BASE_PATH/faktura")
class Faktura {

    @Resource("{id}")
    class Id(val parent: Faktura = Faktura(), val id: String)
}

fun Application.okonomiRoutes(
    db: OkonomiDatabase,
    oebs: OebsService,
) = routing {
    install(Resources)

    authenticate {
        post<Bestilling> {
            val bestilling = call.receive<OpprettBestilling>()

            oebs.behandleBestilling(bestilling)
                .onLeft {
                    application.log.warn("Feil ved opprettelse av bestilling", it)
                    call.respond(HttpStatusCode.InternalServerError)
                }
                .onRight {
                    call.respond(HttpStatusCode.Created)
                }
        }

        post<Bestilling.Id.Status> { bestilling ->
            val body = call.receive<SetBestillingStatus>()

            when (body.status) {
                BestillingStatus.Type.ANNULLERT -> oebs.behandleAnnullering(bestilling.parent.id)
                    .onLeft {
                        application.log.warn("Feil ved annullering", it)
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                    .onRight {
                        call.respond(HttpStatusCode.OK)
                    }

                else -> call.respond(HttpStatusCode.NotImplemented)
            }
        }

        get<Bestilling.Id> { bestilling ->
            val status = db.session {
                queries.bestilling.getBestilling(bestilling.id)?.let {
                    BestillingStatus(
                        bestillingsnummer = it.bestillingsnummer,
                        status = when {
                            it.annullert -> BestillingStatus.Type.ANNULLERT
                            else -> BestillingStatus.Type.AKTIV
                        },
                    )
                }
            }

            if (status == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(status)
            }
        }

        post<Faktura> {
            val body = call.receive<OpprettFaktura>()

            oebs.behandleFaktura(body)
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
                queries.faktura.getFaktura(faktura.id)?.let {
                    FakturaStatus(
                        fakturanummer = it.fakturanummer,
                        status = FakturaStatus.Type.UTBETALT,
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
}

@Serializable
data class BestillingStatus(
    val bestillingsnummer: String,
    val status: Type,
) {

    enum class Type {
        AKTIV,
        ANNULLERT,
        OPPGJORT,
    }
}

@Serializable
data class SetBestillingStatus(
    val status: BestillingStatus.Type,
)

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: Type,
) {

    enum class Type {
        UTBETALT,
    }
}
