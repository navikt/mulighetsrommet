package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.Oppskrift
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject
import java.util.*

fun Route.veilederflateRoutes() {
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()
    val brukerService: BrukerService by inject()
    val navEnhetService: NavEnhetService by inject()

    route("/api/v1/internal") {
        get("/sanity/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

            call.respond(innsatsgrupper)
        }

        get("/sanity/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val tiltakstyper = veilederflateService.hentTiltakstyper()

            call.respond(tiltakstyper)
        }

        post("/sanity/tiltaksgjennomforinger") {
            val request = call.receive<GetRelevanteTiltaksgjennomforingerForBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val brukerdata = brukerService.hentBrukerdata(request.norskIdent, call.getAccessToken())

            val result = veilederflateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
                request,
                getRelevanteEnheterForBruker(brukerdata, navEnhetService),
            )

            call.respond(result)
        }

        post("/sanity/tiltaksgjennomforing") {
            val request = call.receive<GetTiltaksgjennomforingForBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val brukerdata = brukerService.hentBrukerdata(request.norskIdent, call.getAccessToken())

            val result = veilederflateService.hentTiltaksgjennomforingMedBrukerdata(
                request,
                getRelevanteEnheterForBruker(brukerdata, navEnhetService),
            )

            call.respond(result)
        }

        get("/oppskrifter/{tiltakstypeId}") {
            val tiltakstypeId = call.parameters.getOrFail("tiltakstypeId")
            val perspective = call.request.queryParameters["perspective"]?.let { SanityPerspective.valueOf(it) } ?: SanityPerspective.PUBLISHED
            val oppskrifter: List<Oppskrift> = veilederflateService.hentOppskrifterForTiltakstype(tiltakstypeId, perspective)
            call.respond(oppskrifter)
        }
    }
}

fun getRelevanteEnheterForBruker(brukerdata: BrukerService.Brukerdata, navEnhetService: NavEnhetService): List<String> {
    val geografiskEnhet = brukerdata.geografiskEnhet?.enhetsnummer?.let { navEnhetService.hentEnhet(it) }
    val oppfolgingsenhet =
        brukerdata.oppfolgingsenhet?.let { enhet -> enhet.enhetsnummer.let { navEnhetService.hentEnhet(it) } }

    val actualGeografiskEnhet = if (oppfolgingsenhet?.type == Norg2Type.LOKAL) {
        oppfolgingsenhet.enhetsnummer
    } else {
        geografiskEnhet?.enhetsnummer
    }
    val virtuellOppfolgingsenhet = if (oppfolgingsenhet != null && oppfolgingsenhet.type !in listOf(
            Norg2Type.FYLKE,
            Norg2Type.LOKAL,
        )
    ) {
        oppfolgingsenhet.enhetsnummer
    } else {
        null
    }
    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}

@Serializable
data class GetRelevanteTiltaksgjennomforingerForBrukerRequest(
    val norskIdent: String,
    val innsatsgruppe: String? = null,
    val tiltakstypeIds: List<String>? = null,
    val search: String? = null,
)

@Serializable
data class GetTiltaksgjennomforingForBrukerRequest(
    val norskIdent: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
