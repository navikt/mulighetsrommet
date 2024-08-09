package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
sealed class TiltakshistorikkAdminDto {
    abstract val id: UUID
    abstract val opphav: Tiltakshistorikk.Opphav
    abstract val startDato: LocalDate?
    abstract val sluttDato: LocalDate?
    abstract val tiltakstypeNavn: String
    abstract val tiltakNavn: String
    abstract val arrangor: Arrangor

    @Serializable
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String?,
    )

    @Serializable
    data class ArenaDeltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        override val tiltakstypeNavn: String,
        override val tiltakNavn: String,
        override val arrangor: Arrangor,
        val status: ArenaDeltakerStatus,
    ) : TiltakshistorikkAdminDto() {
        override val opphav = Tiltakshistorikk.Opphav.ARENA
    }

    @Serializable
    data class GruppetiltakDeltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        override val tiltakstypeNavn: String,
        override val tiltakNavn: String,
        override val arrangor: Arrangor,
        val status: AmtDeltakerStatus,
    ) : TiltakshistorikkAdminDto() {
        override val opphav = Tiltakshistorikk.Opphav.TEAM_KOMET
    }
}
