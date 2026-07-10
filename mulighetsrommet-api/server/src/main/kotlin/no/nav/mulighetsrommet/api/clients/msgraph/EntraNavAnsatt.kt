package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class EntraNavAnsatt(
    @Serializable(with = UUIDSerializer::class)
    val entraObjectId: UUID,
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhetKode: NavEnhetNummer,
    val hovedenhetNavn: String,
    val mobilnummer: String?,
    val epost: String,
)
