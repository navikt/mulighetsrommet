package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.brukerutbetaling.OpphorTilskuddUtbetaling
import no.nav.mulighetsrommet.api.brukerutbetaling.UtbetalTilskudd
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.kafka.TotrinnskontrollHendelseDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import java.util.UUID

class TilskuddBrukerUtbetalingConsumer(
    private val service: BrukerUtbetalingService,
) : KafkaTopicConsumer<UUID, TotrinnskontrollHendelse>(
    uuidDeserializer(),
    TotrinnskontrollHendelseDeserializer(),
) {
    override suspend fun consume(key: UUID, message: TotrinnskontrollHendelse) {
        if (message.status != TotrinnskontrollHendelse.Status.GODKJENT) {
            return
        }

        when (message.type) {
            TotrinnskontrollType.TILSKUDD_OPPRETTELSE -> service.utbetalTilskudd(
                message.toUtbetalTilskudd(behandlingId = message.entityId),
            )

            TotrinnskontrollType.TILSKUDD_OPPHOR -> service.opphorTilskuddUtbetaling(
                message.toOpphorTilskuddUtbetaling(behandlingId = message.entityId),
            )

            else -> return
        }
    }
}

private fun TotrinnskontrollHendelse.toUtbetalTilskudd(behandlingId: UUID): UtbetalTilskudd = UtbetalTilskudd(
    behandlingId = behandlingId,
    saksbehandler = (behandletAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
        ?: error("behandletAv must be NavAnsatt"),
    beslutter = (besluttetAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
        ?: error("besluttetAv must be NavAnsatt"),
    besluttetTidspunkt = requireNotNull(besluttetTidspunkt),
)

private fun TotrinnskontrollHendelse.toOpphorTilskuddUtbetaling(behandlingId: UUID): OpphorTilskuddUtbetaling = OpphorTilskuddUtbetaling(
    behandlingId = behandlingId,
    saksbehandler = (behandletAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
        ?: error("behandletAv must be NavAnsatt"),
    beslutter = (besluttetAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
        ?: error("besluttetAv must be NavAnsatt"),
    besluttetTidspunkt = requireNotNull(besluttetTidspunkt),
)
