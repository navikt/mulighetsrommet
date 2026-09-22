package no.nav.mulighetsrommet.serialization.json

import kotlinx.serialization.json.Json

val JsonIgnoreUnknownKeys = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
