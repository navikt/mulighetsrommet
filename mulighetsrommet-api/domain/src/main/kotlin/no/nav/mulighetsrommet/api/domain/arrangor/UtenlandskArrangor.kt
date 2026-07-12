package no.nav.mulighetsrommet.api.domain.arrangor

data class UtenlandskArrangor(
    val bic: String,
    val iban: String,
    val gateNavn: String,
    val by: String,
    val postNummer: String,
    val landKode: String,
    val bankNavn: String,
)
