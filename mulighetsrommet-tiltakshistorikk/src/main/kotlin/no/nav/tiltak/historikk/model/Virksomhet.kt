package no.nav.tiltak.historikk.model

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate

data class Virksomhet(
    val organisasjonsnummer: Organisasjonsnummer,
    val overordnetEnhetOrganisasjonsnummer: Organisasjonsnummer?,
    val navn: String?,
    val organisasjonsform: String?,
    val slettetDato: LocalDate?,
)
