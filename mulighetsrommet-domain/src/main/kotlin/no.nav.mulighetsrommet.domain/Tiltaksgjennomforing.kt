package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = null,
    val navn: String,
    val tiltakskode: String,
    val tiltaksnummer: Int,
    val aar: Int?
)
