package no.nav.mulighetsrommet.api.domain.tiltak

data class TiltakstypeUpdate(
    val tiltakstype: Tiltakstype,
    val events: List<TiltakstypeEvent>,
)
