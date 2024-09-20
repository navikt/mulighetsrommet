rootProject.name = "mulighetsrommet"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "common:database",
    "common:domain",
    "common:kafka",
    "common:ktor",
    "common:ktor-clients",
    "common:metrics",
    "common:nais",
    "common:slack",
    "common:tasks",
    "common:token-provider",
    "mulighetsrommet-api",
    "mulighetsrommet-arena-adapter",
    "mulighetsrommet-tiltakshistorikk",
)
