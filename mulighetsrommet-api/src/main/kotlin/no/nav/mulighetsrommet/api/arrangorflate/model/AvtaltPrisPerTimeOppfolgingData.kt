package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode

data class AvtaltPrisPerTimeOppfolgingData(
    val satser: Set<SatsPeriode>,
    val stengtHosArrangor: Set<StengtPeriode>,
    val deltakere: List<Deltaker>,
    val deltakelsePerioder: Set<DeltakelsePeriode>,
)
