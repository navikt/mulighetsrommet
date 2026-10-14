package no.nav.mulighetsrommet.api.domain.personalia

import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.UUID

data class Personalia(
    val deltakerId: UUID,
    private val norskIdent: NorskIdent,
    private val navn: String,
    private val oppfolgingEnhet: NavEnhetDto?,
    private val geografiskEnhet: NavEnhetDto?,
    private val region: NavEnhetDto?,
    val gradering: Gradering,
    val avvistGrunn: AvvistGrunn?,
) {
    fun harTilgang(): Boolean = avvistGrunn == null

    fun navn(): String = if (harTilgang()) {
        navn
    } else {
        when (gradering) {
            Gradering.STRENGT_FORTROLIG_ADRESSE,
            Gradering.STRENGT_FORTROLIG_UTLAND,
            Gradering.FORTROLIG_ADRESSE,
            -> "Adressebeskyttet"

            Gradering.GEOGRAFISK,
            Gradering.SKJERMING,
            -> "Skjermet"

            Gradering.UGRADERT -> navn
        }
    }

    fun norskIdent(): NorskIdent? = if (harTilgang()) {
        norskIdent
    } else {
        null
    }

    fun oppfolgingEnhet(): NavEnhetDto? = oppfolgingEnhet

    fun geografiskEnhet(): NavEnhetDto? = if (harTilgang()) {
        geografiskEnhet
    } else {
        null
    }

    fun region(): NavEnhetDto? = if (harTilgang()) {
        region
    } else {
        null
    }
}
