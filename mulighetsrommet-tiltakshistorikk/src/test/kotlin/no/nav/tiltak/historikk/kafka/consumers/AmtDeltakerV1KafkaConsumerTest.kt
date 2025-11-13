package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.time.LocalDateTime

class AmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("consume deltakere") {
        val db = TiltakshistorikkDatabase(database.db)

        val deltakerConsumer = AmtDeltakerV1KafkaConsumer(db)

        val tiltak = TestFixtures.tiltak
        val amtDeltaker1 = TestFixtures.amtDeltaker

        beforeEach {
            db.session {
                queries.gruppetiltak.upsert(tiltak)
            }
        }

        afterEach {
            database.truncateAll()
        }

        test("upsert deltakere from topic") {
            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            database.assertTable("komet_deltaker")
                .row()
                .value("id").isEqualTo(amtDeltaker1.id)
        }

        test("delete deltakere for tombstone messages") {
            db.session {
                queries.deltaker.upsertKometDeltaker(amtDeltaker1)
            }

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            database.assertTable("komet_deltaker").isEmpty
        }

        test("delete deltakere that have status FEILREGISTRERT") {
            db.session {
                queries.deltaker.upsertKometDeltaker(amtDeltaker1)
            }

            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            database.assertTable("komet_deltaker").isEmpty
        }
    }
})
