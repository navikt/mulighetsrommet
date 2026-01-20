package no.nav.mulighetsrommet.api.arrangor.model

data class UtenlandskArrangor(
    val bic: String,
    val iban: String,
    val gateNavn: String,
    val by: String,
    val postNummer: String,
    val landKode: String,
    val bankNavn: String,
)
