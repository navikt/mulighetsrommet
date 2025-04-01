package no.nav.tiltak.okonomi.oebs

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.serializers.OebsLocalDateTimeSerializer
import no.nav.tiltak.okonomi.service.OkonomiService
import java.time.LocalDateTime

fun Route.oebsRoutes(
    okonomiService: OkonomiService,
) {
    route("/api/v1/kvittering") {
        post("opprett-bestilling") {
            val request = call.receive<String>()
            okonomiService.logKvittering(request)

            val kvittering = JsonIgnoreUnknownKeys.decodeFromString<OebsOpprettBestillingKvittering>(request)

            val bestilling = okonomiService.hentBestilling(kvittering.bestillingsNummer)
                ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke bestilling med bestillingsNummer: ${kvittering.bestillingsNummer}")

            okonomiService.mottaBestillingKvittering(bestilling, kvittering)

            call.respond(HttpStatusCode.OK)
        }

        post("annuller-bestilling") {
            val request = call.receive<String>()
            okonomiService.logKvittering(request)

            val kvittering = JsonIgnoreUnknownKeys.decodeFromString<OebsAnnullerBestillingKvittering>(request)

            val bestilling = okonomiService.hentBestilling(kvittering.bestillingsNummer)
                ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke bestilling med bestillingsNummer: ${kvittering.bestillingsNummer}")

            okonomiService.mottaAnnullerBestillingKvittering(bestilling, kvittering)

            call.respond(HttpStatusCode.OK)
        }

        post("faktura") {
            val request = call.receive<String>()
            okonomiService.logKvittering(request)

            val kvittering = JsonIgnoreUnknownKeys.decodeFromString<OebsFakturaKvittering>(request)

            val faktura = okonomiService.hentFaktura(kvittering.fakturaNummer)
                ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke faktura med fakturaNummer: ${kvittering.fakturaNummer}")

            okonomiService.mottaFakturaKvittering(faktura, kvittering)
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class OebsOpprettBestillingKvittering(
    val bestillingsNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val feilMelding: String? = null,
    val feilKode: String? = null,
) {
    fun isSuccess(): Boolean = feilKode == null && feilMelding == null
}

@Serializable
data class OebsAnnullerBestillingKvittering(
    val bestillingsNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val feilMelding: String? = null,
    val feilKode: String? = null,
) {
    fun isSuccess(): Boolean = feilKode == null && feilMelding == null
}

@Serializable
data class OebsFakturaKvittering(
    val fakturaNummer: String,
    val oebsStatus: String? = null,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val feilMelding: String? = null,
    val feilKode: String? = null,
) {
    fun isSuccess(): Boolean = feilKode == null && feilMelding == null
}
