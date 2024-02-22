package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import java.time.LocalDate
import java.util.*

data class TiltaksgjennomforingDbo(
    val id: UUID,
    val navn: String,
    val tiltakstypeId: UUID,
    val tiltaksnummer: String?,
    val arrangorOrganisasjonsnummer: String,
    val arrangorKontaktpersoner: List<UUID>,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val apentForInnsok: Boolean,
    val antallPlasser: Int,
    val avtaleId: UUID,
    val administratorer: List<String>,
    val navRegion: String,
    val navEnheter: List<String>,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val stengtFra: LocalDate?,
    val stengtTil: LocalDate?,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktpersonDbo>,
    val stedForGjennomforing: String?,
    val faneinnhold: Faneinnhold?,
    val beskrivelse: String?,
    val deltidsprosent: Double,
    val estimertVentetidVerdi: Int?,
    val estimertVentetidEnhet: String?,
)

data class TiltaksgjennomforingKontaktpersonDbo(
    val navIdent: String,
    val navEnheter: List<String>,
    val beskrivelse: String?,
)
