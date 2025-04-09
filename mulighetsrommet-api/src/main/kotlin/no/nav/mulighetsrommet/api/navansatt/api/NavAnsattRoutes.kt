package no.nav.mulighetsrommet.api.navansatt.api

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()

    route("/ansatt") {
        get {
            val filter = getNavAnsattFilter()

            val ansatte = ansattService.getNavAnsatte(filter = filter).map {
                NavAnsattDto.fromNavAnsatt(it)
            }

            call.respond(ansatte)
        }

        get("/sok") {
            val q: String by call.request.queryParameters

            val ansatte = ansattService.getNavAnsattFromAzureSok(query = q).map {
                NavAnsattDto.fromAzureAdNavAnsatt(it)
            }

            call.respond(ansatte)
        }

        get("/me") {
            val azureId = getNavAnsattAzureId()
            val obo = AccessType.OBO(call.getAccessToken())

            val ansatt = ansattService.getOrSynchronizeNavAnsatt(azureId, obo).let {
                NavAnsattDto.fromNavAnsatt(it)
            }

            call.respond(ansatt)
        }
    }
}

fun RoutingContext.getNavAnsattFilter(): NavAnsattFilter {
    val azureIder = call.parameters.getAll("roller")
        ?.map { NavAnsattRolle.valueOf(it) }
        ?: emptyList()

    return NavAnsattFilter(roller = azureIder)
}

data class NavAnsattFilter(
    val roller: List<NavAnsattRolle> = emptyList(),
)
