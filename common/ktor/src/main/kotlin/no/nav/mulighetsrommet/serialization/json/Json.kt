package no.nav.mulighetsrommet.serialization.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

val JsonIgnoreUnknownKeys = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@OptIn(ExperimentalSerializationApi::class)
val JsonRelaxExplicitNulls = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
