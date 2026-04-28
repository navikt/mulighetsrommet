package no.nav.mulighetsrommet.tokenprovider

sealed class AccessType {
    sealed class OBO : AccessType() {
        abstract val token: String

        data class TokenX(override val token: String) : OBO()
        data class AzureAd(override val token: String) : OBO()
    }
    data object M2M : AccessType()
}

fun AccessType.requireAzureAd(): AccessType.OBO.AzureAd {
    require(this is AccessType.OBO.AzureAd) {
        "azureAd access type required"
    }
    return this
}

fun AccessType.requireTokenX(): AccessType.OBO.TokenX {
    require(this is AccessType.OBO.TokenX) {
        "TokenX access type required"
    }
    return this
}
