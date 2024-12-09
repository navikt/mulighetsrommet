package no.nav.mulighetsrommet.api.refusjon.kafka

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.utils.toUUID

class AmtArrangorMeldingV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    test("klarer og deserialisere arrangor-melding") {
        val arrangorMeldingConsumer = AmtArrangorMeldingV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
            deltakerForslagRepository = DeltakerForslagRepository(database.db),
            deltakerRepository = DeltakerRepository(database.db),
        )

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
    }
})
