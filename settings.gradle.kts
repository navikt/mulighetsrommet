rootProject.name = "mulighetsrommet"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "common:ktor",
    "common:database",
    "common:domain",
    "common:slack",
    "common:kafka",
    "mulighetsrommet-api",
    "mulighetsrommet-api-sanity-sync",
    "mulighetsrommet-arena-adapter",
)
