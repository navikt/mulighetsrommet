package no.nav.mulighetsrommet.api

import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries
import no.nav.mulighetsrommet.api.persistence.totrinnskontroll.toTotrinnskontrollHendelse
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.kafka.KafkaProducerRecordQueries
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import org.apache.kafka.common.header.internals.RecordHeaders
import java.time.Instant

class OutboxEventPublisher(session: Session, private val topics: KafkaTopics) {
    val kpr = KafkaProducerRecordQueries(session)
    private val tiltakstypeQueries = TiltakstypeQueries(session)

    fun publish(tiltakstype: Tiltakstype) {
        val dto = requireNotNull(tiltakstypeQueries.getEksternTiltakstype(tiltakstype.id)) {
            "Fant ikke ekstern tiltakstype for id=${tiltakstype.id}"
        }
        val record = StoredProducerRecord(
            topics.sisteTiltakstyperTopic,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }

    fun publish(totrinnskontroll: Totrinnskontroll) {
        val hendelse = totrinnskontroll.toTotrinnskontrollHendelse()
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
}
