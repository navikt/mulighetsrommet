package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.util.*

data class AvtaleDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val avtalenummer: String? = null,
    val leverandorOrganisasjonsnummer: String,
    val leverandorUnderenheter: List<String>,
    val leverandorKontaktpersonId: UUID? = null,
    val startDato: LocalDate,
    val sluttDato: LocalDate,
    val arenaAnsvarligEnhet: String?,
    val navRegion: String,
    val navEnheter: List<String>,
    val avtaletype: Avtaletype,
    val avslutningsstatus: Avslutningsstatus,
    val opphav: ArenaMigrering.Opphav,
    val prisbetingelser: String? = null,
    val antallPlasser: Int? = null,
    val url: String? = null,
    val ansvarlige: List<String> = emptyList(),
)
