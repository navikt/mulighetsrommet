rootProject.name = "mulighetsrommet"

include("common:ktor")
include("common:database")
include("common:domain")
include("common:slack")
include("common:kafka")

include("mulighetsrommet-api")
include("mulighetsrommet-api-sanity-sync")
include("mulighetsrommet-arena-adapter")
