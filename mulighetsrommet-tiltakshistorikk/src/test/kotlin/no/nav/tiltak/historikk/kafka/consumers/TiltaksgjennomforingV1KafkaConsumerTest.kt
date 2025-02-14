package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = SisteTiltaksgjennomforingerV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            db,
        )

        val tiltak = TiltaksgjennomforingEksternV1Dto(
            id = UUID.randomUUID(),
            tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Gruppe AMO",
                arenaKode = "GRUPPEAMO",
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            ),
            navn = "Gruppe AMO",
            virksomhetsnummer = "123123123",
            startDato = LocalDate.now(),
            sluttDato = null,
            status = GjennomforingStatus.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
            antallPlasser = 10,
        )

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
