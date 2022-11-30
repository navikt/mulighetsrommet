package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing.Tilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

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

    val tiltak1 = Tiltaksgjennomforing(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakstypeId = tiltakstype1.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
        antallPlasser = null
    )

    val tiltak2 = Tiltaksgjennomforing(
        id = UUID.randomUUID(),
        navn = "Trening",
        tiltakstypeId = tiltakstype2.id,
        tiltaksnummer = "54321",
        virksomhetsnummer = "123456789",
        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
        antallPlasser = null
    )

    context("CRUD") {
        beforeAny {
            val tiltakstyper = TiltakstypeRepository(listener.db)
            tiltakstyper.save(tiltakstype1)
            tiltakstyper.save(tiltakstype2)
        }

        test("CRUD") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(listener.db)

            tiltaksgjennomforinger.upsert(tiltak1).shouldBeRight()
            tiltaksgjennomforinger.upsert(tiltak2).shouldBeRight()

            tiltaksgjennomforinger.getAll().second shouldHaveSize 2
            tiltaksgjennomforinger.get(tiltak1.id) shouldBe tiltak1
            tiltaksgjennomforinger.getByTiltakstypeId(tiltakstype1.id) shouldHaveSize 1

            tiltaksgjennomforinger.delete(tiltak1.id)

            tiltaksgjennomforinger.getAll().second shouldHaveSize 1
        }
    }

    context("tilgjengelighetsstatus") {
        listener.db.clean()
        listener.db.migrate()

        val tiltakstyper = TiltakstypeRepository(listener.db)
        tiltakstyper.save(tiltakstype1)

        val deltakere = DeltakerRepository(listener.db)
        val deltaker = Deltaker(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = tiltak1.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.DELTAR,
        )

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(listener.db)

        context("when tilgjengelighet is set to Stengt") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    tiltak1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Stengt
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(tiltak1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
            }
        }

        context("when there are no limits to available seats") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    tiltak1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = null
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(tiltak1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
            }
        }

        context("when there are no available seats") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    tiltak1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 0
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(tiltak1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
            }
        }

        context("when all available seats are occupied by deltakelser with status DELTAR") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    tiltak1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 1
                    )
                ).shouldBeRight()

                deltakere.save(deltaker.copy(status = Deltakerstatus.DELTAR))
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(tiltak1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
            }
        }

        context("when deltakelser are no longer DELTAR") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    tiltak1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 1
                    )
                ).shouldBeRight()

                deltakere.save(deltaker.copy(status = Deltakerstatus.AVSLUTTET))
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(tiltak1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
            }
        }
    }

    context("pagination") {
        listener.db.clean()
        listener.db.migrate()

        val tiltakstyper = TiltakstypeRepository(listener.db)
        tiltakstyper.save(tiltakstype1)

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(listener.db)
        (1..105).forEach {
            tiltaksgjennomforinger.upsert(
                Tiltaksgjennomforing(
                    id = UUID.randomUUID(),
                    navn = "$it",
                    tiltakstypeId = tiltakstype1.id,
                    tiltaksnummer = "$it",
                    virksomhetsnummer = "123456789",
                    Tilgjengelighetsstatus.Ledig,
                    antallPlasser = null
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll()

            items.size shouldBe DEFAULT_PAGINATION_LIMIT
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "50"

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 61-80") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
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
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    3
                )
            )

            items.size shouldBe 5
            items.first().navn shouldBe "101"
            items.last().navn shouldBe "105"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
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
