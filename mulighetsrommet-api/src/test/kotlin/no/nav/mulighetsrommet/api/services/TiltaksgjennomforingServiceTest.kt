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
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import java.util.UUID

class TiltaksgjennomforingServiceTest : FunSpec({

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
        tiltaksnummer = "12345"
    )

    val tiltak2 = Tiltaksgjennomforing(
        id = UUID.randomUUID(),
        navn = "Trening",
        tiltakstypeId = tiltakstype2.id,
        tiltaksnummer = "54321"
    )

    context("CRUD") {
        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val arenaService = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        val tiltaksgjennomforingService = TiltaksgjennomforingService(listener.db)

        beforeAny {
            arenaService.createOrUpdate(tiltakstype1)
            arenaService.createOrUpdate(tiltakstype2)
        }

        test("should return empty result when there are no created tiltak") {
            tiltaksgjennomforingService.getTiltaksgjennomforinger().second shouldBe listOf()
        }

        test("should return empty result when tiltak are missing tiltaksnummer") {
            arenaService.createOrUpdate(tiltak1)
            arenaService.createOrUpdate(tiltak2)

            tiltaksgjennomforingService.getTiltaksgjennomforinger().second shouldBe listOf()
        }

        test("should get tiltak by id") {
            tiltaksgjennomforingService.getTiltaksgjennomforingById(tiltak1.id) shouldBe tiltak1
        }

        test("should get tiltaksgjennomføringer by tiltakstypeId") {
            tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakstypeId(tiltakstype1.id) shouldHaveSize 1
        }

//        context("tilgjengelighetsstatus") {
//            context("when tiltak is closed for applications") {
//                beforeAny {
//                    arenaService.createOrUpdate(
//                        tiltak1.copy(
//                            apentForInnsok = false
//                        )
//                    )
//                }
//
//                afterAny {
//                    arenaService.upsertTiltaksgjennomforing(
//                        tiltak1.copy(
//                            apentForInnsok = true
//                        )
//                    )
//                }
//
//                test("should have tilgjengelighet set to STENGT") {
//                    tiltaksgjennomforingService.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
//                }
//            }
//
//            context("when there are no limits to available seats") {
//                beforeAny {
//                    arenaService.upsertTiltaksgjennomforing(
//                        tiltak1.copy(
//                            antallPlasser = null
//                        )
//                    )
//                }
//
//                test("should have tilgjengelighet set to APENT_FOR_INNSOK") {
//                    tiltaksgjennomforingService.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
//                }
//            }
//
//            context("when there are no available seats") {
//                beforeAny {
//                    arenaService.upsertTiltaksgjennomforing(
//                        tiltak1.copy(
//                            antallPlasser = 0
//                        )
//                    )
//                }
//
//                test("should have tilgjengelighet set to VENTELISTE") {
//                    tiltaksgjennomforingService.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
//                }
//            }
//
//            context("when all available seats are occupied by deltakelser with status DELTAR") {
//                beforeAny {
//                    arenaService.upsertTiltaksgjennomforing(
//                        tiltak1.copy(
//                            antallPlasser = 1
//                        )
//                    )
//
//                    arenaService.upsertDeltaker(
//                        AdapterTiltakdeltaker(
//                            tiltaksdeltakerId = 1,
//                            tiltaksgjennomforingId = tiltak1.tiltaksgjennomforingId,
//                            personId = 1,
//                            status = Deltakerstatus.DELTAR
//                        )
//                    )
//                }
//
//                test("should have tilgjengelighet set to VENTELISTE") {
//                    tiltaksgjennomforingService.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
//                }
//            }
//
//            context("when deltakelser are no longer DELTAR") {
//                beforeAny {
//                    arenaService.upsertTiltaksgjennomforing(
//                        tiltak1.copy(
//                            antallPlasser = 1
//                        )
//                    )
//
//                    arenaService.upsertDeltaker(
//                        AdapterTiltakdeltaker(
//                            tiltaksdeltakerId = 1,
//                            tiltaksgjennomforingId = tiltak1.tiltaksgjennomforingId,
//                            personId = 1,
//                            status = Deltakerstatus.AVSLUTTET
//                        )
//                    )
//                }
//
//                test("should have tilgjengelighet set to APENT_FOR_INNSOK") {
//                    tiltaksgjennomforingService.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
//                }
//            }
//        }

        test("should delete tiltak") {
            arenaService.remove(tiltak1)

            tiltaksgjennomforingService.getTiltaksgjennomforingById(tiltak1.id) shouldBe null
        }
    }

    context("pagination") {
        listener.db.clean()
        listener.db.migrate()

        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val arenaService = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(listener.db)

        arenaService.createOrUpdate(tiltakstype1)

        (1..105).forEach {
            arenaService.createOrUpdate(
                Tiltaksgjennomforing(
                    id = UUID.randomUUID(),
                    navn = "Trening$it",
                    tiltakstypeId = tiltakstype1.id,
                    tiltaksnummer = "$it"
                )
            )
        }

        test("default pagination gets first 50 tiltak") {

            val (totalCount, tiltaksgjennomforinger) =
                tiltaksgjennomforingService.getTiltaksgjennomforinger()

            tiltaksgjennomforinger.size shouldBe DEFAULT_PAGINATION_LIMIT
            tiltaksgjennomforinger.first().id shouldBe 1
            tiltaksgjennomforinger.last().id shouldBe 50

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 61-80") {
            val (totalCount, tiltaksgjennomforinger) =
                tiltaksgjennomforingService.getTiltaksgjennomforinger(
                    PaginationParams(
                        4,
                        20
                    )
                )

            tiltaksgjennomforinger.size shouldBe 20
            tiltaksgjennomforinger.first().id shouldBe 61
            tiltaksgjennomforinger.last().id shouldBe 80

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 101-105") {
            val (totalCount, tiltaksgjennomforinger) =
                tiltaksgjennomforingService.getTiltaksgjennomforinger(
                    PaginationParams(
                        3
                    )
                )
            tiltaksgjennomforinger.size shouldBe 5
            tiltaksgjennomforinger.first().id shouldBe 101
            tiltaksgjennomforinger.last().id shouldBe 105

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val (totalCount, tiltaksgjennomforinger) =
                tiltaksgjennomforingService.getTiltaksgjennomforinger(
                    PaginationParams(
                        nullableLimit = 200
                    )
                )
            tiltaksgjennomforinger.size shouldBe 105
            tiltaksgjennomforinger.first().id shouldBe 1
            tiltaksgjennomforinger.last().id shouldBe 105

            totalCount shouldBe 105
        }
    }
})
