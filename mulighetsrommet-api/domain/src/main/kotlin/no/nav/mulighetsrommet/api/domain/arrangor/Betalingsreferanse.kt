package no.nav.mulighetsrommet.api.domain.arrangor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.Kid

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface Betalingsreferanse {
    @Serializable
    @SerialName("KidNummer")
    data class KidNummer(
        val kid: Kid,
    ) : Betalingsreferanse

    @Serializable
    @SerialName("MeldingTilMottaker")
    data class MeldingTilMottaker(
        val melding: String,
    ) : Betalingsreferanse
}
