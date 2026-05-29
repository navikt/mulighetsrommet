package no.nav.tiltak.okonomi.api

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.oebs.OebsBestillingKvittering
import no.nav.tiltak.okonomi.oebs.OebsFakturaKvittering
import no.nav.tiltak.okonomi.plugins.AuthProvider
import no.nav.tiltak.okonomi.service.OkonomiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    val log: Logger = LoggerFactory.getLogger(javaClass)

    post<Kvittering.Bestilling> {
        val request = call.receive<String>()
        okonomiService.logKvittering(request)

        val kvitteringer = JsonIgnoreUnknownKeys.decodeFromString<List<OebsBestillingKvittering>>(request)

        kvitteringer.forEach { kvittering ->
            okonomiService.hentBestilling(kvittering.bestillingsNummer)?.let {
                okonomiService.mottaBestillingKvittering(it, kvittering)
            } ?: log.info("Fant ikke bestilling til kvittering med bestillingnummer ${kvittering.bestillingsNummer}")
        }

        call.respond(HttpStatusCode.OK)
    }

    post<Kvittering.Faktura> {
        val request = call.receive<String>()
        okonomiService.logKvittering(request)

        val kvitteringer = JsonIgnoreUnknownKeys.decodeFromString<List<OebsFakturaKvittering>>(request)

        kvitteringer.forEach { kvittering ->
            okonomiService.hentFaktura(kvittering.fakturaNummer)?.let {
                okonomiService.mottaFakturaKvittering(it, kvittering)
            } ?: log.info("Fant ikke faktura til kvittering med fakturanummer ${kvittering.fakturaNummer}")
        }

        call.respond(HttpStatusCode.OK)
    }
}
