package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakEvent
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    test("should treat all operations as upserts") {
        val consumer = createConsumer(database.db, MockEngine { respondOk() })
        val entities = ArenaEntityMappingRepository(database.db)

        val e1 = createArenaTiltakEvent(Insert) { it.copy(TILTAKSNAVN = "Oppfølging 1") }
        entities.upsert(ArenaEntityMapping(e1.arenaTable, e1.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))
        consumer.handleEvent(e1) shouldBeRight ProcessingResult(Handled)
        database.assertThat("tiltakstype").row().value("navn").isEqualTo("Oppfølging 1")

        val e2 = createArenaTiltakEvent(Update) { it.copy(TILTAKSNAVN = "Oppfølging 2") }
        consumer.handleEvent(e2) shouldBeRight ProcessingResult(Handled)
        database.assertThat("tiltakstype").row().value("navn").isEqualTo("Oppfølging 2")

        val e3 = createArenaTiltakEvent(Delete) { it.copy(TILTAKSNAVN = "Oppfølging 1") }
        consumer.handleEvent(e3) shouldBeRight ProcessingResult(Handled)
        database.assertThat("tiltakstype").row()
            .value("navn").isEqualTo("Oppfølging 1")
            .value("rett_paa_tiltakspenger").isTrue
            .value("registrert_dato_i_arena").isEqualTo(LocalDateTime.of(2010, 1, 11, 0, 0, 0, 0))
            .value("sist_endret_dato_i_arena").isEqualTo(LocalDateTime.of(2022, 1, 11, 0, 0, 0))
            .value("fra_dato").isEqualTo(LocalDate.of(2022, 1, 11))
            .value("fra_dato").isEqualTo(LocalDate.of(2022, 1, 11))
            .value("til_dato").isEqualTo(LocalDate.of(2022, 1, 15))
            .value("tiltaksgruppekode").isEqualTo("UTFAS")
            .value("administrasjonskode").isEqualTo("IND")
            .value("send_tilsagnsbrev_til_deltaker").isTrue
            .value("skal_ha_anskaffelsesprosess").isFalse
            .value("maks_antall_plasser").isNull
            .value("maks_antall_sokere").isEqualTo(10)
            .value("har_fast_antall_plasser").isNull
            .value("skal_sjekke_antall_deltakere").isTrue
            .value("vis_lonnstilskuddskalkulator").isFalse
            .value("rammeavtale").isEqualTo("IKKE")
            .value("opplaeringsgruppe").isNull
            .value("handlingsplan").isEqualTo("TIL")
            .value("tiltaksgjennomforing_krever_sluttdato").isFalse
            .value("maks_periode_i_mnd").isEqualTo(6)
            .value("tiltaksgjennomforing_krever_meldeplikt").isNull
            .value("tiltaksgjennomforing_krever_vedtak").isFalse
            .value("tiltaksgjennomforing_reservert_for_ia_bedrift").isFalse
            .value("har_rett_paa_tilleggsstonader").isFalse
            .value("har_rett_paa_utdanning").isFalse
            .value("tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk").isFalse
            .value("vis_begrunnelse_for_innsoking").isTrue
            .value("henvisningsbrev_og_hovedbrev_til_arbeidsgiver").isFalse
            .value("kopibrev_og_hovedbrev_til_arbeidsgiver").isFalse
    }

    context("api responses") {
        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)
            val entities = ArenaEntityMappingRepository(database.db)

            val e1 = createArenaTiltakEvent(Insert)
            entities.upsert(ArenaEntityMapping(e1.arenaTable, e1.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))

            consumer.handleEvent(e1).shouldBeRight()

            val generatedId = engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                val tiltakstype = decodeRequestBody<TiltakstypeDbo>().apply {
                    navn shouldBe "Oppfølging"
                    rettPaaTiltakspenger shouldBe true
                }

                tiltakstype.id
            }

            consumer.handleEvent(createArenaTiltakEvent(Delete)).shouldBeRight()

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Delete

                url.getLastPathParameterAsUUID() shouldBe generatedId
            }
        }

        test("should treat a 500 response as error") {
            val consumer = createConsumer(database.db, MockEngine { respondError(HttpStatusCode.InternalServerError) })
            val event = createArenaTiltakEvent(Insert)

            consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Failed }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakEventProcessor {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
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

    return TiltakEventProcessor(entities, client)
}
