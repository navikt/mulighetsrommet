package no.nav.mulighetsrommet.api.domain.dbo

import java.time.LocalDateTime
import java.util.*

data class UtkastDbo(
    val id: UUID,
    val opprettetAv: String,
    val utkastData: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val type: Utkasttype,
)

enum class Utkasttype {
    Avtale, Tiltaksgjennomforing
}
