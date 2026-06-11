package no.nav.mulighetsrommet.api

import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.toAgentHendelse
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.kafka.KafkaProducerRecordQueries
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import org.apache.kafka.common.header.internals.RecordHeaders
import java.time.Instant

class OutboxEventPublisher(session: Session, private val topics: KafkaTopics) {
    val kpr = KafkaProducerRecordQueries(session)

    fun publish(totrinnskontroll: Totrinnskontroll) {
        val hendelse = toHendelse(totrinnskontroll)
        val record = StoredProducerRecord(
            topics.totrinnskontrollTopic,
            totrinnskontroll.entityId.toString().toByteArray(),
            Json.encodeToString(hendelse).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }

    fun publish(dto: TiltaksgjennomforingV2Dto) {
        val record = StoredProducerRecord(
            topics.sisteTiltaksgjennomforingerV2Topic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }

    fun publish(melding: OkonomiBestillingMelding, scheduledAt: Instant? = null) {
        val key = when (melding) {
            is OkonomiBestillingMelding.Annullering -> melding.payload.bestillingsnummer
            is OkonomiBestillingMelding.Bestilling -> melding.payload.bestillingsnummer
            is OkonomiBestillingMelding.Faktura -> melding.payload.bestillingsnummer
            is OkonomiBestillingMelding.GjorOppBestilling -> melding.payload.bestillingsnummer
        }
        val headers = scheduledAt
            ?.let {
                RecordHeaders().add(
                    KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT,
                    it.toString().toByteArray(),
                )
            }?.let {
                KafkaUtils.headersToJson(it)
            }
        val record = StoredProducerRecord(
            topics.okonomiBestillingTopic,
            key.toByteArray(),
            Json.encodeToString(melding).toByteArray(),
            headers,
        )
        kpr.storeRecord(record)
    }

    fun publish(ekstern: TiltakstypeV3Dto) {
        val record = StoredProducerRecord(
            topics.sisteTiltakstyperTopic,
            ekstern.id.toString().toByteArray(),
            Json.encodeToString(ekstern).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }

    private fun toHendelse(totrinnskontroll: Totrinnskontroll): TotrinnskontrollHendelse = TotrinnskontrollHendelse(
        id = totrinnskontroll.id,
        entityId = totrinnskontroll.entityId,
        type = totrinnskontroll.type,
        status = when (totrinnskontroll.status) {
            TotrinnskontrollStatus.TIL_BEHANDLING -> TotrinnskontrollHendelse.Status.TIL_BEHANDLING
            TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollHendelse.Status.SATT_PA_VENT
            TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollHendelse.Status.GODKJENT
            TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollHendelse.Status.RETURNERT
        },
        behandletAv = totrinnskontroll.behandletAv.toAgentHendelse(),
        behandletTidspunkt = totrinnskontroll.behandletTidspunkt,
        besluttetAv = totrinnskontroll.besluttetAv?.toAgentHendelse(),
        besluttetTidspunkt = totrinnskontroll.besluttetTidspunkt,
        aarsaker = totrinnskontroll.aarsaker,
        forklaring = totrinnskontroll.forklaring,
    )
}
