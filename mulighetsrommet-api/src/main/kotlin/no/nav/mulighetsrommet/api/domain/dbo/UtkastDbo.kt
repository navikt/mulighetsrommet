package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.json.JsonElement
import java.time.LocalDateTime
import java.util.*

data class UtkastDbo(
    val id: UUID?,
    val avtaleId: UUID?,
    val opprettetAv: String,
    val utkastData: JsonElement,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val type: Utkasttype,
)

enum class Utkasttype {
    Avtale,
    Tiltaksgjennomforing,
}
