package no.nav.mulighetsrommet.api.routes

enum class OpenApiSpec(val routePathPrefix: String, val specName: String) {
    VEILEDERFLATE("/api/veilederflate", "veilederflate"),
    ;

    companion object {
        fun match(url: String): OpenApiSpec? {
            return entries.find { spec -> url.startsWith(spec.routePathPrefix) }
        }
    }
}
