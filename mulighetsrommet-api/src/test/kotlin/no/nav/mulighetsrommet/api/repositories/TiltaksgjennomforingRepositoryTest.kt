package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltakstype1 = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    val tiltakstype2 = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFOLG",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12)
    )

    val tiltak1 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakstypeId = tiltakstype1.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        startDato = LocalDate.of(2022, 1, 1),
        sluttDato = LocalDate.of(2022, 1, 1),
        enhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        avtaleId = 1000
    )

    val tiltak2 = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Trening",
        tiltakstypeId = tiltakstype2.id,
        tiltaksnummer = "54321",
        virksomhetsnummer = "123456789",
        enhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        startDato = LocalDate.of(2022, 1, 1),
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
            tiltaksgjennomforinger.get(tiltak1.id) shouldBe TiltaksgjennomforingAdminDto(
                id = tiltak1.id,
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = tiltakstype1.id,
                    navn = tiltakstype1.navn,
                    arenaKode = tiltakstype1.tiltakskode,
                ),
                navn = tiltak1.navn,
                tiltaksnummer = tiltak1.tiltaksnummer,
                virksomhetsnummer = tiltak1.virksomhetsnummer,
                startDato = tiltak1.startDato,
                sluttDato = tiltak1.sluttDato,
                enhet = tiltak1.enhet,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                avtaleId = tiltak1.avtaleId
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
                    navn = "Tiltak - $it",
                    tiltakstypeId = tiltakstype1.id,
                    tiltaksnummer = "$it",
                    virksomhetsnummer = "123456789",
                    enhet = "2990",
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    startDato = LocalDate.of(2022, 1, 1)
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll()

            items.size shouldBe DEFAULT_PAGINATION_LIMIT
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 49"

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 59-76") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    4,
                    20
                )
            )

            items.size shouldBe 20
            items.first().navn shouldBe "Tiltak - 59"
            items.last().navn shouldBe "Tiltak - 76"

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 95-99") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    3
                )
            )

            items.size shouldBe 5
            items.first().navn shouldBe "Tiltak - 95"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    nullableLimit = 200
                )
            )

            items.size shouldBe 105
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }
    }
})
