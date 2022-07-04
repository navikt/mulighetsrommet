package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable

class BrukerService {
    fun hentBrukerdata(fnr: String): Brukerdata {
        return Brukerdata(
            fnr = fnr,
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
        )
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe
)

enum class Innsatsgruppe(val verdi: String) {
    STANDARD_INNSATS("Standard innsats"),
    SITUASJONSBESTEMT_INNSATS("Situasjonsbestemt innsats"),
    SPESIELT_TILPASSET_INNSATS("Spesielt tilpasset innsats"),
    VARIG_TILPASSET_INNSATS("Varig tilpasset innsats"),
}
