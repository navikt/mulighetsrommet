package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import java.util.*

class TiltakstypeRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(database)

    test("CRUD") {
        val tiltakstyper = TiltakstypeRepository(database.db)

        tiltakstyper.save(
            Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Arbeidstrening",
                tiltakskode = "ARBTREN"
            )
        )
        tiltakstyper.save(
            Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                tiltakskode = "INDOPPFOLG"
            )
        )

        tiltakstyper.getTiltakstyper().second shouldHaveSize 2
        tiltakstyper.getTiltakstyper(search = "Førerhund").second shouldHaveSize 0
        tiltakstyper.getTiltakstyper(search = "Arbeidstrening").second shouldHaveSize 1
    }

    context("pagination") {
        database.db.clean()
        database.db.migrate()

        val tiltakstyper = TiltakstypeRepository(database.db)

        (1..105).forEach {
            tiltakstyper.save(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "$it",
                    tiltakskode = "$it"
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, items) = tiltakstyper.getTiltakstyper()

            items.size shouldBe DEFAULT_PAGINATION_LIMIT
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "50"

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 61-80") {
            val (totalCount, items) = tiltakstyper.getTiltakstyper(
                paginationParams = PaginationParams(
                    4,
                    20
                )
            )

            items.size shouldBe 20
            items.first().navn shouldBe "61"
            items.last().navn shouldBe "80"

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 101-105") {
            val (totalCount, items) = tiltakstyper.getTiltakstyper(
                paginationParams = PaginationParams(
                    3
                )
            )

            items.size shouldBe 5
            items.first().navn shouldBe "101"
            items.last().navn shouldBe "105"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val (totalCount, items) = tiltakstyper.getTiltakstyper(
                paginationParams = PaginationParams(
                    nullableLimit = 200
                )
            )

            items.size shouldBe 105
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "105"

            totalCount shouldBe 105
        }
    }
})
