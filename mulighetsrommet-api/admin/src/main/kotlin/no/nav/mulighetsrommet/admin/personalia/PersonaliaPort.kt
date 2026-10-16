package no.nav.mulighetsrommet.admin.personalia

import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

/**
 * Personopplysninger og tilgangskontroll mot person. Implementeres i server (PDL og tilgangsmaskinen).
 */
interface PersonaliaPort {
    suspend fun getPersonalia(deltakerId: UUID, accessType: AccessType.OBO.AzureAd): Personalia

    suspend fun getPersonaliaAsSystem(deltakerId: UUID): Personalia

    suspend fun navAnsattTilgangTilPerson(norskIdent: NorskIdent, accessType: AccessType.OBO.AzureAd): AvvistGrunn?
}
