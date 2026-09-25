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
    val behandling: Behandling,
    val beslutning: Beslutning?,
) {
    @Serializable
    data class Behandling(
        @Serializable(with = AgentSerializer::class)
        val utfortAv: Agent,
        @Serializable(with = InstantSerializer::class)
        val tidspunkt: Instant,
        val begrunnelse: String?,
        val aarsaker: List<String>,
    )

    @Serializable
    data class Beslutning(
        @Serializable(with = AgentSerializer::class)
        val utfortAv: Agent,
        @Serializable(with = InstantSerializer::class)
        val tidspunkt: Instant,
        val begrunnelse: String?,
        val aarsaker: List<String>,
    )

    init {
        require((status == TotrinnskontrollStatus.TIL_BEHANDLING) == (beslutning == null)) {
            "Beslutning må være satt hvis og bare hvis status er besluttet"
        }
    }

    fun kanSettesPaVent(): Boolean {
        return status == TotrinnskontrollStatus.TIL_BEHANDLING
    }

    fun kanTilbakestilles(): Boolean {
        return status == TotrinnskontrollStatus.SATT_PA_VENT
    }

    fun kanBesluttes(): Boolean {
        return status in setOf(TotrinnskontrollStatus.TIL_BEHANDLING, TotrinnskontrollStatus.SATT_PA_VENT)
    }

    fun kanBehandlesAv(agent: Agent): Boolean {
        return !(agent is NavIdent && agent == behandling.utfortAv)
    }

    companion object {
        fun opprett(
            id: UUID,
            entityId: UUID,
            type: TotrinnskontrollType,
            behandletAv: Agent,
            behandletBegrunnelse: String? = null,
            behandletAarsaker: List<String> = emptyList(),
        ): Totrinnskontroll = Totrinnskontroll(
            id = id,
            entityId = entityId,
            type = type,
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandling = Behandling(
                utfortAv = behandletAv,
                tidspunkt = instantAsMicros(),
                begrunnelse = behandletBegrunnelse,
                aarsaker = behandletAarsaker,
            ),
            beslutning = null,
        )
    }

    fun settPaVent(
        besluttetAv: Agent,
        besluttetBegrunnelse: String? = null,
        besluttetAarsaker: List<String> = emptyList(),
    ): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanSettesPaVent()) {
            return alleredeBesluttetError()
        }
        return copy(
            status = TotrinnskontrollStatus.SATT_PA_VENT,
            beslutning = Beslutning(
                utfortAv = besluttetAv,
                tidspunkt = instantAsMicros(),
                begrunnelse = besluttetBegrunnelse,
                aarsaker = besluttetAarsaker,
            ),
        ).right()
    }

    fun tilbakestill(nyBehandletAv: Agent): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanTilbakestilles()) {
            return TotrinnskontrollError.KanBareTilbakestillesNarSattPaVent.left()
        }
        return copy(
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandling = Behandling(
                utfortAv = nyBehandletAv,
                tidspunkt = instantAsMicros(),
                begrunnelse = null,
                aarsaker = emptyList(),
            ),
            beslutning = null,
        ).right()
    }

    fun godkjenn(besluttetAv: Agent): Either<TotrinnskontrollError, Totrinnskontroll> {
        if (!kanBesluttes()) {
            return alleredeBesluttetError()
        }
        if (!kanBehandlesAv(besluttetAv)) {
            return TotrinnskontrollError.KanIkkeBesluttesAvBehandler.left()
        }
        return copy(
            status = TotrinnskontrollStatus.GODKJENT,
            beslutning = Beslutning(
                utfortAv = besluttetAv,
                tidspunkt = instantAsMicros(),
                begrunnelse = null,
                aarsaker = emptyList(),
            ),
        ).right()
    }

    fun returner(
        besluttetAv: Agent,
        besluttetBegrunnelse: String? = null,
        besluttetAarsaker: List<String> = emptyList(),
    ): Either<TotrinnskontrollError, Totrinnskontroll> {
        // TODO: ikke tillate systemet å returnere godkjent totrinnskontroll
        //  Vi har et tilfelle der systemet er tillatt å endre fra GODKJENT til RETURNERT, men det mer "riktige"
        //  hadde kanskje heller vært om vi opprettet et nytt innslag i totrinnskontroll-loggen?
        if (!kanBesluttes() && besluttetAv is NavIdent) {
            return alleredeBesluttetError()
        }
        return copy(
            status = TotrinnskontrollStatus.RETURNERT,
            beslutning = Beslutning(
                utfortAv = besluttetAv,
                tidspunkt = instantAsMicros(),
                begrunnelse = besluttetBegrunnelse,
                aarsaker = besluttetAarsaker,
            ),
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
    ENKELTPLASS_PRISENDRING,
    TILSKUDD_OPPRETTELSE,
    TILSKUDD_OPPHOR,
}

enum class TotrinnskontrollStatus {
    TIL_BEHANDLING,
    SATT_PA_VENT,
    RETURNERT,
    GODKJENT,
}

private fun instantAsMicros(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
