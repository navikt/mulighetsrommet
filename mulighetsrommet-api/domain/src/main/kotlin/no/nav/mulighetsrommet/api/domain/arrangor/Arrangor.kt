package no.nav.mulighetsrommet.api.domain.arrangor

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

data class Arrangor(
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val organisasjonsform: String?,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val slettetDato: LocalDate? = null,
    val erUtenlandsk: Boolean,
    val kontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
)
