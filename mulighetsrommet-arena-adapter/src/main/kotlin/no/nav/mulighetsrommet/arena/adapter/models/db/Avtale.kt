package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Avtale(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val aar: Int,
    val lopenr: Int,
    val tiltakskode: String,
    val leverandorId: Int,
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime,
    val ansvarligEnhet: String,
    val rammeavtale: Boolean,
    val status: Status,
    val prisbetingelser: String?,
) {
    enum class Status {
        Planlagt,
        Aktiv,
        Avsluttet,
        Avbrutt;

        companion object {
            fun fromArenaAvtalestatuskode(avtalestatuskode: Avtalestatuskode): Status = when (avtalestatuskode) {
                Avtalestatuskode.Planlagt -> Planlagt
                Avtalestatuskode.Gjennomforer -> Aktiv
                Avtalestatuskode.Avsluttet -> Avsluttet
                Avtalestatuskode.Avbrutt -> Avbrutt
            }
        }
    }
}
