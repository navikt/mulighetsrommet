rootProject.name = "mulighetsrommet"

include("common:ktor")
include("common:database")
include("common:domain")

include("mulighetsrommet-api")
include("mulighetsrommet-api-sanity-sync")
include("mulighetsrommet-arena-adapter")
include("mulighetsrommet-arena-ords-proxy")
