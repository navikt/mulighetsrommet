package no.nav.tiltak.historikk.model

import java.util.UUID

data class Tiltakstype(
    val navn: String,
    val tiltakskode: String?,
    val arenaTiltakskode: String?,
    val tiltakstypeId: UUID,
)
