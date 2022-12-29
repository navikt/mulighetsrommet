package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    val tiltakstype1 = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN"
    )

    val tiltakstype2 = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFOLG"
    )

    val tiltak1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakstypeId = tiltakstype1.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        fraDato = LocalDateTime.of(2022, 1, 1, 8, 0),
        tilDato = LocalDateTime.of(2022, 1, 1, 8, 0),
        enhet = "2990"
    )

    val tiltak2 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Trening",
        tiltakstypeId = tiltakstype2.id,
        tiltaksnummer = "54321",
        virksomhetsnummer = "123456789",
        enhet = "2990"
    )

    context("CRUD") {
        beforeAny {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype1)
            tiltakstyper.upsert(tiltakstype2)
        }

        test("CRUD") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(tiltak1)
            tiltaksgjennomforinger.upsert(tiltak2)

            tiltaksgjennomforinger.getAll().second shouldHaveSize 2
            tiltaksgjennomforinger.get(tiltak1.id) shouldBe TiltaksgjennomforingDto(
                id = tiltak1.id,
                tiltakstype = TiltakstypeDto(
                    id = tiltakstype1.id,
                    navn = tiltakstype1.navn,
                    kode = tiltakstype1.tiltakskode,
                ),
                navn = tiltak1.navn,
                tiltaksnummer = tiltak1.tiltaksnummer,
                virksomhetsnummer = tiltak1.virksomhetsnummer,
                fraDato = tiltak1.fraDato,
                tilDato = tiltak1.tilDato,
                enhet = tiltak1.enhet
            )

            tiltaksgjennomforinger.delete(tiltak1.id)

            tiltaksgjennomforinger.getAll().second shouldHaveSize 1
        }
    }

//        TODO: implementer på nytt
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

    context("pagination") {
        database.db.clean()
        database.db.migrate()

        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype1)

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        (1..105).forEach {
            tiltaksgjennomforinger.upsert(
                TiltaksgjennomforingDbo(
                    id = UUID.randomUUID(),
                    navn = "$it",
                    tiltakstypeId = tiltakstype1.id,
                    tiltaksnummer = "$it",
                    virksomhetsnummer = "123456789",
                    enhet = "2990"
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
