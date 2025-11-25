package no.nav.mulighetsrommet.api.routes

import io.ktor.util.AttributeKey

private val OpenApiHashTiltaksadministrasjon: AttributeKey<String> = AttributeKey<String>("OpenApiHashTiltaksadministrasjon")
private val OpenApiHashPublic: AttributeKey<String> = AttributeKey<String>("OpenApiHashPublic")
private val OpenApiHashVeilederflate: AttributeKey<String> = AttributeKey<String>("OpenApiHashVeilederflate")
private val OpenApiHashArrangorflate: AttributeKey<String> = AttributeKey<String>("OpenApiHashArrangorflate")

enum class OpenApiSpec(
    val routePathPrefix: Regex,
    val specName: String,
    val hashAttributeKey: AttributeKey<String>,
) {
    PUBLIC(
        "/api/v\\d(?!/intern)".toRegex(),
        "public",
        OpenApiHashPublic,
    ),
    TILTAKSADMINISTRASJON(
        "/api/tiltaksadministrasjon".toRegex(),
        "tiltaksadministrasjon",
        OpenApiHashTiltaksadministrasjon,
    ),
    VEILEDERFLATE(
        "/api/veilederflate".toRegex(),
        "veilederflate",
        OpenApiHashVeilederflate,
    ),
    ARRANGORFLATE(
        "/api/arrangorflate".toRegex(),
        "arrangorflate",
        OpenApiHashArrangorflate,
    ),
    ;

    companion object {
        fun match(url: String): OpenApiSpec? {
            return entries.find { spec -> spec.routePathPrefix.containsMatchIn(url) }
        }
    }
}
