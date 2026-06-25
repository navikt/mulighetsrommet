package no.nav.mulighetsrommet.api.plugins

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import no.nav.mulighetsrommet.env.NaisEnv

fun Application.configureHTTP() {
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                else -> null
            }
        }
    }
    install(ConditionalHeaders)
    install(CORS) {
        when (NaisEnv.current()) {
            NaisEnv.Local -> anyHost()

            NaisEnv.DevGCP -> {
                allowHost("*.dev.nav.no", schemes = listOf("https"))
            }

            NaisEnv.ProdGCP -> {
                allowHost("*.nav.no", schemes = listOf("https"))
                allowHost("nav.no", schemes = listOf("https"))
            }
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        allowHeader(HttpHeaders.XRequestId)
        allowHeadersPrefixed("nav-")
        exposeHeader("X-OpenAPI-Version")
    }
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff") // hindrer MIME-type sniffing
        header("X-Frame-Options", "DENY") // hindrer clickjacking via iframe
        header("Referrer-Policy", "strict-origin-when-cross-origin") // begrenser referrer-lekkasje
    }
}
