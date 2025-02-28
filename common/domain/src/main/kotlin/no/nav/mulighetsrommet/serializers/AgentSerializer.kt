package no.nav.mulighetsrommet.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.mulighetsrommet.model.*

object AgentSerializer : KSerializer<Agent> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Agent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Agent) {
        when (value) {
            is Tiltaksadministrasjon -> encoder.encodeString("Tiltaksadministrasjon")
            is Arena -> encoder.encodeString("Arena")
            is Arrangor -> encoder.encodeString("Arrangor")
            is NavIdent -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Agent {
        return when (val string = decoder.decodeString()) {
            "Tiltaksadministrasjon" -> Tiltaksadministrasjon
            "Arena" -> Arena
            "Arrangor" -> Arrangor
            else -> NavIdent(string)
        }
    }
}
