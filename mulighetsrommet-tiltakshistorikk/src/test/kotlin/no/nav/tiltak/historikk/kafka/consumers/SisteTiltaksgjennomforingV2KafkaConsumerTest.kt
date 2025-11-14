package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.GjennomforingType
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase

class SisteTiltaksgjennomforingV2KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = SisteTiltaksgjennomforingerV2KafkaConsumer(db)

        val gruppe: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingGruppe
        val enkeltplass: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingEnkeltplass

        test("upsert gjennomforing from topic") {
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))
            consumer.consume(enkeltplass.id, Json.encodeToJsonElement(enkeltplass))

            database.assertRequest("select * from gjennomforing order by created_at")
                .hasNumberOfRows(2)
                .row()
                .value("id").isEqualTo(gruppe.id)
                .value("gjennomforing_type").isEqualTo(GjennomforingType.GRUPPE.name)
                .value("tiltakskode").isEqualTo(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name)
                .value("arrangor_organisasjonsnummer").isEqualTo("123123123")
                .value("navn").isEqualTo("Gruppe AMO")
                .value("deltidsprosent").isEqualTo(80.0)
                .row()
                .value("id").isEqualTo(enkeltplass.id)
                .value("gjennomforing_type").isEqualTo(GjennomforingType.ENKELTPLASS.name)
                .value("tiltakskode").isEqualTo(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING.name)
                .value("arrangor_organisasjonsnummer").isEqualTo("123123123")
                .value("navn").isNull
                .value("deltidsprosent").isNull

            var updatedGruppe: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingGruppe.copy(navn = "Nytt navn")
            consumer.consume(gruppe.id, Json.encodeToJsonElement(updatedGruppe))

            database.assertRequest("select * from gjennomforing order by updated_at desc")
                .hasNumberOfRows(2)
                .row()
                .value("id").isEqualTo(gruppe.id)
                .value("navn").isEqualTo("Nytt navn")
                .row()
                .value("id").isEqualTo(enkeltplass.id)
                .value("navn").isNull
        }

        test("delete gjennomforing for tombstone messages") {
            db.session {
                queries.gjennomforing.upsert(toGjennomforingDbo(gruppe))
                queries.gjennomforing.upsert(toGjennomforingDbo(enkeltplass))
            }

            consumer.consume(gruppe.id, JsonNull)
            database.assertRequest("select * from gjennomforing").hasNumberOfRows(1)

            consumer.consume(enkeltplass.id, JsonNull)
            database.assertRequest("select * from gjennomforing").hasNumberOfRows(0)
        }
    }
})
