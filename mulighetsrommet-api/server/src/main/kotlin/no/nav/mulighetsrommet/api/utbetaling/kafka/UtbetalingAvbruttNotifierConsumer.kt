package no.nav.mulighetsrommet.api.utbetaling.kafka

import arrow.core.nonEmptyListOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class UtbetalingAvbruttNotifierConsumer(
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        val totrinnskontrollHendelse = JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse?>(message)
        if (totrinnskontrollHendelse == null || totrinnskontrollHendelse.type != TotrinnskontrollType.UTBETALING_AVBRYTELSE) {
            return
        }

        if (totrinnskontrollHendelse.status == TotrinnskontrollHendelse.Status.RETURNERT) {
            informerSaksbehandlerAvslattAvbytelse(totrinnskontrollHendelse)
        }
    }

    private fun informerSaksbehandlerAvslattAvbytelse(behandling: TotrinnskontrollHendelse) = db.transaction {
        val utbetaling = queries.utbetaling.getOrError(behandling.entityId)
        val besluttetAv = behandling.besluttetAv
        val behandletAv = behandling.behandletAv
        requireNotNull(besluttetAv) {
            "Forventet at besluttet av var populert for en returnert totrinnskontroll"
        }
        if (behandletAv !is TotrinnskontrollAgent.NavAnsatt) {
            return@transaction
        }

        val beslutterNavn = getAgentNavn(besluttetAv)
        val notification = ScheduledNotification(
            title = "Et utbetalingskrav du sendte til avbrytelse er blitt avslått",
            description = listOf(
                "$beslutterNavn avslo avbrytelsen av utbetalingen til ${utbetaling.arrangor.navn} for tiltaket ${utbetaling.getTiltaksnavn()}.",
                "Gjelder utbetalingsperioden ${utbetaling.periode.formatPeriode()}.",
                "Kontakt $beslutterNavn om dette er feil.",
            ).joinToString(" "),
            metadata = NotificationMetadata(
                linkText = "Gå til utbetaling",
                link = "/gjennomforinger/${utbetaling.gjennomforing.id}/utbetalinger/${utbetaling.id}",
            ),
            createdAt = Instant.now(),
            targets = nonEmptyListOf(behandletAv.navIdent),
        )
        queries.notifications.insert(notification)
    }

    private fun getAgentNavn(agent: TotrinnskontrollAgent): String = when (agent) {
        TotrinnskontrollAgent.Arrangor -> throw IllegalStateException("Arrangør kan ikke være en beslutter")

        is TotrinnskontrollAgent.NavAnsatt -> {
            val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(agent.navIdent)
            beslutterAnsatt?.displayName() ?: agent.navIdent.value
        }

        is TotrinnskontrollAgent.System -> agent.system
    }
}
