package no.nav.mulighetsrommet.api.contracts.totrinnskontroll

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse.Status
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.util.UUID

@Serializable
data class TotrinnskontrollHendelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val status: Status,
    val behandletAv: TotrinnskontrollAgent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val besluttetAv: TotrinnskontrollAgent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val aarsaker: List<String>,
    val forklaring: String?,
) {
    enum class Status {
        TIL_BEHANDLING,
        SATT_PA_VENT,
        GODKJENT,
        RETURNERT,
    }
}

@Serializable
data class TotrinnskontrollHendelseOld(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    @Serializable(with = OldAgentSerializer::class)
    val behandletAv: TotrinnskontrollAgent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    @Serializable(with = OldAgentSerializer::class)
    val besluttetAv: TotrinnskontrollAgent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
    val besluttelse: Besluttelse?,
    val aarsaker: List<String>,
    val forklaring: String?,
) {
    enum class Besluttelse {
        GODKJENT,
        AVVIST,
    }

    fun toNew() = TotrinnskontrollHendelse(
        id = id,
        entityId = entityId,
        type = type,
        behandletTidspunkt = behandletTidspunkt,
        besluttetAv = besluttetAv,
        besluttetTidspunkt = besluttetTidspunkt,
        aarsaker = aarsaker,
        forklaring = forklaring,
        behandletAv = behandletAv,
        status = when (besluttelse) {
            Besluttelse.GODKJENT -> Status.GODKJENT
            Besluttelse.AVVIST -> Status.RETURNERT
            null -> Status.TIL_BEHANDLING
        },
    )
}

private object OldAgentSerializer : KSerializer<TotrinnskontrollAgent> {
    private val delegateSerializer = TotrinnskontrollAgent.serializer()
    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: TotrinnskontrollAgent) = delegateSerializer.serialize(encoder, value)

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): TotrinnskontrollAgent {
        return when (val element = (decoder as kotlinx.serialization.json.JsonDecoder).decodeJsonElement()) {
            is JsonObject -> Json.decodeFromJsonElement(delegateSerializer, element)
            is JsonPrimitive -> element.jsonPrimitive.content.toAgent()
            else -> error("Unexpected JSON element for TotrinnskontrollAgent: $element")
        }
    }

    private fun String.toAgent(): TotrinnskontrollAgent = when (this) {
        "Tiltaksadministrasjon" -> TotrinnskontrollAgent.System("Tiltaksadministrasjon")
        "Arena" -> TotrinnskontrollAgent.System("Arena")
        "Arrangor" -> TotrinnskontrollAgent.Arrangor
        else -> TotrinnskontrollAgent.NavAnsatt(NavIdent(this))
    }
}
