package no.nav.tiltak.historikk.model

import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ArbeidsgiverAvtale(
    val avtaleId: UUID,
    val norskIdent: NorskIdent,
    val organisasjonsnummer: Organisasjonsnummer,
    val tiltakstype: Tiltakskode,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: Status,
    val stillingsprosent: Float?,
    val dagerPerUke: Float?,
    val opprettetTidspunkt: Instant,
    val oppdatertTidspunkt: Instant,
) {
    enum class Status {
        ANNULLERT,
        AVBRUTT,
        PAABEGYNT,
        MANGLER_GODKJENNING,
        KLAR_FOR_OPPSTART,
        GJENNOMFORES,
        AVSLUTTET,
    }
}
