package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import java.util.*

class ArenaEntityServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database =
        extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    val tiltaksnummer = "123456"
    val uuid = UUID.randomUUID()

    val event = ArenaEvent(
        status = ArenaEvent.ProcessingStatus.Processed,
        arenaTable = ArenaTable.Tiltaksgjennomforing,
        arenaId = tiltaksnummer,
        payload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    )

    context("only processed events should be included by getMappingIfProcessed method") {
        val arenaEntityMappingRepository =
            ArenaEntityMappingRepository(database.db)
        val arenaEventRepository = ArenaEventRepository(database.db)

        val arenaEntityService = ArenaEntityService(
            mappings = arenaEntityMappingRepository,
            events = arenaEventRepository,
            tiltakstyper = mockk(),
            saker = mockk(),
            tiltaksgjennomforinger = mockk(),
            deltakere = mockk(),
            avtaler = mockk(),
        )

        arenaEntityMappingRepository.insert(
            ArenaEntityMapping(
                ArenaTable.Tiltaksgjennomforing,
                tiltaksnummer,
                uuid
            )
        )

        test("event should be fetched if status is processed") {
            arenaEventRepository.upsert(event)

            arenaEntityService.getMappingIfProcessed(
                ArenaTable.Tiltaksgjennomforing,
                tiltaksnummer
            )?.entityId shouldBe uuid
        }

        test("event should not be fetched if status is pending") {
            arenaEventRepository.upsert(event.copy(status = ArenaEvent.ProcessingStatus.Pending))

            arenaEntityService.getMappingIfProcessed(
                ArenaTable.Tiltaksgjennomforing,
                tiltaksnummer
            )?.entityId shouldBe null
        }
    }
})
