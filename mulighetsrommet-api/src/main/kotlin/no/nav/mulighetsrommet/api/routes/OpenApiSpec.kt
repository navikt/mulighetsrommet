package no.nav.mulighetsrommet.api.routes

enum class OpenApiSpec(
    val routePathPrefix: Regex,
    val specName: String,
    val version: Int,
) {
    PUBLIC("/api/v\\d(?!/intern)".toRegex(), "public", 1),
    TILTAKSADMINISTRASJON("/api/tiltaksadministrasjon".toRegex(), "tiltaksadministrasjon", 1),
    VEILEDERFLATE("/api/veilederflate".toRegex(), "veilederflate", 1),
    ARRANGORFLATE("/api/arrangorflate".toRegex(), "arrangorflate", 1),
    ;

    companion object {
        fun match(url: String): OpenApiSpec? {
            return entries.find { spec -> spec.routePathPrefix.containsMatchIn(url) }
        }

        fun fromSpecName(name: String) = when (name) {
            TILTAKSADMINISTRASJON.specName -> TILTAKSADMINISTRASJON
            PUBLIC.specName -> PUBLIC
            VEILEDERFLATE.specName -> VEILEDERFLATE
            ARRANGORFLATE.specName -> ARRANGORFLATE
            else -> throw IllegalArgumentException("Ukjent spec")
        }
    }
}
