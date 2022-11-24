package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import java.util.*

class TiltakstypeServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    beforeSpec {
        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val arenaService = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        val tiltakstype1 = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakskode = "ARBTREN"
        )

        val tiltakstype2 = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakskode = "INDOPPFOLG"
        )

        arenaService.createOrUpdate(tiltakstype1)
        arenaService.createOrUpdate(tiltakstype2)
    }

    context("CRUD") {
        val tiltakstypeService = TiltakstypeService(listener.db)

        test("should read tiltakstyper") {
            tiltakstypeService.getTiltakstyper().second shouldHaveSize 2
        }

        test("should filter tiltakstyper by innsatsgruppe") {
            tiltakstypeService.getTiltakstyper(innsatsgrupper = listOf(1)).second shouldHaveSize 1
            tiltakstypeService.getTiltakstyper(
                innsatsgrupper = listOf(1, 2)
            ).second shouldHaveSize 2
            tiltakstypeService.getTiltakstyper(innsatsgrupper = listOf(3)).second shouldHaveSize 0
        }

        test("should filter tiltakstyper by search") {
            tiltakstypeService.getTiltakstyper(search = "Førerhund").second shouldHaveSize 0
            tiltakstypeService.getTiltakstyper(search = "Arbeidstrening").second shouldHaveSize 1
        }
    }
    context("pagination") {
        listener.db.clean()
        listener.db.migrate()

        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val arenaService = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)
        val tiltakstypeService = TiltakstypeService(listener.db)

        (1..105).forEach {
            arenaService.createOrUpdate(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging$it",
                    tiltakskode = "ABC$it"
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, tiltakstyper) =
                tiltakstypeService.getTiltakstyper()

            tiltakstyper.size shouldBe DEFAULT_PAGINATION_LIMIT
            tiltakstyper.first().id shouldBe 1
            tiltakstyper.last().id shouldBe 50

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 61-80") {
            val (totalCount, tiltakstyper) =
                tiltakstypeService.getTiltakstyper(
                    paginationParams = PaginationParams(
                        4,
                        20
                    )
                )

            tiltakstyper.size shouldBe 20
            tiltakstyper.first().id shouldBe 61
            tiltakstyper.last().id shouldBe 80

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 101-105") {
            val (totalCount, tiltakstyper) =
                tiltakstypeService.getTiltakstyper(
                    paginationParams = PaginationParams(
                        3
                    )
                )
            tiltakstyper.size shouldBe 5
            tiltakstyper.first().id shouldBe 101
            tiltakstyper.last().id shouldBe 105

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val (totalCount, tiltakstyper) =
                tiltakstypeService.getTiltakstyper(
                    paginationParams = PaginationParams(
                        nullableLimit = 200
                    )
                )
            tiltakstyper.size shouldBe 105
            tiltakstyper.first().id shouldBe 1
            tiltakstyper.last().id shouldBe 105

            totalCount shouldBe 105
        }
    }
})
