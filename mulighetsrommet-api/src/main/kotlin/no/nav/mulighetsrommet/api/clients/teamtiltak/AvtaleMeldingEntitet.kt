package no.nav.mulighetsrommet.api.clients.teamtiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AvtaleMeldingEntitet(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val meldingId: UUID? = null,
    val avtaleStatus: AvtaleMeldingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tidspunkt: LocalDateTime? = null,
    val json: String? = null,
    val sendt: Boolean? = null,
    val sendtCompacted: Boolean? = null,
)

@Serializable
enum class AvtaleMeldingStatus(val beskrivelse: String) {
    ANNULLERT("Annullert"),
    AVBRUTT("Avbrutt"),
    PÅBEGYNT("Påbegynt"),
    MANGLER_GODKJENNING("Mangler godkjenning"),
    KLAR_FOR_OPPSTART("Klar for oppstart"),
    GJENNOMFØRES("Gjennomføres"),
    AVSLUTTET("Avsluttet"),
}
