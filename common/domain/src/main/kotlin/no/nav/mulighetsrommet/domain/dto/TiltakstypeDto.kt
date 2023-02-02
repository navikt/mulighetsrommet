package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistEndretIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate,
    val rettPaaTiltakspenger: Boolean
) {
    companion object {
        fun from(tiltakstype: TiltakstypeDbo) = tiltakstype.run {
            TiltakstypeDto(
                id = id,
                navn = navn,
                arenaKode = tiltakskode,
                registrertIArenaDato = registrertDatoIArena,
                sistEndretIArenaDato = sistEndretDatoIArena,
                fraDato = fraDato,
                tilDato = tilDato,
                rettPaaTiltakspenger = rettPaaTiltakspenger
            )
        }
    }
}


