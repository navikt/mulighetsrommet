package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DelMedBrukerDbo(
    val id: String? = null,
    val norskIdent: NorskIdent,
    val navident: String,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID? = null,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val tiltakstypeNavn: String?,
    val deltFraFylke: NavEnhetNummer?,
    val deltFraEnhet: NavEnhetNummer?,
)
