package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakstypeDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertDatoIArena: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistEndretDatoIArena: LocalDateTime,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate,
    val rettPaaTiltakspenger: Boolean,
)
