package no.nav.mulighetsrommet.api.veilederflate.routes

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.cms.CacheUsage
import no.nav.mulighetsrommet.api.veilederflate.models.Oppskrifter
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.koin.ktor.ext.inject
import java.util.*

internal data class ArbeidsmarkedstiltakFilter(
    val enheter: NonEmptyList<String>,
    val innsatsgruppe: Innsatsgruppe,
    val tiltakstyper: List<String>?,
    val search: String?,
    val apentForPamelding: ApentForPamelding,
    val erSykmeldtMedArbeidsgiver: Boolean,
)

enum class ApentForPamelding {
    APENT,
    STENGT,
    APENT_ELLER_STENGT,
}

internal fun RoutingContext.getArbeidsmarkedstiltakFilter(): ArbeidsmarkedstiltakFilter {
    val queryParameters = call.request.queryParameters

    val enheter = queryParameters.getAll("enheter")
        ?.toNonEmptyListOrNull()
        ?: throw StatusException(HttpStatusCode.BadRequest, "Nav-enheter er påkrevd")
    val innsatsgruppe = queryParameters["innsatsgruppe"]
        ?.let { Innsatsgruppe.valueOf(it) }
        ?: throw StatusException(HttpStatusCode.BadRequest, "Innsatsgruppe er påkrevd")
    val erSykmeldtMedArbeidsgiver = queryParameters["erSykmeldtMedArbeidsgiver"]
        ?.toBoolean()
        ?: false

    val apentForPamelding: ApentForPamelding by queryParameters

    return ArbeidsmarkedstiltakFilter(
        enheter = enheter,
        innsatsgruppe = innsatsgruppe,
        tiltakstyper = queryParameters.getAll("tiltakstyper"),
        search = queryParameters["search"],
        apentForPamelding = apentForPamelding,
        erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver,
    )
}

fun Route.arbeidsmarkedstiltakRoutes() {
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/veileder") {
        get("/innsatsgrupper") {
            val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

            call.respond(innsatsgrupper)
        }

        get("/tiltakstyper") {
            val tiltakstyper = veilederflateService.hentTiltakstyper()

            call.respond(tiltakstyper)
        }

        get("/gjennomforinger") {
            poaoTilgangService.verifyAccessToModia(getNavAnsattAzureId())

            val filter = getArbeidsmarkedstiltakFilter()

            val result = veilederflateService.hentTiltaksgjennomforinger(
                enheter = filter.enheter,
                innsatsgruppe = filter.innsatsgruppe,
                tiltakstypeIds = filter.tiltakstyper,
                search = filter.search,
                apentForPamelding = filter.apentForPamelding,
                erSykmeldtMedArbeidsgiver = filter.erSykmeldtMedArbeidsgiver,
                cacheUsage = CacheUsage.UseCache,
            )

            call.respond(result)
        }

        get("/gjennomforinger/{id}") {
            poaoTilgangService.verifyAccessToModia(getNavAnsattAzureId())

            val id: UUID by call.parameters

            val result = veilederflateService.hentTiltaksgjennomforing(
                id = id,
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

            val oppskrifter = veilederflateService.hentOppskrifter(tiltakstypeId, perspective)

            call.respond(Oppskrifter(data = oppskrifter))
        }

        route("/nav") {
            get("/gjennomforinger") {
                val filter = getArbeidsmarkedstiltakFilter()

                val result = veilederflateService.hentTiltaksgjennomforinger(
                    enheter = filter.enheter,
                    innsatsgruppe = filter.innsatsgruppe,
                    tiltakstypeIds = filter.tiltakstyper,
                    search = filter.search,
                    apentForPamelding = filter.apentForPamelding,
                    erSykmeldtMedArbeidsgiver = filter.erSykmeldtMedArbeidsgiver,
                    cacheUsage = CacheUsage.UseCache,
                )

                call.respond(result)
            }

            get("/gjennomforinger/{id}") {
                val id: UUID by call.parameters

                val result = veilederflateService.hentTiltaksgjennomforing(
                    id = id,
                    sanityPerspective = SanityPerspective.PUBLISHED,
                )

                call.respond(result)
            }
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSADMINISTRASJON_GENERELL) {
            route("/preview") {
                get("/gjennomforinger") {
                    val filter = getArbeidsmarkedstiltakFilter()

                    val result = veilederflateService.hentTiltaksgjennomforinger(
                        enheter = filter.enheter,
                        innsatsgruppe = filter.innsatsgruppe,
                        tiltakstypeIds = filter.tiltakstyper,
                        search = filter.search,
                        apentForPamelding = filter.apentForPamelding,
                        erSykmeldtMedArbeidsgiver = filter.erSykmeldtMedArbeidsgiver,
                        cacheUsage = CacheUsage.NoCache,
                    )

                    call.respond(result)
                }

                get("/gjennomforinger/{id}") {
                    val id = call.parameters.getOrFail("id")
                        .let { UUID.fromString(it.replace("drafts.", "")) }

                    val result = veilederflateService.hentTiltaksgjennomforing(
                        id = id,
                        sanityPerspective = SanityPerspective.PREVIEW_DRAFTS,
                    )

                    call.respond(result)
                }
            }
        }
    }
}
