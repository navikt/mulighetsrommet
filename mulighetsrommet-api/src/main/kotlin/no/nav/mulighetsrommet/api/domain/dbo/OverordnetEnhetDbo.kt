package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto

data class OverordnetEnhetDbo(
    val organisasjonsnummer: String,
    val navn: String,
    val underenheter: List<VirksomhetDto>,
    val poststed: String? = null,
    val postnummer: String? = null,
)

fun VirksomhetDto.toOverordnetEnhetDbo(): OverordnetEnhetDbo {
    if (overordnetEnhet != null || underenheter == null) {
        throw IllegalArgumentException("Virksomhet $organisasjonsnummer er ikke en full overordnet enhet. Virksomhetsdata: $this")
    }
    return OverordnetEnhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        underenheter = underenheter,
        postnummer = postnummer,
        poststed = poststed,
    )
}
