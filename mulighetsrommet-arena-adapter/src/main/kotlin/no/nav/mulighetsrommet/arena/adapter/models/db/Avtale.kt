package no.nav.mulighetsrommet.arena.adapter.models.db

import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode
import java.time.LocalDateTime
import java.util.*

data class Avtale(
    val id: UUID,
    val avtaleId: Int,
    val aar: Int,
    val lopenr: Int,
    val tiltakskode: String,
    val leverandorId: Int,
    val navn: String,
    val fraDato: LocalDateTime,
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
        Avbrutt,
        Overfort,
        ;

        companion object {
            fun fromArenaAvtalestatuskode(avtalestatuskode: Avtalestatuskode): Status = when (avtalestatuskode) {
                Avtalestatuskode.Planlagt -> Planlagt
                Avtalestatuskode.Gjennomforer -> Aktiv
                Avtalestatuskode.Avsluttet -> Avsluttet
                Avtalestatuskode.Avbrutt -> Avbrutt
                Avtalestatuskode.Overfort -> Overfort
            }
        }
    }
}
