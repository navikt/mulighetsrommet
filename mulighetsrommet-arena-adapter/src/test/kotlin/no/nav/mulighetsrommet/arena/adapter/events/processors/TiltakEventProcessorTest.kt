package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.time.LocalDateTime

class TiltakEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            avtaler = AvtaleRepository(database.db),
        )

        fun createProcessor(): TiltakEventProcessor {
            return TiltakEventProcessor(entities)
        }

        fun prepareEvent(event: ArenaEvent): Pair<ArenaEvent, ArenaEntityMapping> {
            val mapping = entities.getOrCreateMapping(event)
            return Pair(event, mapping)
        }

        test("should treat all operations as upserts") {
            val processor = createProcessor()

            val (e1, mapping) = prepareEvent(createArenaTiltakEvent(Insert) { it.copy(TILTAKSNAVN = "Oppfølging 1") })
            processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
            database.assertThat("tiltakstype").row()
                .value("id").isEqualTo(mapping.entityId)
                .value("navn").isEqualTo("Oppfølging 1")

            val e2 = createArenaTiltakEvent(Update) { it.copy(TILTAKSNAVN = "Oppfølging 2") }
            processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
            database.assertThat("tiltakstype").row()
                .value("id").isEqualTo(mapping.entityId)
                .value("navn").isEqualTo("Oppfølging 2")

            val e3 = createArenaTiltakEvent(Delete) { it.copy(TILTAKSNAVN = "Oppfølging 1") }
            processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
            database.assertThat("tiltakstype").row()
                .value("id").isEqualTo(mapping.entityId)
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
    }
})
