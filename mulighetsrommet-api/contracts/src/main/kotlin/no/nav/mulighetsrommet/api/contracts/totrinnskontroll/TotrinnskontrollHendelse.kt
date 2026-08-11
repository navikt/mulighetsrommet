package no.nav.mulighetsrommet.api.contracts.totrinnskontroll

import kotlinx.serialization.Serializable
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
    val behandletAv: String,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val besluttetAv: String?,
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
        besluttetAv = besluttetAv?.toAgent(),
        besluttetTidspunkt = besluttetTidspunkt,
        aarsaker = aarsaker,
        forklaring = forklaring,
        behandletAv = behandletAv.toAgent(),
        status = when (besluttelse) {
            Besluttelse.GODKJENT -> Status.GODKJENT
            Besluttelse.AVVIST -> Status.RETURNERT
            null -> Status.TIL_BEHANDLING
        },
    )

    private fun String.toAgent(): TotrinnskontrollAgent = when (this) {
        "Tiltaksadministrasjon" -> TotrinnskontrollAgent.System("Tiltaksadministrasjon")
        "Arena" -> TotrinnskontrollAgent.System("Arena")
        "Arrangor" -> TotrinnskontrollAgent.Arrangor
        else -> TotrinnskontrollAgent.NavAnsatt(NavIdent(this))
    }
}
