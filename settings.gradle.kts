rootProject.name = "mulighetsrommet"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "common:brreg",
    "common:database",
    "common:database-helpers",
    "common:domain",
    "common:kafka",
    "common:ktor",
    "common:ktor-clients",
    "common:metrics",
    "common:nais",
    "common:slack",
    "common:tasks",
    "common:token-provider",
    "common:tiltaksokonomi-client",
    "mulighetsrommet-api",
    "mulighetsrommet-arena-adapter",
    "mulighetsrommet-tiltakshistorikk",
    "mulighetsrommet-tiltaksokonomi",
)
