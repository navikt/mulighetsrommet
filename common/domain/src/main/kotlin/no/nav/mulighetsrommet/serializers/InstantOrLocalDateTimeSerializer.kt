package no.nav.mulighetsrommet.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Bakoverkompatibel serializer for [Instant] som også håndterer deserialisering fra [LocalDateTime] i Oslo-tid.
 * Skal på sikt slettes, men beholdes så lenge det finnes melding til/fra tiltaksokonomi-tjenesten som inneholder
 * serialiserte LocalDateTimes.
 */
object InstantOrLocalDateTimeSerializer : KSerializer<Instant> {
    private val osloZone = ZoneId.of("Europe/Oslo")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantOrLocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        val str = decoder.decodeString()
        return try {
            Instant.parse(str)
        } catch (_: DateTimeParseException) {
            LocalDateTime.parse(str).atZone(osloZone).toInstant()
        }
    }
}
