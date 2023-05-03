rootProject.name = "mulighetsrommet"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "common:database",
    "common:domain",
    "common:kafka",
    "common:ktor",
    "common:ktor-clients",
    "common:metrics",
    "common:slack",
    "mulighetsrommet-api",
    "mulighetsrommet-arena-adapter",
)
