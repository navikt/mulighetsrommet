package no.nav.mulighetsrommet.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

object FloatToIntSerializer : KSerializer<Int> {
    override val descriptor = Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeDouble().roundToInt()
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(value.toString())
    }
}
