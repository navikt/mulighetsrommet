package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

enum class GjennomforingType {
    ARENA,
    AVTALE,
    ENKELTPLASS,
}

data class GjennomforingDbo(
    val id: UUID,
    val type: GjennomforingType,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
    val avtaleId: UUID? = null,
    val prismodellId: UUID? = null,
    val oppmoteSted: String? = null,
    val faneinnhold: Faneinnhold? = null,
    val beskrivelse: String? = null,
    val estimertVentetidVerdi: Int? = null,
    val estimertVentetidEnhet: String? = null,
    val tilgjengeligForArrangorDato: LocalDate? = null,
    val arenaTiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class GjennomforingArenaDataDbo(
    val id: UUID,
    val tiltaksnummer: Tiltaksnummer? = null,
    val arenaAnsvarligEnhet: String? = null,
)

data class GjennomforingKontaktpersonDbo(
    val navIdent: NavIdent,
    val beskrivelse: String?,
)
