package no.nav.mulighetsrommet.api.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.kafka.ProcessingUtils.getArenaDateFormat
import no.nav.mulighetsrommet.api.kafka.ProcessingUtils.removeDoubleQuotes
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EventProcessor(private val topicMap: Map<String, String>, private val tiltakstypeService: TiltakstypeService) {

    private val logger = LoggerFactory.getLogger(EventProcessor::class.java)

    suspend fun process(record: ConsumerRecord<String, String>) {
        when (record.topic()) {
            topicMap["tiltakendret"] -> insertTiltakstype(record.value())
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: ${record.topic()}")
        }
    }

    /**
     * Gjør om string value fra arena event til Json. Henter så ut "after" JsonObject.
     * Mapper dette til tiltakstype DTO og kaller tiltakstypeService for opprettelse.
     */
    private suspend fun insertTiltakstype(recordValue: String) {
        logger.debug("Processing event from topic: ${topicMap["tiltakendret"]} (insert)")
        val arenaTiltakstypeJson = Json.parseToJsonElement(recordValue).jsonObject.get("after")?.jsonObject!!
        val tiltakstype = toTiltakstypeFromArenaJson(arenaTiltakstypeJson)
        tiltakstypeService.createTiltakstype(tiltakstype)
    }

    private fun toTiltakstypeFromArenaJson(json: JsonObject): Tiltakstype {
        return Tiltakstype(
            navn = removeDoubleQuotes(json["TILTAKSNAVN"].toString()),
            innsatsgruppe = 1,
            tiltakskode = Tiltakskode.valueOf(
                removeDoubleQuotes(json["TILTAKSKODE"].toString())
            ),
            fraDato = LocalDateTime.parse(
                removeDoubleQuotes(json["DATO_FRA"].toString()),
                getArenaDateFormat()
            ),
            tilDato = LocalDateTime.parse(
                removeDoubleQuotes(json["DATO_TIL"].toString()),
                getArenaDateFormat()
            ),
            createdBy = removeDoubleQuotes(json["REG_USER"].toString()),
            createdAt = LocalDateTime.parse(
                removeDoubleQuotes(json["REG_DATO"].toString()),
                getArenaDateFormat()
            )
        )
    }
}
