package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.routes.v1.Opsjonsmodell
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.util.*

data class AvtaleDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val avtalenummer: String?,
    val websaknummer: Websaknummer?,
    val arrangorId: UUID,
    val arrangorUnderenheter: List<UUID>,
    val arrangorKontaktpersoner: List<UUID>,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val opsjonMaksVarighet: LocalDate?,
    val navEnheter: List<String>,
    val avtaletype: Avtaletype,
    val prisbetingelser: String?,
    val antallPlasser: Int?,
    val administratorer: List<NavIdent>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val amoKategorisering: AmoKategorisering?,
    val opsjonsmodell: Opsjonsmodell?,
    val customOpsjonsmodellNavn: String?,
)
