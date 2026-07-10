package no.nav.mulighetsrommet.admin.navansatt

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class NavAnsattDto(
    @Serializable(with = UUIDSerializer::class)
    val entraObjectId: UUID,
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: Hovedenhet,
    val mobilnummer: String?,
    val epost: String,
    val roller: List<NavAnsattRolleDto>,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}

@Serializable
data class NavAnsattRolleDto(
    val rolle: Rolle,
    val navn: String,
)
