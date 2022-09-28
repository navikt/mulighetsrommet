package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import java.time.LocalDateTime

@Serializable
data class DelMedBruker(
    val id: String? = null,
    val bruker_fnr: String,
    val navident: String,
    val tiltaksnummer: String,
    val dialogId: String,
    @Serializable(with = DateSerializer::class)
    val created_at: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val updated_at: LocalDateTime? = null,
    val created_by: String? = null,
    val updated_by: String? = null
)
