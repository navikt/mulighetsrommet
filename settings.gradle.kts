rootProject.name = "mulighetsrommet"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "common:database",
    "common:domain",
    "common:kafka",
    "common:ktor",
    "common:ktor-clients",
    "common:slack",
    "mulighetsrommet-api",
    "mulighetsrommet-api-sanity-sync",
    "mulighetsrommet-arena-adapter",
)
