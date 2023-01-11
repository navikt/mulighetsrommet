package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null,
    val rettPaaTiltakspenger: Boolean
) {
    companion object {
        fun from(tiltakstype: TiltakstypeDbo) = tiltakstype.run {
            TiltakstypeDto(
                id = id,
                navn = navn,
                arenaKode = tiltakskode,
                fraDato = fraDato,
                tilDato = tilDato,
                rettPaaTiltakspenger = rettPaaTiltakspenger
            )
        }
    }
}

fun isGruppetiltak(tiltakstypeArenaKode: String): Boolean {
    // Enn så lenge så opererer vi med en hardkodet liste over hvilke gjennomføringer vi anser som gruppetiltak
    val gruppetiltak = listOf(
        "ARBFORB",
        "ARBRRHDAG",
        "AVKLARAG",
        "DIGIOPPARB",
        "FORSAMOGRU",
        "FORSFAGGRU",
        "GRUFAGYRKE",
        "GRUPPEAMO",
        "INDJOBSTOT",
        "INDOPPFAG",
        "INDOPPRF",
        "IPSUNG",
        "JOBBK",
        "UTVAOONAV",
        "UTVOPPFOPL",
        "VASV"
    )
    return tiltakstypeArenaKode in gruppetiltak
}
