package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val tiltaksnummer: String?,
    val virksomhetsnummer: String,
    val virksomhetsnavn: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate? = null,
    val arenaAnsvarligEnhet: String?,
    val status: Tiltaksgjennomforingsstatus,
    val tilgjengelighet: TiltaksgjennomforingDbo.Tilgjengelighetsstatus,
    val estimertVentetid: String? = null,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID? = null,
    val ansvarlig: String?,
    val navEnheter: List<NavEnhet>,
    val navRegion: String? = null,
    val sanityId: String?,
    val oppstart: TiltaksgjennomforingDbo.Oppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val oppstartsdato: LocalDate? = null,
    val opphav: ArenaMigrering.Opphav,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate? = null,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktperson> = emptyList(),
    val lokasjon: String? = null,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )
}
