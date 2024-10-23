package no.nav.mulighetsrommet.tokenprovider

sealed class AccessType {
    data class OBO(val token: String) : AccessType()
    data object M2M : AccessType()
    data class TOKENX(val token: String) : AccessType()
}
