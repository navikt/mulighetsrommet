package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
internal data class MsGraphUserDto(
    /**
     * Object ID i Azure AD
     */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /**
     * Fornavn
     */
    val givenName: String?,
    /**
     * Etternavn
     */
    val surname: String?,
    /**
     * NAVident
     *
     * Vi antar at denne er definert for alle ansatte,
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos NAV.
     */
    val onPremisesSamAccountName: String?,
    /**
     * E-postadresse
     */
    val mail: String?,
    /**
     * NAV Enhetskode
     *
     * Vi antar at denne er definert for alle ansatte,
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos NAV.
     */
    val streetAddress: String?,
    /**
     * NAV Enhetsnavn
     *
     * Vi antar at denne er definert for alle ansatte,
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos NAV.
     */
    val city: String?,
    /**
     * Mobilnummer
     */
    val mobilePhone: String? = null,
)

@Serializable
internal data class MsGraphGroup(
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
