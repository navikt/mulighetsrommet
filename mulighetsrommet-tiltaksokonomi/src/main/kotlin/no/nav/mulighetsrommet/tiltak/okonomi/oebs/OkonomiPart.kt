package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.mulighetsrommet.model.NavIdent

@Serializable(with = PartSerializer::class)
sealed class OkonomiPart {
    data class NavAnsatt(val navIdent: NavIdent) : OkonomiPart()

    data object Tiltaksadministrasjon : OkonomiPart()
}

object PartSerializer : KSerializer<OkonomiPart> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Part", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OkonomiPart) {
        val stringValue = when (value) {
            is OkonomiPart.NavAnsatt -> value.navIdent.toString()
            OkonomiPart.Tiltaksadministrasjon -> "TILTAKSADMINISTRASJON"
        }
        encoder.encodeString(stringValue)
    }

    override fun deserialize(decoder: Decoder): OkonomiPart {
        val stringValue = decoder.decodeString()
        return if (stringValue == "TILTAKSADMINISTRASJON") {
            OkonomiPart.Tiltaksadministrasjon
        } else {
            OkonomiPart.NavAnsatt(NavIdent(stringValue))
        }
    }
}
