package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class MsGraphUserDto(
    /**
     * Object ID i Azure AD
     */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
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

@Serializable
data class MsGraphGroup(
    /**
     * Object ID i Azure AD
     */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /**
     * Unikt navn, har typisk formatet 0000-GA-<min-ad-gruppe>
     */
    val displayName: String,
)
