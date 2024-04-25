package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.PersonopplysningFrekvens
import no.nav.mulighetsrommet.domain.dto.PersonopplysningMedBeskrivelse
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltakstypeAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val arenaKode: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: TiltakstypeStatus,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val personopplysninger: Map<PersonopplysningFrekvens, List<PersonopplysningMedBeskrivelse>>,
)
