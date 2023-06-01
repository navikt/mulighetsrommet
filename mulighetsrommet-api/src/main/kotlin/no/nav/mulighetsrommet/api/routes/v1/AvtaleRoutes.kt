package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.services.AvtaleService
import no.nav.mulighetsrommet.api.utils.getAvtaleFilter
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.avtaleRoutes() {
    val avtaler: AvtaleService by inject()

    val logger = application.environment.log

    route("/api/v1/internal/avtaler") {
        get {
            val pagination = getPaginationParams()
            val filter = getAvtaleFilter()
            val result = avtaler.getAll(filter, pagination)

            call.respond(result)
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            avtaler.get(id)
                .onRight {
                    if (it == null) {
                        return@get call.respondText(
                            text = "Det finnes ikke noen avtale med id $id",
                            status = HttpStatusCode.NotFound,
                        )
                    }
                    return@get call.respond(it)
                }
                .onLeft {
                    log.error("$it")
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke hente avtale")
                }
        }

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")

            val nokkeltall = avtaler.getNokkeltallForAvtaleMedId(id)

            call.respond(nokkeltall)
        }

        put {
            val avtaleRequest = call.receive<AvtaleRequest>()

            avtaler.upsert(avtaleRequest)
                .map { call.respond(it) }
                .mapLeft {
                    logError(logger, it.error)
                    call.respond(HttpStatusCode.InternalServerError, "Kunne ikke opprette avtale")
                }
        }

        delete("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respond(avtaler.delete(id))
        }
    }
}

@Serializable
data class AvtaleRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val leverandorOrganisasjonsnummer: String,
    val leverandorUnderenheter: List<String> = emptyList(),
    val avtalenummer: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val navRegion: String,
    val antallPlasser: Int,
    val url: String,
    val ansvarlig: String,
    val avtaletype: Avtaletype,
    val prisOgBetalingsinformasjon: String? = null,
    val navEnheter: List<String> = emptyList(),
    val opphav: ArenaMigrering.Opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
) {
    fun toDbo(): AvtaleDbo {
        return AvtaleDbo(
            id = id ?: UUID.randomUUID(),
            navn = navn,
            avtalenummer = avtalenummer,
            tiltakstypeId = tiltakstypeId,
            leverandorOrganisasjonsnummer = leverandorOrganisasjonsnummer,
            leverandorUnderenheter = leverandorUnderenheter,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = null,
            navRegion = navRegion,
            avtaletype = avtaletype,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            antallPlasser = antallPlasser,
            url = url,
            ansvarlige = listOf(ansvarlig),
            prisbetingelser = prisOgBetalingsinformasjon,
            navEnheter = navEnheter,
            opphav = opphav,
        )
    }
}
