package no.nav.mulighetsrommet.domain.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import java.time.LocalDateTime

object AvtaleStatusSerializer : KSerializer<AvtaleStatus> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AvtaleStatus") {
            element("name", String.serializer().descriptor)
            element("tidspunkt", LocalDateTimeSerializer.descriptor)
            element("aarsak", AvbruttAarsakSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): AvtaleStatus {
        var name: String? = null
        var tidspunkt: LocalDateTime? = null
        var aarsak: AvbruttAarsak? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, 0)
                    1 -> tidspunkt = decodeSerializableElement(descriptor, 1, LocalDateTimeSerializer)
                    2 -> aarsak = decodeSerializableElement(descriptor, 1, AvbruttAarsakSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        requireNotNull(name)
        return AvtaleStatus.fromString(name!!, tidspunkt, aarsak)
    }

    override fun serialize(encoder: Encoder, value: AvtaleStatus) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.enum.name)
            when (value) {
                is AvtaleStatus.AVBRUTT -> {
                    encodeSerializableElement(descriptor, 1, LocalDateTimeSerializer, value.tidspunkt)
                    encodeSerializableElement(descriptor, 2, AvbruttAarsakSerializer, value.aarsak)
                }
                else -> {}
            }
        }
    }
}
