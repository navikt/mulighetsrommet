package no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.tiltakshistorikk.databaseConfig
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("konsumer gjennomføringer") {
        afterEach {
            database.db.truncateAll()
        }

        val gruppetiltak = GruppetiltakRepository(database.db)
        val consumer = SisteTiltaksgjennomforingerV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            gruppetiltak,
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
            status = TiltaksgjennomforingStatus.GJENNOMFORES,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
        )

        test("upsert gruppetiltak from topic") {
            consumer.consume(tiltak.id, Json.encodeToJsonElement(tiltak))

            database.assertThat("gruppetiltak")
                .row()
                .value("id").isEqualTo(tiltak.id)
        }

        test("delete gruppetiltak for tombstone messages") {
            gruppetiltak.upsert(tiltak)

            consumer.consume(tiltak.id, JsonNull)

            database.assertThat("gruppetiltak").isEmpty
        }
    }
})
