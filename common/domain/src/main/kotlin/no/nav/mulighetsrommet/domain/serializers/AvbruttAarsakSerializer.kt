package no.nav.mulighetsrommet.domain.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak

class AvbruttAarsakSerializer : KSerializer<AvbruttAarsak> {
    override val descriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): AvbruttAarsak {
        return when (val aarsak = decoder.decodeString()) {
            "ENDRING_HOS_ARRANGOR" -> AvbruttAarsak.EndringHosArrangor
            "BUDSJETT_HENSYN" -> AvbruttAarsak.BudsjettHensys
            "FOR_FAA_DELTAKERE" -> AvbruttAarsak.ForFaaDeltakere
            "FEILREGISTRERING" -> AvbruttAarsak.Feilregistrering
            "FORCE_MAJEURE" -> AvbruttAarsak.Feilregistrering
            "AVBRUTT_I_ARENA" -> AvbruttAarsak.AvbruttIArena
            else -> AvbruttAarsak.Annet(name = aarsak)
        }
    }

    override fun serialize(encoder: Encoder, value: AvbruttAarsak) {
        encoder.encodeString(value.name)
    }
}
