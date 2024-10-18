package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class ArenaEntityServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val arenaId = "123456"
    val entityId = UUID.randomUUID()

    context("only processed events should be included by getMappingIfProcessed method") {
        val arenaEntityMappingRepository = ArenaEntityMappingRepository(database.db)

        val arenaEntityService = ArenaEntityService(
            mappings = arenaEntityMappingRepository,
            tiltakstyper = mockk(),
            saker = mockk(),
            tiltaksgjennomforinger = mockk(),
            avtaler = mockk(),
        )

        test("should return mapping if status is Handled") {
            arenaEntityMappingRepository.upsert(
                ArenaEntityMapping(
                    ArenaTable.Tiltaksgjennomforing,
                    arenaId,
                    entityId,
                    ArenaEntityMapping.Status.Handled,
                ),
            )

            val mapping = arenaEntityService.getMappingIfHandled(
                ArenaTable.Tiltaksgjennomforing,
                arenaId,
            )

            mapping?.entityId shouldBe entityId
        }

        test("should not return mapping if status is not Handled") {
            forAll(
                row(ArenaEntityMapping.Status.Unhandled),
                row(ArenaEntityMapping.Status.Ignored),
            ) { status ->
                arenaEntityMappingRepository.upsert(
                    ArenaEntityMapping(ArenaTable.Tiltaksgjennomforing, arenaId, entityId, status),
                )

                val mapping = arenaEntityService.getMappingIfHandled(
                    ArenaTable.Tiltaksgjennomforing,
                    arenaId,
                )

                mapping shouldBe null
            }
        }
    }
})
