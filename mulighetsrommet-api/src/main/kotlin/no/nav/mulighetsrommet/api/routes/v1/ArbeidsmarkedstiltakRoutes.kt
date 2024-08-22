package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.KontaktinfoVarsel
import no.nav.mulighetsrommet.api.domain.dto.Oppskrifter
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateKontaktinfo
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.services.CacheUsage
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.koin.ktor.ext.inject
import java.util.*

data class ArbeidsmarkedstiltakFilter(
    val enheter: NonEmptyList<String>,
    val innsatsgruppe: Innsatsgruppe,
    val tiltakstyper: List<String>?,
    val search: String?,
    val apentForInnsok: ApentForInnsok,
)

enum class ApentForInnsok {
    APENT,
    STENGT,
    APENT_ELLER_STENGT,
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getArbeidsmarkedstiltakFilter(): ArbeidsmarkedstiltakFilter {
    val queryParameters = call.request.queryParameters

    val enheter = queryParameters.getAll("enheter")
        ?.toNonEmptyListOrNull()
        ?: throw StatusException(HttpStatusCode.BadRequest, "NAV-enheter er påkrevd")
    val innsatsgruppe = queryParameters["innsatsgruppe"]
        ?.let { Innsatsgruppe.valueOf(it) }
        ?: throw StatusException(HttpStatusCode.BadRequest, "Innsatsgruppe er påkrevd")

    val apentForInnsok: ApentForInnsok by queryParameters

    return ArbeidsmarkedstiltakFilter(
        enheter = enheter,
        innsatsgruppe = innsatsgruppe,
        tiltakstyper = queryParameters.getAll("tiltakstyper"),
        search = queryParameters["search"],
        apentForInnsok = apentForInnsok,
    )
}

fun Route.veilederTiltakRoutes() {
    val avtaler: AvtaleService by inject()
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    get("/avtaler/{id}/behandle-personopplysninger") {
        val id = call.parameters.getOrFail<UUID>("id")
        val behandlingAvPersonopplysninger = avtaler.getBehandlingAvPersonopplysninger(id)
        call.respond(behandlingAvPersonopplysninger)
    }

    route("/veileder") {
        get("/innsatsgrupper") {
            val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

            call.respond(innsatsgrupper)
        }

        get("/tiltakstyper") {
            val tiltakstyper = veilederflateService.hentTiltakstyper()

            call.respond(tiltakstyper)
        }

        get("/tiltaksgjennomforinger") {
            poaoTilgangService.verifyAccessToModia(getNavAnsattAzureId())

            val filter = getArbeidsmarkedstiltakFilter()

            val result = veilederflateService.hentTiltaksgjennomforinger(
                enheter = filter.enheter,
                innsatsgruppe = filter.innsatsgruppe,
                tiltakstypeIds = filter.tiltakstyper,
                search = filter.search,
                apentForInnsok = filter.apentForInnsok,
                cacheUsage = CacheUsage.UseCache,
            )

            call.respond(result)
        }

        get("/tiltaksgjennomforinger/{id}") {
            poaoTilgangService.verifyAccessToModia(getNavAnsattAzureId())

            val id: UUID by call.parameters
            val enheter: List<String> by call.request.queryParameters

            val result = veilederflateService.hentTiltaksgjennomforing(
                id = id,
                enheter = enheter,
                sanityPerspective = SanityPerspective.PUBLISHED,
            )

            call.respond(result)
        }

        get("/oppskrifter/{tiltakstypeId}") {
            val tiltakstypeId: UUID by call.parameters
            val perspective = call.request.queryParameters["perspective"]
                ?.let {
                    when (it) {
                        "published" -> SanityPerspective.PUBLISHED
                        "raw" -> SanityPerspective.RAW
                        else -> SanityPerspective.PREVIEW_DRAFTS
                    }
                }
                ?: SanityPerspective.PUBLISHED

            val oppskrifter = veilederflateService.hentOppskrifterForTiltakstype(tiltakstypeId, perspective)

            call.respond(Oppskrifter(data = oppskrifter))
        }

        route("/nav") {
            fun utenKontaktInfo(gjennomforing: VeilederflateTiltaksgjennomforing): VeilederflateTiltaksgjennomforing {
                val arrangor = gjennomforing.arrangor?.copy(kontaktpersoner = emptyList())
                return gjennomforing.copy(
                    arrangor = arrangor,
                    kontaktinfo = VeilederflateKontaktinfo(
                        varsler = listOf(KontaktinfoVarsel.IKKE_TILGANG_TIL_KONTAKTINFO),
                        tiltaksansvarlige = emptyList(),
                    ),
                )
            }

            get("/tiltaksgjennomforinger") {
                val filter = getArbeidsmarkedstiltakFilter()

                val result = veilederflateService.hentTiltaksgjennomforinger(
                    enheter = filter.enheter,
                    innsatsgruppe = filter.innsatsgruppe,
                    tiltakstypeIds = filter.tiltakstyper,
                    search = filter.search,
                    apentForInnsok = filter.apentForInnsok,
                    cacheUsage = CacheUsage.UseCache,
                ).map { utenKontaktInfo(it) }

                call.respond(result)
            }

            get("/tiltaksgjennomforinger/{id}") {
                val id: UUID by call.parameters

                val result = veilederflateService.hentTiltaksgjennomforing(
                    id = id,
                    enheter = emptyList(),
                    sanityPerspective = SanityPerspective.PUBLISHED,
                ).let { utenKontaktInfo(it) }

                call.respond(result)
            }
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSADMINISTRASJON_GENERELL) {
            route("/preview") {
                get("/tiltaksgjennomforinger") {
                    val filter = getArbeidsmarkedstiltakFilter()

                    val result = veilederflateService.hentTiltaksgjennomforinger(
                        enheter = filter.enheter,
                        innsatsgruppe = filter.innsatsgruppe,
                        tiltakstypeIds = filter.tiltakstyper,
                        search = filter.search,
                        apentForInnsok = filter.apentForInnsok,
                        cacheUsage = CacheUsage.NoCache,
                    )

                    call.respond(result)
                }

                get("/tiltaksgjennomforinger/{id}") {
                    val id = call.parameters.getOrFail("id")
                        .let { UUID.fromString(it.replace("drafts.", "")) }
                    val enheter = call.request.queryParameters.getAll("enheter")
                        ?: emptyList()

                    val result = veilederflateService.hentTiltaksgjennomforing(
                        id = id,
                        enheter = enheter,
                        sanityPerspective = SanityPerspective.PREVIEW_DRAFTS,
                    )

                    call.respond(result)
                }
            }
        }
    }
}
