package no.nav.mulighetsrommet.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.mulighetsrommet.model.AvbruttAarsak

object AvbruttAarsakSerializer : KSerializer<AvbruttAarsak> {
    override val descriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): AvbruttAarsak {
        return when (val aarsak = decoder.decodeString()) {
            "ENDRING_HOS_ARRANGOR" -> AvbruttAarsak.EndringHosArrangor
            "BUDSJETT_HENSYN" -> AvbruttAarsak.BudsjettHensyn
            "FOR_FAA_DELTAKERE" -> AvbruttAarsak.ForFaaDeltakere
            "FEILREGISTRERING" -> AvbruttAarsak.Feilregistrering
            "AVBRUTT_I_ARENA" -> AvbruttAarsak.AvbruttIArena
            else -> AvbruttAarsak.Annet(aarsak)
        }
    }

    override fun serialize(encoder: Encoder, value: AvbruttAarsak) {
        when (value) {
            is AvbruttAarsak.Annet -> encoder.encodeString(value.beskrivelse)
            else -> encoder.encodeString(value.name)
        }
    }
}
