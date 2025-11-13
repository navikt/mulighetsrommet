package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID

data class DetaljerDbo(
    val navn: String,
    val tiltakstypeId: UUID,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: ArrangorDbo?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: AvtaleStatusType,
    val avtaletype: Avtaletype,
    val administratorer: List<NavIdent>,
    val amoKategorisering: AmoKategorisering?,
    val opsjonsmodell: Opsjonsmodell,
    val utdanningslop: UtdanningslopDbo?,
)

data class ArrangorDbo(
    val hovedenhet: UUID,
    val underenheter: List<UUID>,
    val kontaktpersoner: List<UUID>,
)

data class OpsjonsmodellDbo(
    val type: OpsjonsmodellType,
    val opsjonMaksVarighet: LocalDate?,
    val customOpsjonsmodellNavn: String? = null,
)
