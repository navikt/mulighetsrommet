package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.repositories.ArenaRepository
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing.Tilgjengelighetsstatus
import java.time.LocalDateTime

class TiltaksgjennomforingServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    context("CRUD") {
        val arenaRepository = ArenaRepository(listener.db)

        val service = TiltaksgjennomforingService(listener.db)

        beforeAny {
            arenaRepository.upsertTiltakstype(
                AdapterTiltak(
                    navn = "Arbeidstrening",
                    innsatsgruppe = 1,
                    tiltakskode = "ARBTREN",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )
            arenaRepository.upsertTiltakstype(
                AdapterTiltak(
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFOLG",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )
        }

        val tiltak1 = AdapterTiltaksgjennomforing(
            navn = "Oppfølging",
            arrangorId = 1,
            tiltakskode = "INDOPPFOLG",
            id = 1,
            sakId = 1
        )
        val tiltak2 = AdapterTiltaksgjennomforing(
            navn = "Trening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 2,
            sakId = 2
        )

        test("should return empty result when there are no created tiltak") {
            service.getTiltaksgjennomforinger() shouldBe listOf()
        }

        test("should return empty result when tiltak are missing tiltaksnummer") {
            arenaRepository.upsertTiltaksgjennomforing(tiltak1)
            arenaRepository.upsertTiltaksgjennomforing(tiltak2)

            service.getTiltaksgjennomforinger() shouldBe listOf()
        }

        test("should get tiltak when they have been assigned tiltaksnummer") {
            arenaRepository.updateTiltaksgjennomforingWithSak(
                AdapterSak(id = 1, lopenummer = 11, aar = 2022)
            )
            arenaRepository.updateTiltaksgjennomforingWithSak(
                AdapterSak(id = 2, lopenummer = 22, aar = 2022)
            )

            service.getTiltaksgjennomforinger() shouldBe listOf(
                Tiltaksgjennomforing(
                    id = 1,
                    navn = "Oppfølging",
                    tiltakskode = "INDOPPFOLG",
                    tiltaksnummer = 11,
                    aar = 2022,
                    tilgjengelighet = Tilgjengelighetsstatus.Ledig
                ),
                Tiltaksgjennomforing(
                    id = 2,
                    navn = "Trening",
                    tiltakskode = "ARBTREN",
                    tiltaksnummer = 22,
                    aar = 2022,
                    tilgjengelighet = Tilgjengelighetsstatus.Ledig
                )
            )
        }

        test("should get tiltak by id") {
            service.getTiltaksgjennomforingById(tiltak1.id) shouldBe Tiltaksgjennomforing(
                id = 1,
                navn = "Oppfølging",
                tiltakskode = "INDOPPFOLG",
                tiltaksnummer = 11,
                aar = 2022,
                tilgjengelighet = Tilgjengelighetsstatus.Ledig
            )
        }

        test("should get tiltaksgjennomføringer by tiltakskode") {
            service.getTiltaksgjennomforingerByTiltakskode("ARBTREN") shouldHaveSize 1
        }

        context("tilgjengelighetsstatus") {
            context("when tiltak is closed for applications") {
                beforeAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            apentForInnsok = false
                        )
                    )
                }

                afterAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            apentForInnsok = true
                        )
                    )
                }

                test("should have tilgjengelighet set to STENGT") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
                }
            }

            context("when there are no limits to available seats") {
                beforeAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            antallPlasser = null
                        )
                    )
                }

                test("should have tilgjengelighet set to APENT_FOR_INNSOK") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
                }
            }

            context("when there are no available seats") {
                beforeAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            antallPlasser = 0
                        )
                    )
                }

                test("should have tilgjengelighet set to VENTELISTE") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
                }
            }

            context("when all available seats are occupied by deltakelser with status DELTAR") {
                beforeAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            antallPlasser = 1
                        )
                    )

                    arenaRepository.upsertDeltaker(
                        AdapterTiltakdeltaker(
                            id = 1,
                            tiltaksgjennomforingId = tiltak1.id,
                            personId = 1,
                            status = Deltakerstatus.DELTAR
                        )
                    )
                }

                test("should have tilgjengelighet set to VENTELISTE") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
                }
            }

            context("when deltakelser are no longer DELTAR") {
                beforeAny {
                    arenaRepository.upsertTiltaksgjennomforing(
                        tiltak1.copy(
                            antallPlasser = 1
                        )
                    )

                    arenaRepository.upsertDeltaker(
                        AdapterTiltakdeltaker(
                            id = 1,
                            tiltaksgjennomforingId = tiltak1.id,
                            personId = 1,
                            status = Deltakerstatus.AVSLUTTET
                        )
                    )
                }

                test("should have tilgjengelighet set to APENT_FOR_INNSOK") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
                }
            }
        }

        test("should delete tiltak") {
            arenaRepository.deleteTiltaksgjennomforing(tiltak1)

            service.getTiltaksgjennomforingById(tiltak1.id) shouldBe null
        }

        context("pagination") {
            listener.db.clean()
            listener.db.migrate()

            arenaRepository.upsertTiltakstype(
                AdapterTiltak(
                    navn = "Arbeidstrening",
                    innsatsgruppe = 1,
                    tiltakskode = "ARBTREN",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )
            arenaRepository.upsertTiltakstype(
                AdapterTiltak(
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFOLG",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )

            (1..105).forEach {
                arenaRepository.upsertTiltaksgjennomforing(
                    AdapterTiltaksgjennomforing(
                        navn = "Trening",
                        arrangorId = 1,
                        tiltakskode = "ARBTREN",
                        id = it,
                        sakId = it
                    )
                )
                arenaRepository.updateTiltaksgjennomforingWithSak(
                    AdapterSak(
                        id = it,
                        lopenummer = it,
                        aar = 2022
                    )
                )
            }

            test("default pagination gets first 50 tiltak") {

                val tiltaksgjennomforinger =
                    service.getTiltaksgjennomforinger()

                tiltaksgjennomforinger.size shouldBe DEFAULT_PAGINATION_LIMIT
                tiltaksgjennomforinger.first().id shouldBe 1
                tiltaksgjennomforinger.last().id shouldBe 50
            }

            test("pagination with page 4 and size 20 should give tiltak with id 61-80") {
                val tiltaksgjennomforinger =
                    service.getTiltaksgjennomforinger(
                        PaginationParams(
                            4,
                            20
                        )
                    )

                tiltaksgjennomforinger.size shouldBe 20
                tiltaksgjennomforinger.first().id shouldBe 61
                tiltaksgjennomforinger.last().id shouldBe 80
            }

            test("pagination with page 3 default size should give tiltak with id 101-105") {
                val tiltaksgjennomforinger =
                    service.getTiltaksgjennomforinger(
                        PaginationParams(
                            3
                        )
                    )
                tiltaksgjennomforinger.size shouldBe 5
                tiltaksgjennomforinger.first().id shouldBe 101
                tiltaksgjennomforinger.last().id shouldBe 105
            }

            test("pagination with default page and size 200 should give tiltak with id 1-105") {
                val tiltaksgjennomforinger =
                    service.getTiltaksgjennomforinger(
                        PaginationParams(
                            nullableLimit = 200
                        )
                    )
                tiltaksgjennomforinger.size shouldBe 105
                tiltaksgjennomforinger.first().id shouldBe 1
                tiltaksgjennomforinger.last().id shouldBe 105
            }
        }
    }
})
