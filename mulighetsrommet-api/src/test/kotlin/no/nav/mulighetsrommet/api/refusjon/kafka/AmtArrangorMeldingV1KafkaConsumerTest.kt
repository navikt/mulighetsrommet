package no.nav.mulighetsrommet.api.refusjon.kafka

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.model.DeltakerDto
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDateTime
import java.util.*

class AmtArrangorMeldingV1KafkaConsumerTest : FunSpec({
    val deltakerForslagRepository: DeltakerForslagRepository = mockk(relaxed = true)
    val deltakerRepository: DeltakerRepository = mockk(relaxed = true)

    val arrangorMeldingConsumer = AmtArrangorMeldingV1KafkaConsumer(
        config = KafkaTopicConsumer.Config(id = "deltaker", topic = "deltaker"),
        deltakerForslagRepository,
        deltakerRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    test("klarer og deserialisere arrangor-melding") {
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
        every { deltakerRepository.get(any()) } returns DeltakerDto(
            id = UUID.randomUUID(),
            gjennomforingId = UUID.randomUUID(),
            norskIdent = null,
            startDato = null,
            sluttDato = null,
            registrertTidspunkt = LocalDateTime.now(),
            endretTidspunkt = LocalDateTime.now(),
            deltakelsesprosent = null,
            status = DeltakerStatus(type = DeltakerStatus.Type.DELTAR, aarsak = null, opprettetDato = LocalDateTime.now()),
        )
        every { deltakerForslagRepository.upsert(any()) } returns Unit

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

        verify(exactly = 1) { deltakerForslagRepository.upsert(any()) }
    }
})
