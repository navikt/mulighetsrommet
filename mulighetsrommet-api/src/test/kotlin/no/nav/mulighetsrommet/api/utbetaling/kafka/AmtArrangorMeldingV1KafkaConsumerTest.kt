package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.amt.model.EndringAarsak
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.utils.toUUID
import java.util.*

class AmtArrangorMeldingV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createArrangorMeldingConsumer() = AmtArrangorMeldingV1KafkaConsumer(
        config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker", Properties()),
        db = database.db,
    )

    test("klarer og deserialisere arrangor-melding") {
        val arrangorMeldingConsumer = createArrangorMeldingConsumer()

        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """{
                "type":"Forslag",
                "id":"26b2ef7f-2c33-4468-b9cd-98e935d747cc",
                "deltakerId":"d70c80d8-98aa-4591-a060-b3e70dcbe6b6",
                "opprettetAvArrangorAnsattId":"fff9a665-cbde-4dbc-9ef9-deb8681a0d6f",
                "opprettet":"2024-10-07T13:52:49.178623",
                "begrunnelse":null,
                "endring":{
                  "type":"AvsluttDeltakelse",
                  "sluttdato":null,
                  "aarsak":{
                    "type":"TrengerAnnenStotte"
                  },
                  "harDeltatt":false
                },
                "status":{
                  "type":"Godkjent",
                  "godkjentAv":{
                    "id":"63074159-4c8f-463f-93d7-6569c156b8c6",
                    "enhetId":"0a65ec18-626d-4ee9-aa69-c0ce43ec8ef5"
                  },
                  "godkjent":"2024-10-07T13:52:57.250147881"
                },
                "navAnsatt":{
                  "id":"63074159-4c8f-463f-93d7-6569c156b8c6",
                  "enhetId":"0a65ec18-626d-4ee9-aa69-c0ce43ec8ef5"
                },
                "sistEndret":"2024-10-07T13:52:57.250147881"
              }
                """.trimIndent(),
            ),
        )

        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """
                    {"type":"Forslag","id":"f3163724-a636-47aa-83b8-215fab00334f","deltakerId":"3161e5a3-7f9c-443d-b15a-f5797bb318fa","opprettetAvArrangorAnsattId":"fff9a665-cbde-4dbc-9ef9-deb8681a0d6f","opprettet":"2024-11-11T14:55:33.368107921","begrunnelse":"sfsfwfwe","endring":{"type":"AvsluttDeltakelse","sluttdato":"2024-11-06","aarsak":{"type":"IkkeMott"},"harDeltatt":null},"status":{"type":"VenterPaSvar"},"sistEndret":"2024-11-11T14:55:33.368107921","navAnsatt":null}
                """.trimIndent(),
            ),
        )

        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """
                    {"type":"Forslag","id":"fc46cbeb-4932-43f8-8f4c-1f7cc3a92e3e","deltakerId":"459e9f88-5987-4c6d-8332-aecdbbd74285","opprettetAvArrangorAnsattId":"fff9a665-cbde-4dbc-9ef9-deb8681a0d6f","opprettet":"2024-10-25T07:56:17.712116898","begrunnelse":"efwef","endring":{"type":"Deltakelsesmengde","deltakelsesprosent":80,"dagerPerUke":3},"status":{"type":"VenterPaSvar"},"sistEndret":"2024-10-25T07:56:17.712116898","navAnsatt":null}
                """.trimIndent(),
            ),
        )

        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """
                    {"type":"Forslag","id":"0a9a68d1-fb78-4f97-89ac-d23cb650c382","deltakerId":"9ed1343b-57db-4858-a298-22aabe6f6353","opprettetAvArrangorAnsattId":"fff9a665-cbde-4dbc-9ef9-deb8681a0d6f","opprettet":"2024-12-17T13:46:59.340457849","begrunnelse":"test av forslag","endring":{"type":"FjernOppstartsdato"},"status":{"type":"VenterPaSvar"},"sistEndret":"2024-12-17T13:46:59.340457849","navAnsatt":null}
                """.trimIndent(),
            ),
        )

        // Håndterer andre typer meldinger som ikke er relevante for oss
        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """
                    {"type":"EndringFraArrangor","id":"0a9a68d1-fb78"}
                """.trimIndent(),
            ),
        )
    }

    test("venter på svar genererer upsert") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(GjennomforingFixtures.AFT1),
            deltakere = listOf(DeltakerFixtures.createDeltaker(GjennomforingFixtures.AFT1.id)),
        ).initialize(database.db)

        val deltakerId = domain.deltakere[0].id

        val arrangorMeldingConsumer = createArrangorMeldingConsumer()

        arrangorMeldingConsumer.consume(
            "26b2ef7f-2c33-4468-b9cd-98e935d747cc".toUUID(),
            Json.parseToJsonElement(
                """{
                "type":"Forslag",
                "id":"26b2ef7f-2c33-4468-b9cd-98e935d747cc",
                "deltakerId":"$deltakerId",
                "opprettetAvArrangorAnsattId":"fff9a665-cbde-4dbc-9ef9-deb8681a0d6f",
                "opprettet":"2024-10-07T13:52:49.178623",
                "begrunnelse":null,
                "endring":{
                  "type":"AvsluttDeltakelse",
                  "sluttdato":null,
                  "aarsak":{
                    "type":"TrengerAnnenStotte"
                  },
                  "harDeltatt":false
                },
                "status":{
                  "type":"VenterPaSvar"
                },
                "navAnsatt":{
                  "id":"63074159-4c8f-463f-93d7-6569c156b8c6",
                  "enhetId":"0a65ec18-626d-4ee9-aa69-c0ce43ec8ef5"
                },
                "sistEndret":"2024-10-07T13:52:57.250147881"
              }
                """.trimIndent(),
            ),
        )

        val forslag = database.run {
            queries.deltakerForslag.getForslagByGjennomforing(GjennomforingFixtures.AFT1.id)
        }

        forslag shouldBe mapOf(
            deltakerId to listOf(
                DeltakerForslag(
                    id = UUID.fromString("26b2ef7f-2c33-4468-b9cd-98e935d747cc"),
                    deltakerId = deltakerId,
                    endring = Melding.Forslag.Endring.AvsluttDeltakelse(
                        aarsak = EndringAarsak.TrengerAnnenStotte,
                        harDeltatt = false,
                    ),
                    status = DeltakerForslag.Status.VENTER_PA_SVAR,
                ),
            ),
        )
    }
})
