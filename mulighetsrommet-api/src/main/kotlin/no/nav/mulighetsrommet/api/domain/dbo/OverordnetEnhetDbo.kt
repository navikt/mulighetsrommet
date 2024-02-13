package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import java.time.LocalDate

data class OverordnetEnhetDbo(
    val organisasjonsnummer: String,
    val navn: String,
    val slettetDato: LocalDate?,
    val underenheter: List<VirksomhetDto>,
    val poststed: String?,
    val postnummer: String?,
)

fun VirksomhetDto.toOverordnetEnhetDbo(): OverordnetEnhetDbo {
    if (overordnetEnhet != null || underenheter == null) {
        throw IllegalArgumentException("Virksomhet $organisasjonsnummer er ikke en full overordnet enhet. Virksomhetsdata: $this")
    }

    return OverordnetEnhetDbo(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        slettetDato = slettetDato,
        underenheter = underenheter,
        postnummer = postnummer,
        poststed = poststed,
    )
}
