package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakgjennomforingEvent
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDateTime
import java.util.*

class TiltakdeltakerEventProcessorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(
        FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema())
    )

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    val regDatoBeforeAktivitetsplanen = AktivitetsplanenLaunchDate
        .minusDays(1)
        .format(ArenaUtils.TimestampFormatter)

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when the dependent tiltaksgjennomføring is missing") {
            val consumer =
                createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createArenaTiltakdeltakerEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("deltaker").isEmpty
        }
    }

    context("when dependent events has been processed") {
        val tiltakstypeGruppe = TiltakstypeFixtures.Gruppe
        val tiltakstypeIndividuell = TiltakstypeFixtures.Individuell
        val sak = Sak(
            sakId = 1,
            lopenummer = 123,
            aar = 2022,
            enhet = "2990"
        )

        val tiltaksgjennomforing = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = 3,
            sakId = 1,
            tiltakskode = "INDOPPFAG",
            arrangorId = null,
            navn = null,
            status = "GJENNOMFOR",
            fraDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        )
        val sakIndividuell = Sak(
            sakId = 2,
            lopenummer = 122,
            aar = 2022,
            enhet = "2990"
        )
        val tiltaksgjennomforingIndividuell = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = 4,
            sakId = 2,
            tiltakskode = "AMO",
            arrangorId = 123,
            navn = null,
            status = "GJENNOMFOR",
            fraDato = LocalDateTime.of(2023, 1, 1, 0, 0)
        )

        beforeEach {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstypeGruppe).getOrThrow()
            tiltakstyper.upsert(tiltakstypeIndividuell).getOrThrow()

            val saker = SakRepository(database.db)
            saker.upsert(sak).getOrThrow()
            saker.upsert(sakIndividuell).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(tiltaksgjennomforing).getOrThrow()
            tiltaksgjennomforinger.upsert(tiltaksgjennomforingIndividuell).getOrThrow()

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTable.Tiltaksgjennomforing,
                    tiltaksgjennomforing.tiltaksgjennomforingId.toString(),
                    tiltaksgjennomforing.id
                )
            )
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTable.Tiltaksgjennomforing,
                    tiltaksgjennomforingIndividuell.tiltaksgjennomforingId.toString(),
                    tiltaksgjennomforingIndividuell.id
                )
            )
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTable.Tiltakstype,
                    tiltakstypeGruppe.tiltakskode,
                    tiltakstypeGruppe.id
                )
            )
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTable.Tiltakstype,
                    tiltakstypeIndividuell.tiltakskode,
                    tiltakstypeIndividuell.id
                )
            )

            val events = ArenaEventRepository(database.db)
            events.upsert(
                createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(TILTAKGJENNOMFORING_ID = tiltaksgjennomforing.tiltaksgjennomforingId)
                }
            )

            events.upsert(
                createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(TILTAKGJENNOMFORING_ID = tiltaksgjennomforingIndividuell.tiltaksgjennomforingId)
                }
            )
        }

        test("should be ignored when REG_DATO is before aktivitetsplanen") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = createArenaTiltakdeltakerEvent(Insert) { it.copy(REG_DATO = regDatoBeforeAktivitetsplanen) }

            consumer.processEvent(event).status shouldBe Ignored
        }

        test("should be ignored when dependent tiltaksgjennomforing is ignored") {
            val events = ArenaEventRepository(database.db)
            events.upsert(
                createArenaTiltakgjennomforingEvent(Insert, status = Ignored) {
                    it.copy(TILTAKGJENNOMFORING_ID = tiltaksgjennomforing.tiltaksgjennomforingId)
                }
            )
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = createArenaTiltakdeltakerEvent(Insert) { it.copy(DELTAKERSTATUSKODE = "FULLF") }

            consumer.processEvent(event).status shouldBe Ignored
        }

        test("should treat all operations as upserts") {
            val engine = createMockEngine(
                "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                "/api/v1/internal/arena/tiltakshistorikk.*" to { respondOk() }
            )
            val consumer = createConsumer(database.db, engine)

            val e1 = createArenaTiltakdeltakerEvent(Insert) { it.copy(DELTAKERSTATUSKODE = "GJENN") }
            consumer.processEvent(e1).status shouldBe Processed
            database.assertThat("deltaker").row().value("status").isEqualTo("DELTAR")

            val e2 = createArenaTiltakdeltakerEvent(Update) { it.copy(DELTAKERSTATUSKODE = "FULLF") }
            consumer.processEvent(e2).status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("AVSLUTTET")

            val e3 = createArenaTiltakdeltakerEvent(Delete) { it.copy(DELTAKERSTATUSKODE = "FULLF") }
            consumer.processEvent(e3).status shouldBe Processed
            database.assertThat("deltaker").row().value("status").isEqualTo("AVSLUTTET")
        }

        context("api responses") {
            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.InternalServerError) },
                    "/api/v1/internal/arena/deltaker" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createArenaTiltakdeltakerEvent(Insert))

                event.status shouldBe Failed
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.NotFound) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createArenaTiltakdeltakerEvent(Insert))

                event.status shouldBe Invalid
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to {
                        respondError(
                            HttpStatusCode.InternalServerError
                        )
                    }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createArenaTiltakdeltakerEvent(Insert))

                event.status shouldBe Failed
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk.*" to { respondOk() },
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor(
                                "123456",
                                "000000"
                            )
                        )
                    }
                )

                val consumer = createConsumer(database.db, engine)

                consumer.processEvent(createArenaTiltakdeltakerEvent(Insert))

                val generatedId = engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val deltaker =
                        decodeRequestBody<TiltakshistorikkDbo>().apply {
                            this.shouldBeInstanceOf<TiltakshistorikkDbo.Gruppetiltak>()
                            tiltaksgjennomforingId shouldBe tiltaksgjennomforing.id
                            norskIdent shouldBe "12345678910"
                        }

                    deltaker.id
                }

                consumer.processEvent(createArenaTiltakdeltakerEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe generatedId
                }

                val event = createArenaTiltakdeltakerEvent(Insert) {
                    it.copy(
                        TILTAKDELTAKER_ID = 2,
                        TILTAKGJENNOMFORING_ID = tiltaksgjennomforingIndividuell.tiltaksgjennomforingId
                    )
                }

                consumer.processEvent(event)

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val deltaker =
                        decodeRequestBody<TiltakshistorikkDbo>().apply {
                            this.shouldBeInstanceOf<TiltakshistorikkDbo.IndividueltTiltak>()
                            beskrivelse shouldBe tiltaksgjennomforing.navn
                            virksomhetsnummer shouldBe "123456"
                            tiltakstypeId shouldBe tiltakstypeIndividuell.id
                            norskIdent shouldBe "12345678910"
                        }

                    deltaker.id
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakdeltakerEventProcessor {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
        "Bearer token"
    }

    val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
        "Bearer token"
    }

    val entities = ArenaEntityService(
        events = ArenaEventRepository(db),
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db),
        avtaler = AvtaleRepository(db),
    )

    return TiltakdeltakerEventProcessor(
        ConsumerConfig("deltaker", "deltaker"),
        ArenaEventRepository(db),
        entities,
        client,
        ords
    )
}
