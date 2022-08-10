package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.database.kotest.extensions.DatabaseListener
import no.nav.mulighetsrommet.domain.Deltakerstatus
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing.Tilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import java.time.LocalDateTime

class TiltaksgjennomforingServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    context("CRUD") {
        val arenaService = ArenaService(listener.db)

        val service = TiltaksgjennomforingService(listener.db)

        beforeAny {
            arenaService.upsertTiltakstype(
                AdapterTiltak(
                    navn = "Arbeidstrening",
                    innsatsgruppe = 1,
                    tiltakskode = "ARBTREN",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )
            arenaService.upsertTiltakstype(
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
            sakId = 1,
        )
        val tiltak2 = AdapterTiltaksgjennomforing(
            navn = "Trening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 2,
            sakId = 2,
        )

        test("should return empty result when there are no created tiltak") {
            service.getTiltaksgjennomforinger() shouldBe listOf()
        }

        test("should return empty result when tiltak are missing tiltaksnummer") {
            arenaService.upsertTiltaksgjennomforing(tiltak1)
            arenaService.upsertTiltaksgjennomforing(tiltak2)

            service.getTiltaksgjennomforinger() shouldBe listOf()
        }

        test("should get tiltak when they have been assigned tiltaksnummer") {
            arenaService.updateTiltaksgjennomforingWithSak(
                AdapterSak(id = 1, lopenummer = 11, aar = 2022)
            )
            arenaService.updateTiltaksgjennomforingWithSak(
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
                ),
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
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(apentForInnsok = false))
                }

                afterAny {
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(apentForInnsok = true))
                }

                test("should have tilgjengelighet set to STENGT") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
                }
            }

            context("when there are no limits to available seats") {
                beforeAny {
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(antallPlasser = null))
                }

                test("should have tilgjengelighet set to APENT_FOR_INNSOK") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
                }
            }

            context("when there are no available seats") {
                beforeAny {
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(antallPlasser = 0))
                }

                test("should have tilgjengelighet set to VENTELISTE") {
                    service.getTiltaksgjennomforingById(1)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
                }
            }

            context("when all available seats are occupied by deltakelser with status DELTAR") {
                beforeAny {
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(antallPlasser = 1))

                    arenaService.upsertDeltaker(
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
                    arenaService.upsertTiltaksgjennomforing(tiltak1.copy(antallPlasser = 1))

                    arenaService.upsertDeltaker(
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
            arenaService.deleteTiltaksgjennomforing(tiltak1)

            service.getTiltaksgjennomforingById(tiltak1.id) shouldBe null
        }
    }
})
