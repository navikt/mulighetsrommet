package no.nav.mulighetsrommet.api.veilederflate.routes

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.swagger.v3.oas.models.media.Schema
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavAnsattEntraObjectId
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.veilederflate.models.Oppskrifter
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateInnsatsgruppe
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltak
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.services.VeilederflateService
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

internal data class ArbeidsmarkedstiltakFilter(
    val enheter: NonEmptyList<NavEnhetNummer>,
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
        ?.map { NavEnhetNummer(it) }
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

    get("/innsatsgrupper", {
        tags = setOf("VeilederTiltak")
        operationId = "getInnsatsgrupper"
        response {
            code(HttpStatusCode.OK) {
                description = "Hent innsatsgrupper"
                body<List<VeilederflateInnsatsgruppe>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

        call.respond(innsatsgrupper)
    }

    get("/tiltakstyper", {
        tags = setOf("VeilederTiltak")
        operationId = "getVeilederflateTiltakstyper"
        response {
            code(HttpStatusCode.OK) {
                description = "Hent tiltakstyper"
                body<List<VeilederflateTiltakstype>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val tiltakstyper = veilederflateService.hentTiltakstyper()

        call.respond(tiltakstyper)
    }

    get("/gjennomforinger", {
        tags = setOf("VeilederTiltak")
        operationId = "getAllVeilederTiltak"
        request {
            queryParameter<Innsatsgruppe>("innsatsgruppe")
            queryParameter<List<String>>("enheter") {
                explode = true
            }
            queryParameter<String>("search")
            queryParameter<ApentForPamelding>("apentForPamelding")
            queryParameter<List<String>>("tiltakstyper") {
                explode = true
            }
            queryParameter<Boolean>("erSykmeldtMedArbeidsgiver")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle tiltak som matcher filteret"
                body<List<VeilederflateTiltak>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        poaoTilgangService.verifyAccessToModia(getNavAnsattEntraObjectId())

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

    get("/gjennomforinger/{id}", {
        tags = setOf("VeilederTiltak")
        operationId = "getVeilederTiltak"
        request {
            // TODO: fant ikke noen god måte å spesifere uuid, men det er kanskje bare like greit å dokumentere det en string
            pathParameter(
                "id",
                Schema<Any>().also {
                    it.types = setOf("string")
                    it.format = "uuid"
                },
            ) {
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Tiltak for gitt id"
                body<VeilederflateTiltak>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        poaoTilgangService.verifyAccessToModia(getNavAnsattEntraObjectId())

        val id: UUID by call.parameters

        val result = veilederflateService.hentTiltaksgjennomforing(
            id = id,
            sanityPerspective = SanityPerspective.PUBLISHED,
        )

        call.respond(result)
    }

    get("/oppskrifter/{tiltakstypeId}", {
        tags = setOf("Oppskrifter")
        operationId = "getOppskrifter"
        request {
            pathParameter<String>("tiltakstypeId") {
                required = true
            }
            queryParameter<SanityPerspective>("perspective") {
                description = "Hvilket Sanity-perspective du vil bruke"
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste med oppskrifter tilknyttet tiltakstypen"
                body<Oppskrifter>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val tiltakstypeId: UUID by call.parameters
        val perspective = call.request.queryParameters["perspective"]
            ?.let { SanityPerspective.valueOf(it) }
            ?: SanityPerspective.PUBLISHED

        val oppskrifter = veilederflateService.hentOppskrifter(tiltakstypeId, perspective)

        call.respond(Oppskrifter(data = oppskrifter))
    }

    route("/nav") {
        get("/gjennomforinger", {
            tags = setOf("VeilederTiltak")
            operationId = "getAllNavTiltak"
            request {
                queryParameter<Innsatsgruppe>("innsatsgruppe")
                queryParameter<List<String>>("enheter") {
                    explode = true
                }
                queryParameter<String>("search")
                queryParameter<ApentForPamelding>("apentForPamelding")
                queryParameter<List<String>>("tiltakstyper") {
                    explode = true
                }
                queryParameter<Boolean>("erSykmeldtMedArbeidsgiver")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle tiltak som matcher filteret"
                    body<List<VeilederflateTiltak>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
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

        get("/gjennomforinger/{id}", {
            tags = setOf("VeilederTiltak")
            operationId = "getNavTiltak"
            request {
                pathParameter<String>("id") {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltak for gitt id"
                    body<VeilederflateTiltak>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters

            val result = veilederflateService.hentTiltaksgjennomforing(
                id = id,
                sanityPerspective = SanityPerspective.PUBLISHED,
            )

            call.respond(result)
        }
    }

    authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
        route("/preview") {
            get("/gjennomforinger", {
                tags = setOf("VeilederTiltak")
                operationId = "getAllPreviewTiltak"
                request {
                    queryParameter<Innsatsgruppe>("innsatsgruppe")
                    queryParameter<List<String>>("enheter") {
                        explode = true
                    }
                    queryParameter<String>("search")
                    queryParameter<ApentForPamelding>("apentForPamelding")
                    queryParameter<List<String>>("tiltakstyper") {
                        explode = true
                    }
                    queryParameter<Boolean>("erSykmeldtMedArbeidsgiver")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Alle tiltak som matcher filteret"
                        body<List<VeilederflateTiltak>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
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

            get("/gjennomforinger/{id}", {
                tags = setOf("VeilederTiltak")
                operationId = "getPreviewTiltak"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tiltak for gitt id"
                        body<VeilederflateTiltak>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
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
