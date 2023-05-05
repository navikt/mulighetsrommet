package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable

@Serializable
data class ODataListResponse<T>(
    val value: List<T>,
)
