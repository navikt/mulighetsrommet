package no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.tiltakshistorikk.createDatabaseTestConfig
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingV1ConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("konsumer gjennomføringer") {
        afterEach {
            database.db.truncateAll()
        }

        val gruppetiltak = GruppetiltakRepository(database.db)
        val consumer = TiltaksgjennomforingV1Consumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            gruppetiltak,
        )

        val tiltak = TiltaksgjennomforingV1Dto(
            id = UUID.randomUUID(),
            tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Gruppe AMO",
                arenaKode = "GRUPPEAMO",
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            ),
            navn = "Gruppe AMO",
            virksomhetsnummer = "123123123",
            startDato = LocalDate.now(),
            sluttDato = null,
            status = TiltaksgjennomforingStatus.GJENNOMFORES,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
        )

        test("upsert gruppetiltak from topic") {
            consumer.consume(tiltak.id, Json.encodeToJsonElement(tiltak))

            database.assertThat("gruppetiltak")
                .row()
                .value("id").isEqualTo(tiltak.id)
        }

        test("noop when tiltakskode is missing") {
            val msg = """
                {
                  "id": "44e3aa41-5901-467e-af7d-b59b2bcc202d",
                  "tiltakstype": {
                    "id": "9d36aa68-2099-4418-8d03-fb2f16288d22",
                    "navn": "Gruppe AMO",
                    "arenaKode": "GRUPPEAMO"
                  },
                  "navn": "Gruppe AMO",
                  "startDato": "2024-06-25",
                  "status": "GJENNOMFORES",
                  "virksomhetsnummer": "123123123",
                  "oppstart": "FELLES"
                }
            """.trimIndent()
            consumer.consume(tiltak.id, Json.parseToJsonElement(msg))

            database.assertThat("gruppetiltak").isEmpty
        }

        test("delete gruppetiltak for tombstone messages") {
            gruppetiltak.upsert(tiltak)

            consumer.consume(tiltak.id, JsonNull)

            database.assertThat("gruppetiltak").isEmpty
        }
    }
})
