package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

data class AvtaleDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val avtalenummer: String?,
    val leverandorVirksomhetId: UUID,
    val leverandorUnderenheter: List<UUID>,
    val leverandorKontaktpersonId: UUID?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val navEnheter: List<String>,
    val avtaletype: Avtaletype,
    val prisbetingelser: String?,
    val antallPlasser: Int?,
    val url: String?,
    val administratorer: List<NavIdent>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)
