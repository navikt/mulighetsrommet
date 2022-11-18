package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Deltaker(
    val tiltaksdeltakerId: Int,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus
)
