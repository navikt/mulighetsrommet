package no.nav.tiltak.historikk.kafka.consumers

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Gjennomforing
import no.nav.tiltak.historikk.model.Virksomhet
import no.nav.tiltak.historikk.service.VirksomhetService

class ReplikerSisteTiltaksgjennomforingerV2KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    beforeEach {
        TiltakshistorikkDatabase(database.db).session {
            queries.tiltakstype.upsert(TestFixtures.Tiltakstype.gruppeAmo)
            queries.tiltakstype.upsert(TestFixtures.Tiltakstype.enkelAmo)
        }
    }

    afterEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(db, mockk(relaxed = true))

        val gruppe: TiltaksgjennomforingV2Dto = TestFixtures.Gjennomforing.gruppeAmo
        val enkeltplass: TiltaksgjennomforingV2Dto = TestFixtures.Gjennomforing.enkelAmo

        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.arrangor)
            }
        }

        test("upsert gjennomforing from topic") {
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))
            consumer.consume(enkeltplass.id, Json.encodeToJsonElement(enkeltplass))

            db.session {
                queries.gjennomforing.get(gruppe.id) shouldBe Gjennomforing(
                    id = gruppe.id,
                    type = Gjennomforing.Type.GRUPPE,
                    tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                    arrangorOrganisasjonsnummer = "987654321",
                    navn = "Gruppe AMO",
                    deltidsprosent = 80.0,
                )
                queries.gjennomforing.get(enkeltplass.id) shouldBe Gjennomforing(
                    id = enkeltplass.id,
                    type = Gjennomforing.Type.ENKELTPLASS,
                    tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                    arrangorOrganisasjonsnummer = "987654321",
                    navn = null,
                    deltidsprosent = null,
                )
            }

            var updatedGruppe: TiltaksgjennomforingV2Dto = TestFixtures.Gjennomforing.gruppeAmo.copy(navn = "Nytt navn")
            consumer.consume(gruppe.id, Json.encodeToJsonElement(updatedGruppe))

            db.session {
                queries.gjennomforing.get(gruppe.id)?.navn shouldBe "Nytt navn"
            }
        }

        test("delete gjennomforing for tombstone messages") {
            db.session {
                queries.gjennomforing.upsert(gruppe.toGjennomforing())
                queries.gjennomforing.upsert(enkeltplass.toGjennomforing())
            }

            consumer.consume(gruppe.id, JsonNull)

            db.session {
                queries.gjennomforing.get(gruppe.id).shouldBeNull()
                queries.gjennomforing.get(enkeltplass.id).shouldNotBeNull()
            }

            consumer.consume(enkeltplass.id, JsonNull)

            db.session {
                queries.gjennomforing.get(gruppe.id).shouldBeNull()
                queries.gjennomforing.get(enkeltplass.id).shouldBeNull()
            }
        }
    }

    context("synkroniserer virksomhet hvis den ikke finnes") {
        val db = TiltakshistorikkDatabase(database.db)

        val gruppe: TiltaksgjennomforingV2Dto = TestFixtures.Gjennomforing.gruppeAmo

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

            val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(db, virksomheter)
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))

            db.session {
                queries.gjennomforing.get(gruppe.id)?.arrangorOrganisasjonsnummer shouldBe "987654321"
            }

            virksomheter.getVirksomhet(Organisasjonsnummer("987654321")).shouldBe(TestFixtures.Virksomhet.arrangor)
        }

        test("lagrer utenlandsk virksomhet uten navn") {
            var brreg = mockk<BrregClient>()
            var virksomheter = VirksomhetService(db, brreg)

            var gjennomforing: TiltaksgjennomforingV2Dto = TestFixtures.Gjennomforing.gruppeAmo.copy(
                arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("111222333")),
            )

            val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(db, virksomheter)
            consumer.consume(gjennomforing.id, Json.encodeToJsonElement(gjennomforing))

            db.session {
                queries.gjennomforing.get(gjennomforing.id)?.arrangorOrganisasjonsnummer shouldBe "111222333"
            }

            virksomheter.getVirksomhet(Organisasjonsnummer("111222333")) shouldBe Virksomhet(
                organisasjonsnummer = Organisasjonsnummer("111222333"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = null,
                organisasjonsform = null,
                slettetDato = null,
            )
        }
    }
})
