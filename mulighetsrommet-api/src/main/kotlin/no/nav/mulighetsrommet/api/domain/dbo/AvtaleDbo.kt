package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AvtaleDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val avtalenummer: String?,
    val leverandorOrganisasjonsnummer: String,
    val leverandorUnderenheter: List<String>,
    val leverandorKontaktpersonId: UUID?,
    val startDato: LocalDate,
    val sluttDato: LocalDate,
    val navEnheter: List<String>,
    val avtaletype: Avtaletype,
    val opphav: ArenaMigrering.Opphav,
    val prisbetingelser: String?,
    val antallPlasser: Int?,
    val url: String?,
    val administratorer: List<String>,
    val updatedAt: LocalDateTime,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)
