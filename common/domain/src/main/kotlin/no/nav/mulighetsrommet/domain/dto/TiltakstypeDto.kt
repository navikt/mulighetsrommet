package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String
) {
    companion object {
        fun from(tiltakstype: TiltakstypeDbo) = tiltakstype.run {
            TiltakstypeDto(
                id = id,
                navn = navn,
                arenaKode = tiltakskode
            )
        }
    }
}


