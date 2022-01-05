package no.nav.mulighetsrommet.api.kafka

import kotlinx.coroutines.delay
import no.nav.mulighetsrommet.api.database.DatabaseFactory.dbQuery
import no.nav.mulighetsrommet.api.domain.TiltaksgjennomforingTable
import no.nav.mulighetsrommet.api.domain.TiltaksvariantTable
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class KafkaFactory {

    private val streamsConfiguration = KafkaStreamConfig()
//    private val kafkaStreams: KafkaStreams
//    private val topology: Topology
//    private val adminClient: AdminClient

    init {
//        topology = buildStream()
//        kafkaStreams = KafkaStreams(topology, streamsConfiguration)
//        adminClient = AdminClient.create(streamsConfiguration)
//        kafkaStreams.cleanUp()
//        kafkaStreams.start()
    }

    private fun buildStream(): Topology {
        val builder = StreamsBuilder()
        builder.stream<String, String>(KafkaTopics.Tiltaksgjennomforing.topic)
        return builder.build()
    }

//    fun shutdown() {
//        kafkaStreams.close()
//    }
//
//    fun isAlive(): Boolean {
//        return kafkaStreams.state().isRunningOrRebalancing
//    }

    private fun createConsumer(): Consumer<String, String> {
        val props = streamsConfiguration
        props["key.deserializer"] = StringDeserializer::class.java
        props["value.deserializer"] = StringDeserializer::class.java
        return KafkaConsumer(props)
    }

    fun consumeArenaEvents() {
        val consumer = createConsumer()
        consumer.subscribe(listOf(KafkaTopics.Tiltaksgjennomforing.topic))
        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            if (!records.isEmpty) {
                println("Consumed ${records.count()} records")
                records.iterator().forEach {
                    val message = it.value()
                    println("Message: $message")
                }
            }
        }
    }

    // Denne er kun for å ha en måte å simulere at events kommer inn fra Arena via Kafka.
    // TODO: Fjern denne når bestilling av Arena er på plass.
    suspend fun consumeTiltaksgjennomforingEventsFromArena() {
        delay(Duration.ofMinutes(2).toMillis())
        while (true) {
            val uuid = UUID.randomUUID()
            val tiltaksnr = Random.nextInt(0, 999999)

            val arenaEvent = ArenaEvent(
                "Tiltaksgjennomføring ($uuid)",
                "Beskrivelse",
                tiltaksnr,
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(2)
            )

            val tiltaksgjennomforingId = dbQuery {
                TiltaksgjennomforingTable.insertAndGetId {
                    it[tittel] = arenaEvent.tittel
                    it[tiltaksvariantId] = getRandomId(TiltaksvariantTable)
                    it[tiltaksnummer] = arenaEvent.tiltaksnummer
                    it[beskrivelse] = arenaEvent.beskrivelse
                    it[fraDato] = arenaEvent.fraDato
                    it[tilDato] = arenaEvent.tilDato
                }
            }
            println("Opprettet tiltaksgjennomforing med id $tiltaksgjennomforingId")
            delay(Duration.ofHours(2).toMillis())
        }
    }

    data class ArenaEvent(
        val tittel: String,
        val beskrivelse: String,
        val tiltaksnummer: Int, // Ikke unikt, kan kolidere
        val fraDato: LocalDateTime,
        val tilDato: LocalDateTime
    )
}

/**
 * Only for testing
 */
fun <T : IntIdTable> getRandomId(table: T): Int {
    return table
        .slice(table.id)
        .selectAll()
        .map { it[table.id].value }
        .random()
}
