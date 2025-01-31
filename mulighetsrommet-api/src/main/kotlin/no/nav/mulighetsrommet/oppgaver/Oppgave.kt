package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class OppgaveType {
    TILSAGN_TIL_BESLUTNING,
    TILSAGN_TIL_ANNULLERING,
    TILSAGN_RETURNERT_AV_BESLUTTER,
}

@Serializable
data class Oppgave(
    val type: OppgaveType,
    val title: String,
    val description: String? = null,
    val tiltakstype: Tiltakskode,
    val link: OppgaveLink,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val deadline: LocalDateTime,
)

@Serializable
data class OppgaveLink(
    val linkText: String,
    val link: String,
)
