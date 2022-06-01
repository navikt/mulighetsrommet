package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.DateSerializer
import java.time.LocalDateTime

enum class ArenaDeltakerstatuskode {
    FULLF,
    DELAVB,
    GJENN_AVB,
    JATAKK,
    AVSLAG,
    GJENN_AVL,
    AKTUELL,
    INFOMOETE,
    GJENN,
    NEITAKK,
    IKKAKTUELL,
    TILBUD,
    IKKEM,
    VENTELISTE,
}

@Serializable
data class ArenaTiltakdeltaker(
    val id: Int,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    val deltakerstatuskode: ArenaDeltakerstatuskode,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
