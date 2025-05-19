package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakDeltMedBruker(
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val dialogId: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakId: UUID,
    val tiltakstype: Tiltakstype,
) {
    @Serializable
    data class Tiltakstype(
        val tiltakskode: Tiltakskode?,
        val arenakode: String?,
        val navn: String,
    )
}
