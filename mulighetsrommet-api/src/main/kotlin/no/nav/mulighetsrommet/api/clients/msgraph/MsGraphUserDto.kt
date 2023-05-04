package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable

@Serializable
data class MsGraphUserDto(
    /**
     * Fornavn
     */
    val givenName: String,
    /**
     * Etternavn
     */
    val surname: String,
    /**
     * NAVident
     */
    val onPremisesSamAccountName: String,
    /**
     * E-postadresse
     */
    val mail: String,
    /**
     * NAV Enhetskode
     */
    val streetAddress: String,
    /**
     * NAV Enhetsnavn
     */
    val city: String,
)
