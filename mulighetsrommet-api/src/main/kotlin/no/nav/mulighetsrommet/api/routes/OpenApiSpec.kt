package no.nav.mulighetsrommet.api.routes

enum class OpenApiSpec(val routePathPrefix: Regex, val specName: String) {
    PUBLIC("/api/v\\d(?!/intern)".toRegex(), "public"),
    TILTAKSADMINISTRASJON("/api/tiltaksadministrasjon".toRegex(), "tiltaksadministrasjon"),
    VEILEDERFLATE("/api/veilederflate".toRegex(), "veilederflate"),
    ARRANGORFLATE("/api/arrangorflate".toRegex(), "arrangorflate"),
    ;

    companion object {
        fun match(url: String): OpenApiSpec? {
            return entries.find { spec -> spec.routePathPrefix.containsMatchIn(url) }
        }
    }
}
