package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode

data class GyldigTilsagnPeriode(
    private val perioder: Map<Tiltakskode, Periode>,
) {
    fun forTiltakskode(tiltakskode: Tiltakskode): Periode? = perioder[tiltakskode]
}
