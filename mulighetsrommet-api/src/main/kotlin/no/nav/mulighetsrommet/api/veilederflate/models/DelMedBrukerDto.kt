package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DelMedBrukerDto(
    val id: Int,
    val norskIdent: NorskIdent,
    val navIdent: NavIdent,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID?,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val deltFraFylke: NavEnhetNummer?,
    val deltFraEnhet: NavEnhetNummer?,
)
