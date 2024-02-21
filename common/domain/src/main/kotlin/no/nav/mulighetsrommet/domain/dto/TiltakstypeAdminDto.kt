package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Gruppetiltak
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakstypeAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Gruppetiltak?,
    val arenaKode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrertIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistEndretIArenaDato: LocalDateTime,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate,
    val rettPaaTiltakspenger: Boolean,
    val status: Tiltakstypestatus,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
) {
    companion object {
        fun from(tiltakstype: TiltakstypeDbo) = tiltakstype.run {
            TiltakstypeAdminDto(
                id = id,
                navn = navn,
                tiltakskode = tiltakskode,
                arenaKode = arenaKode,
                registrertIArenaDato = registrertDatoIArena,
                sistEndretIArenaDato = sistEndretDatoIArena,
                fraDato = fraDato,
                tilDato = tilDato,
                rettPaaTiltakspenger = rettPaaTiltakspenger,
                status = Tiltakstypestatus.resolveFromDates(LocalDate.now(), fraDato, tilDato),
                sanityId = null,
            )
        }
    }
}
