package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase

class SisteTiltaksgjennomforingV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = SisteTiltaksgjennomforingerV1KafkaConsumer(db)

        val tiltak = TestFixtures.tiltak

        test("upsert gruppetiltak from topic") {
            consumer.consume(tiltak.id, Json.encodeToJsonElement(tiltak))

            database.assertTable("gruppetiltak")
                .row()
                .value("id").isEqualTo(tiltak.id)
        }

        test("delete gruppetiltak for tombstone messages") {
            db.session { queries.gruppetiltak.upsert(tiltak) }

            consumer.consume(tiltak.id, JsonNull)

            database.assertTable("gruppetiltak").isEmpty
        }
    }
})
