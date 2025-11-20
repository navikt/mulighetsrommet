package no.nav.tiltak.historikk.kafka.consumers

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.db.queries.GjennomforingType
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.service.VirksomhetService

class SisteTiltaksgjennomforingerV2KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    beforeEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = SisteTiltaksgjennomforingerV2KafkaConsumer(db, mockk(relaxed = true))

        val gruppe: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingGruppe
        val enkeltplass: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingEnkeltplass

        beforeAny {
            db.session {
                queries.virksomhet.upsert(TestFixtures.virksomhet)
            }
        }

        test("upsert gjennomforing from topic") {
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))
            consumer.consume(enkeltplass.id, Json.encodeToJsonElement(enkeltplass))

            database.assertRequest("select * from gjennomforing order by created_at")
                .hasNumberOfRows(2)
                .row()
                .value("id").isEqualTo(gruppe.id)
                .value("gjennomforing_type").isEqualTo(GjennomforingType.GRUPPE.name)
                .value("tiltakskode").isEqualTo(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name)
                .value("arrangor_organisasjonsnummer").isEqualTo("987654321")
                .value("navn").isEqualTo("Gruppe AMO")
                .value("deltidsprosent").isEqualTo(80.0)
                .row()
                .value("id").isEqualTo(enkeltplass.id)
                .value("gjennomforing_type").isEqualTo(GjennomforingType.ENKELTPLASS.name)
                .value("tiltakskode").isEqualTo(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING.name)
                .value("arrangor_organisasjonsnummer").isEqualTo("987654321")
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

    context("synkroniserer virksomhet hvis den ikke finnes") {
        val db = TiltakshistorikkDatabase(database.db)

        val gruppe: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingGruppe

        test("lagrer virksomhet fra brreg") {
            var brreg = mockk<BrregClient>()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("987654321")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                organisasjonsform = "BEDR",
                navn = "Arrangør",
                postadresse = null,
                forretningsadresse = null,
                overordnetEnhet = null,
            ).right()
            var virksomheter = VirksomhetService(db, brreg)

            val consumer = SisteTiltaksgjennomforingerV2KafkaConsumer(db, virksomheter)
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))

            database.assertRequest("select * from gjennomforing")
                .hasNumberOfRows(1)
                .row()
                .value("arrangor_organisasjonsnummer").isEqualTo("987654321")

            virksomheter.getVirksomhet(Organisasjonsnummer("987654321")).shouldBe(TestFixtures.virksomhet)
        }

        test("lagrer utenlandsk virksomhet uten navn") {
            var brreg = mockk<BrregClient>()
            var virksomheter = VirksomhetService(db, brreg)

            var gjennomforing: TiltaksgjennomforingV2Dto = TestFixtures.gjennomforingGruppe.copy(
                arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("111222333")),
            )

            val consumer = SisteTiltaksgjennomforingerV2KafkaConsumer(db, virksomheter)
            consumer.consume(gjennomforing.id, Json.encodeToJsonElement(gjennomforing))

            database.assertRequest("select * from gjennomforing")
                .hasNumberOfRows(1)
                .row()
                .value("arrangor_organisasjonsnummer").isEqualTo("111222333")

            virksomheter.getVirksomhet(Organisasjonsnummer("111222333")) shouldBe VirksomhetDbo(
                organisasjonsnummer = Organisasjonsnummer("111222333"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = null,
                organisasjonsform = null,
                slettetDato = null,
            )
        }
    }
})
