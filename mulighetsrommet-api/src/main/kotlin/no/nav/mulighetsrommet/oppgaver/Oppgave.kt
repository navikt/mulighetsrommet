package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Oppgave(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: OppgaveType,
    val enhet: OppgaveEnhet?,
    val title: String,
    val description: String? = null,
    val tiltakstype: OppgaveTiltakstype,
    val link: OppgaveLink,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val oppgaveIcon: OppgaveIcon,
)

@Serializable
data class OppgaveEnhet(
    val nummer: NavEnhetNummer,
    val navn: String,
)

@Serializable
data class OppgaveTiltakstype(
    val tiltakskode: Tiltakskode,
    val navn: String,
)

enum class OppgaveIcon {
    TILSAGN,
    UTBETALING,
}

@Serializable
data class OppgaveLink(
    val linkText: String,
    val link: String,
)
