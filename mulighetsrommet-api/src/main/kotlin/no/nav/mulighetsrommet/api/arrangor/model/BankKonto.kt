package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.Kontonummer

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BankKonto {
    @Serializable
    @SerialName("BBan")
    data class BBan(
        val kontonummer: Kontonummer,
    ) : BankKonto()

    @Serializable
    @SerialName("IBan")
    data class IBan(
        val bic: String,
        val iban: String,
        val bankNavn: String,
        val bankLandKode: String,
    ) : BankKonto()
}
