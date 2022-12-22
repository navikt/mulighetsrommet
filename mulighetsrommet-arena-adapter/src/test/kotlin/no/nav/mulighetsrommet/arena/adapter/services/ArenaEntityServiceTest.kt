package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import java.util.*

class ArenaEntityServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val arenaEntityMappingRepository: ArenaEntityMappingRepository = mockk()
    val arenaEventRepository: ArenaEventRepository = mockk()

    val arenaEntityService = ArenaEntityService(
        events = arenaEventRepository,
        mappings = arenaEntityMappingRepository,
        tiltakstyper = mockk(),
        saker = mockk(),
        tiltaksgjennomforinger = mockk(),
        deltakere = mockk()
    )

    context("only processed events should be included by getMappingIfProcessed method") {
        val uuid = UUID.randomUUID()
        val tiltaksnummer = "123456"

        val arenaEvent = ArenaEvent(
            arenaTable = ArenaTables.Tiltaksgjennomforing,
            arenaId = tiltaksnummer,
            payload = mockk(),
            status = ArenaEvent.ConsumptionStatus.Processed
        )

        every {
            arenaEntityMappingRepository.get(
                ArenaTables.Tiltaksgjennomforing,
                tiltaksnummer
            )
        } returns ArenaEntityMapping(
            arenaTable = ArenaTables.Tiltaksgjennomforing,
            arenaId = tiltaksnummer,
            entityId = uuid
        )

        test("event should be fetched if status is processed") {
            every {
                arenaEventRepository.get(
                    ArenaTables.Tiltaksgjennomforing,
                    tiltaksnummer
                )
            } returns arenaEvent

            arenaEntityService.getMappingIfProcessed(
                ArenaTables.Tiltaksgjennomforing,
                tiltaksnummer
            )?.entityId shouldBe uuid
        }

        test("event should not be fetched if status is pending") {
            every {
                arenaEventRepository.get(
                    ArenaTables.Tiltaksgjennomforing,
                    tiltaksnummer
                )
            } returns arenaEvent.copy(status = ArenaEvent.ConsumptionStatus.Pending)

            arenaEntityService.getMappingIfProcessed(
                ArenaTables.Tiltaksgjennomforing,
                tiltaksnummer
            )?.entityId shouldBe null
        }
    }
})
