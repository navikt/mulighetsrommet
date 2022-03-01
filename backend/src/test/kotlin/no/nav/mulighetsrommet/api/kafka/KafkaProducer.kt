package no.nav.mulighetsrommet.api.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

// Utrekk av en tilfeldig melding fra vår kafka-manager
val topic = """
            {
              "table": "SIAMO.TILTAK",
              "op_type": "I",
              "op_ts": "2021-11-18 11:32:21.953855",
              "current_ts": "2021-11-18 12:48:35.208001",
              "pos": "00000000000000038901",
              "after": {
                "TILTAKSNAVN": "2-årig opplæringstiltak",
                "TILTAKSGRUPPEKODE": "UTFAS",
                "REG_DATO": "2015-12-30 08:42:06",
                "REG_USER": "SIAMO",
                "MOD_DATO": "2021-04-09 09:21:26",
                "MOD_USER": "SIAMO",
                "TILTAKSKODE": "MENTOR",
                "DATO_FRA": "2016-01-01 00:00:00",
                "DATO_TIL": "2019-06-30 00:00:00",
                "AVSNITT_ID_GENERELT": null,
                "STATUS_BASISYTELSE": "J",
                "ADMINISTRASJONKODE": "IND",
                "STATUS_KOPI_TILSAGN": "J",
                "ARKIVNOKKEL": "533",
                "STATUS_ANSKAFFELSE": "J",
                "MAKS_ANT_PLASSER": null,
                "MAKS_ANT_SOKERE": null,
                "STATUS_FAST_ANT_PLASSER": null,
                "STATUS_SJEKK_ANT_DELTAKERE": null,
                "STATUS_KALKULATOR": "N",
                "RAMMEAVTALE": "SKAL",
                "OPPLAERINGSGRUPPE": "UTD",
                "HANDLINGSPLAN": "TIL",
                "STATUS_SLUTTDATO": "J",
                "MAKS_PERIODE": 24,
                "STATUS_MELDEPLIKT": null,
                "STATUS_VEDTAK": "J",
                "STATUS_IA_AVTALE": "N",
                "STATUS_TILLEGGSSTONADER": "J",
                "STATUS_UTDANNING": "N",
                "AUTOMATISK_TILSAGNSBREV": "N",
                "STATUS_BEGRUNNELSE_INNSOKT": "J",
                "STATUS_HENVISNING_BREV": "N",
                "STATUS_KOPIBREV": "N"
              }
            }
""".trimIndent()

// Denne er kun ment som en måte å produsere tilsvarende events inn på køene for å utvikle/test
fun main(args: Array<String>): Unit = runBlocking {
    val properties = KafkaPropertiesBuilder.producerBuilder()
        .withBrokerUrl("localhost:29092")
        .withBaseProperties()
        .withProducerId("mulighetsrommet-api-producer.v1")
        .withSerializers(StringSerializer::class.java, StringSerializer::class.java)
        .build()

    val producer = KafkaProducerClientBuilder.builder<String, String>()
        .withProperties(properties)
        .build()

    launch {
        produceTiltakEndretEvents(producer)
        producer.close()
    }
}

private suspend fun produceTiltakEndretEvents(producer: KafkaProducerClient<String, String>) {
    tiltakEndretTopic.forEach { it ->
        producer.send(ProducerRecord("teamarenanais.aapen-arena-tiltakendret-v1-q2", it.first, it.second))
        delay(5000)
    }
}
