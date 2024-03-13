package no.nav.mulighetsrommet.api.clients

sealed class AccessType {
    data class OBO(val token: String) : AccessType()
    object M2M : AccessType()
}
