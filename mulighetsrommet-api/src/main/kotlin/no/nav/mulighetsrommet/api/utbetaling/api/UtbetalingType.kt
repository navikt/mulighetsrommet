package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable(with = UtbetalingTypeSerializer::class)
enum class UtbetalingType(val displayName: String, val displayNameLong: String?, val tagName: String?) {
    KORRIGERING("Korrigering", null, "KOR"),
    INVESTERING("Investering", "Utbetaling for investering", "INV"),
    INNSENDING("Innsending", null, null),
    ;

    companion object {
        fun from(utbetaling: Utbetaling): UtbetalingType = when {
            utbetaling.innsender is NavIdent && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_DRIFTSTILSKUDD -> {
                KORRIGERING
            }

            utbetaling.innsender is Arrangor && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_INVESTERINGER -> {
                INVESTERING
            }

            else -> {
                INNSENDING
            }
        }
    }
}

object UtbetalingTypeSerializer : KSerializer<UtbetalingType> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("UtbetalingType") {
            element<String>("type")
            element<String>("displayName")
            element<String?>("displayNameLong")
            element<String?>("tagName")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: UtbetalingType) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            encodeStringElement(descriptor, 1, value.displayName)
            encodeNullableSerializableElement(descriptor, 2, serializer<String>(), value.displayNameLong)
            encodeNullableSerializableElement(descriptor, 3, serializer<String>(), value.tagName)
        }
    }

    override fun deserialize(decoder: Decoder): UtbetalingType {
        var type: String? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }
        }
        return type?.let { UtbetalingType.valueOf(it) }
            ?: throw IllegalArgumentException("Invalid UtbetalingType enum data")
    }
}
