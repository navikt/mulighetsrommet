package no.nav.mulighetsrommet.api.veilederflate

import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface VeilederflateTiltakQueryHandler {
    fun get(id: UUID): VeilederflateTiltakDbo?

    fun getAll(
        innsatsgruppe: Innsatsgruppe,
        brukersEnheter: List<NavEnhetNummer>,
        search: String? = null,
        apentForPamelding: Boolean? = null,
        tiltakskoder: List<Tiltakskode>? = null,
        erSykmeldtMedArbeidsgiver: Boolean = false,
    ): List<VeilederflateTiltakDbo>

    fun getAllTiltakDokument(
        brukersEnheter: List<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>? = null,
    ): List<VeilederflateTiltakDokument>

    fun getTiltakDokument(id: UUID): VeilederflateTiltakDokument?
}
