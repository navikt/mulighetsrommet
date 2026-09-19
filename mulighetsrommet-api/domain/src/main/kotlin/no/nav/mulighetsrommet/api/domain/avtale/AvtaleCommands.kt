package no.nav.mulighetsrommet.api.domain.avtale

import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

data class OpprettAvtaleCommand(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val navn: String,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: Avtale.Arrangor?,
    val startDato: LocalDate,
    val opsjonsmodell: Opsjonsmodell,
    val avtaletype: Avtaletype,
    val administratorer: Set<NavIdent>,
    val veilederinfo: Avtale.VeilederInfo,
    val personvern: Avtale.Personvern,
    val opplaring: OpplaringKategorisering?,
    val prisinfo: Avtale.Prisinfo,
)

data class OppdaterAvtaledetaljerCommand(
    val navn: String,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: Avtale.Arrangor?,
    val startDato: LocalDate,
    val opsjonsmodell: Opsjonsmodell,
    val avtaletype: Avtaletype,
    val administratorer: Set<NavIdent>,
    val opplaring: OpplaringKategorisering?,
)

sealed interface RegistrerOpsjonCommand {
    val registrertAv: NavIdent

    data class UtloseOpsjon(
        val nySluttDato: LocalDate,
        override val registrertAv: NavIdent,
    ) : RegistrerOpsjonCommand

    data class SkalIkkeUtloseOpsjon(
        override val registrertAv: NavIdent,
    ) : RegistrerOpsjonCommand
}
