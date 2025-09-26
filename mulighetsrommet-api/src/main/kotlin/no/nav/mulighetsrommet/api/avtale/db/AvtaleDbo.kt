package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

data class AvtaleDbo(
    val id: UUID,
    val status: AvtaleStatusType,
    val avtalenummer: String?,
    val detaljer: DetaljerDbo,
    val prismodell: PrismodellDbo,
    val veilederinformasjon: VeilederinformasjonDbo,
    val personvern: PersonvernDbo,
)

data class DetaljerDbo(
    val navn: String,
    val tiltakstypeId: UUID,
    val sakarkivnummer: String?,
    val arrangor: ArrangorDbo?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val avtaletype: Avtaletype,
    val administratorer: List<NavIdent>,
    val amoKategorisering: AmoKategorisering?,
    val opsjonsmodell: OpsjonsmodellDbo,
    val utdanningslop: UtdanningslopDbo?,
)
data class ArrangorDbo(
    val hovedenhet: UUID,
    val underenheter: List<UUID>,
    val kontaktpersoner: List<UUID>,
)

data class OpsjonsmodellDbo(
    val type: OpsjonsmodellType,
    val maksVarighet: LocalDate?,
    val customNavn: String?,
)

data class VeilederinformasjonDbo(
    val redaksjoneltInnhold: RedaksjoneltInnholdDbo?,
    val navEnheter: Set<NavEnhetNummer>,
)

data class RedaksjoneltInnholdDbo(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)

data class PersonvernDbo(
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
)
