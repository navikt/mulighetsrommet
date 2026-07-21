package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import java.time.LocalDate
import java.util.UUID

data class DetaljerDbo(
    val navn: String,
    val tiltakstypeId: UUID,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: AvtaleArrangorDbo?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: AvtaleStatusType,
    val avtaletype: Avtaletype,
    val administratorer: List<NavIdent>,
    val opplaringKategorisering: OpplaringKategorisering?,
    val opsjonsmodell: Opsjonsmodell,
)

data class AvtaleArrangorDbo(
    val hovedenhet: UUID,
    val underenheter: List<UUID>,
    val kontaktpersoner: List<UUID>,
)
