package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.BrukerUtbetalingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottakerType
import no.nav.mulighetsrommet.model.Fakturanummer as FakturanummerValueType
import no.nav.mulighetsrommet.model.Kid as KidValueType

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class TilskuddMottaker {

    @Serializable
    @SerialName("BRUKER")
    data class Bruker(val utbetalingType: BrukerUtbetalingType) : TilskuddMottaker()

    @Serializable
    @SerialName("ARRANGOR")
    data class Arrangor(val referanse: ArrangorReferanse) : TilskuddMottaker()

    fun toType(): TilskuddMottakerType = when (this) {
        is Bruker -> TilskuddMottakerType.BRUKER
        is Arrangor -> TilskuddMottakerType.ARRANGOR
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class ArrangorReferanse {
    @Serializable
    @SerialName("KID")
    data class Kid(val kid: KidValueType?) : ArrangorReferanse()

    @Serializable
    @SerialName("FAKTURANUMMER")
    data class Fakturanummer(val fakturanummer: FakturanummerValueType) : ArrangorReferanse()
}
