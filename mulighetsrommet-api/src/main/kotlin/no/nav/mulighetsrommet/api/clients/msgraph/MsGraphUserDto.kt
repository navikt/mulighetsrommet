package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos Nav.
     */
    val onPremisesSamAccountName: String?,
    /**
     * E-postadresse
     */
    val mail: String?,
    /**
     * Nav Enhetskode
     *
     * Vi antar at denne er definert for alle ansatte,
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos Nav.
     */
    val streetAddress: String?,
    /**
     * Nav Enhetsnavn
     *
     * Vi antar at denne er definert for alle ansatte,
     * om feltet er null så antar vi at brukeren ikke er en ansatt hos Nav.
     */
    val city: String?,
    /**
     * Mobilnummer
     */
    val mobilePhone: String? = null,
)
