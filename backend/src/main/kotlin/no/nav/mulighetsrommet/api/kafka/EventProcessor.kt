package no.nav.mulighetsrommet.api.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventProcessor(private val topicMap: Map<String, String>, private val tiltakstypeService: TiltakstypeService) {

    private val logger = LoggerFactory.getLogger(EventProcessor::class.java)

    suspend fun process(record: ConsumerRecord<String, String>) {
        when (record.topic()) {
            topicMap["tiltakendret"] -> insertTiltakstype(record.value())
        }
    }

    private suspend fun insertTiltakstype(recordValue: String) {
        val arenaTiltakstype = Json.parseToJsonElement(recordValue).jsonObject.get("after")?.jsonObject
        val newTiltakstype = Tiltakstype(
            navn = arenaTiltakstype?.get("TILTAKSNAVN").toString().replace("\"", ""),
            innsatsgruppe = 1,
            tiltakskode = Tiltakskode.valueOf(arenaTiltakstype?.get("TILTAKSKODE").toString().replace("\"", "")),
            fraDato = LocalDateTime.parse(arenaTiltakstype?.get("DATO_FRA").toString().replace("\"", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            tilDato = LocalDateTime.parse(arenaTiltakstype?.get("DATO_TIL").toString().replace("\"", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            createdBy = arenaTiltakstype?.get("REG_USER").toString().replace("\"", ""),
            createdAt = LocalDateTime.parse(arenaTiltakstype?.get("REG_DATO").toString().replace("\"", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )
        println(newTiltakstype)
        tiltakstypeService.createTiltakstype(newTiltakstype)
    }
}
