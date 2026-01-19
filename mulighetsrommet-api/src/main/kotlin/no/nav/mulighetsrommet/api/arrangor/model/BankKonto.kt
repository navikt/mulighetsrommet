package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Kontonummer

@Serializable
sealed class BankKonto {
    @Serializable
    data class BBan(
        val kontonummer: Kontonummer,
    ) : BankKonto()

    @Serializable
    data class IBan(
        val bic: String,
        val iban: String,
        val bankNavn: String,
        val bankLandKode: String,
    ) : BankKonto()
}
