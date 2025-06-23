package no.nav.tiltak.okonomi.api

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.api.serializers.OebsLocalDateTimeSerializer
import no.nav.tiltak.okonomi.plugins.AuthProvider
import no.nav.tiltak.okonomi.service.OkonomiService
import java.time.LocalDateTime

@Resource("$API_BASE_PATH/kvittering")
class Kvittering {
    @Resource("bestilling")
    class Bestilling(val parent: Kvittering = Kvittering())

    @Resource("faktura")
    class Faktura(val parent: Kvittering = Kvittering())
}

fun Routing.oebsRoutes(
    okonomiService: OkonomiService,
) = authenticate(AuthProvider.AZURE_AD_OEBS_API.name) {
    post<Kvittering.Bestilling> {
        val request = call.receive<String>()
        okonomiService.logKvittering(request)

        val kvitteringer = JsonIgnoreUnknownKeys.decodeFromString<List<OebsBestillingKvittering>>(request)

        var error: ProblemDetail? = null

        kvitteringer.forEach { kvittering ->
            val bestilling = okonomiService.hentBestilling(kvittering.bestillingsNummer)
            if (bestilling == null) {
                error = NotFound("Fant ikke bestilling med bestillingsNummer: ${kvittering.bestillingsNummer}")
            } else {
                okonomiService.mottaBestillingKvittering(bestilling, kvittering)
            }
        }

        error?.let {
            call.respondWithProblemDetail(it)
        } ?: call.respond(HttpStatusCode.OK)
    }

    post<Kvittering.Faktura> {
        val request = call.receive<String>()
        okonomiService.logKvittering(request)

        val kvitteringer = JsonIgnoreUnknownKeys.decodeFromString<List<OebsFakturaKvittering>>(request)
        kvitteringer.forEach { kvittering ->
            val faktura = okonomiService.hentFaktura(kvittering.fakturaNummer)
                ?: throw StatusException(
                    HttpStatusCode.NotFound,
                    "Fant ikke faktura med fakturaNummer: ${kvittering.fakturaNummer}",
                )

            okonomiService.mottaFakturaKvittering(faktura, kvittering)
        }
        call.respond(HttpStatusCode.OK)
    }
}

@Serializable
data class OebsBestillingKvittering(
    val bestillingsNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val statusOebs: String? = null,
    val feilMelding: String? = null,
    val feilKode: String? = null,
    val annullert: String? = null,
) {
    fun isSuccess(): Boolean = statusOebs != "Avvist" && feilKode == null && feilMelding == null
    fun isAnnulleringKvittering(): Boolean = annullert != null
}

@Serializable
data class OebsFakturaKvittering(
    val fakturaNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val statusOebs: String? = null,
    val feilMelding: String? = null,
    val feilKode: String? = null,
) {
    fun isSuccess(): Boolean = statusOebs != "Avvist" && feilKode == null && feilMelding == null
}
