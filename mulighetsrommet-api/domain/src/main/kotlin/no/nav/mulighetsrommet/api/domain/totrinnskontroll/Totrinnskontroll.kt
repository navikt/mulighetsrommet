package no.nav.mulighetsrommet.api.domain.totrinnskontroll

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class Totrinnskontroll(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val entityId: UUID,
    val type: TotrinnskontrollType,
    val status: TotrinnskontrollStatus,
    @Serializable(with = AgentSerializer::class)
    val behandletAv: Agent,
    @Serializable(with = InstantSerializer::class)
    val behandletTidspunkt: Instant,
    val aarsaker: List<String>,
    val forklaring: String?,
    @Serializable(with = AgentSerializer::class)
    val besluttetAv: Agent?,
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant?,
) {

    fun kanSettesPaVent(): Boolean {
        return status == TotrinnskontrollStatus.TIL_BEHANDLING
    }

    fun kanTilbakestilles(): Boolean {
        return status == TotrinnskontrollStatus.SATT_PA_VENT
    }

    fun kanBesluttes(): Boolean {
        return status in setOf(TotrinnskontrollStatus.TIL_BEHANDLING, TotrinnskontrollStatus.SATT_PA_VENT)
    }

    fun kanBesluttesAv(agent: Agent): Boolean {
        return !(agent is NavIdent && agent == behandletAv)
    }

    companion object {
        fun opprett(
            id: UUID,
            entityId: UUID,
            type: TotrinnskontrollType,
            behandletAv: Agent,
            aarsaker: List<String> = emptyList(),
            forklaring: String? = null,
        ): Totrinnskontroll = Totrinnskontroll(
            id = id,
            entityId = entityId,
            type = type,
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandletAv = behandletAv,
            behandletTidspunkt = instantAsMicros(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
    }

    fun settPaVent(
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanSettesPaVent()) {
            return alleredeBesluttetError()
        }
        return copy(
            status = TotrinnskontrollStatus.SATT_PA_VENT,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
            aarsaker = aarsaker.ifEmpty { this.aarsaker },
            forklaring = forklaring ?: this.forklaring,
        ).right()
    }

    fun tilbakestill(nyBehandletAv: Agent): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanTilbakestilles()) {
            return TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent.left()
        }
        return copy(
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandletAv = nyBehandletAv,
            behandletTidspunkt = instantAsMicros(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            aarsaker = listOf(),
            forklaring = null,
        ).right()
    }

    fun godkjenn(besluttetAv: Agent): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanBesluttes()) {
            return alleredeBesluttetError()
        }
        if (!kanBesluttesAv(besluttetAv)) {
            return TotrinnskontrollError.KanIkkeBesluttesAvBehandler.left()
        }
        return copy(
            status = TotrinnskontrollStatus.GODKJENT,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
        ).right()
    }

    fun returner(
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<TotrinnskontrollError, Totrinnskontroll> {
        // TODO: ikke tillate systemet å returnere godkjent totrinnskontroll
        //  Vi har et tilfelle der systemet er tillatt å endre fra GODKJENT til RETURNERT, men det mer "riktige"
        //  hadde kanskje heller vært om vi opprettet et nytt innslag i totrinnskontroll-loggen?
        if (!kanBesluttes() && besluttetAv is NavIdent) {
            return alleredeBesluttetError()
        }
        return copy(
            status = TotrinnskontrollStatus.RETURNERT,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
            aarsaker = aarsaker.ifEmpty { this.aarsaker },
            forklaring = forklaring ?: this.forklaring,
        ).right()
    }

    private fun alleredeBesluttetError(): Either<TotrinnskontrollError, Nothing> {
        return TotrinnskontrollError.AlleredeBesluttet(status).left()
    }
}

enum class TotrinnskontrollType {
    TILSAGN_OPPRETTELSE,
    TILSAGN_ANNULLERING,
    TILSAGN_OPPGJOR,
    UTBETALING_LINJE_OPPRETTELSE,
    UTBETALING_AVBRYTELSE,
    ENKELTPLASS_OKONOMI,
    TILSKUDD_OPPRETTELSE,
}

enum class TotrinnskontrollStatus {
    TIL_BEHANDLING,
    SATT_PA_VENT,
    GODKJENT,
    RETURNERT,
}

private fun instantAsMicros(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
