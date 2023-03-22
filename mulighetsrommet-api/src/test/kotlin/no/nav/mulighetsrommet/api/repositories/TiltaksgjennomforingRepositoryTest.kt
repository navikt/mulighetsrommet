package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo.Tilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltakstype1 = TiltakstypeFixtures.Arbeidstrening

    val tiltakstype2 = TiltakstypeFixtures.Oppfolging

    val gjennomforing1 = TiltaksgjennomforingFixtures.Arbeidstrening1

    val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging1

    context("CRUD") {
        beforeAny {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype1)
            tiltakstyper.upsert(tiltakstype2)
        }

        test("CRUD") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing2).shouldBeRight()

            tiltaksgjennomforinger.getAll(filter = filter).second shouldHaveSize 2
            tiltaksgjennomforinger.get(gjennomforing1.id) shouldBe TiltaksgjennomforingAdminDto(
                id = gjennomforing1.id,
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = tiltakstype1.id,
                    navn = tiltakstype1.navn,
                    arenaKode = tiltakstype1.tiltakskode,
                ),
                navn = gjennomforing1.navn,
                tiltaksnummer = gjennomforing1.tiltaksnummer,
                virksomhetsnummer = gjennomforing1.virksomhetsnummer,
                startDato = gjennomforing1.startDato,
                sluttDato = gjennomforing1.sluttDato,
                enhet = gjennomforing1.enhet,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                antallPlasser = null,
                avtaleId = gjennomforing1.avtaleId
            )

            tiltaksgjennomforinger.delete(gjennomforing1.id)

            tiltaksgjennomforinger.getAll(filter = filter).second shouldHaveSize 1
        }
    }

    context("tilgjengelighetsstatus") {
        database.db.clean()
        database.db.migrate()

        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype1)

        val deltakere = DeltakerRepository(database.db)
        val deltaker = DeltakerDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = gjennomforing1.id,
            status = Deltakerstatus.DELTAR,
            opphav = Deltakeropphav.AMT,
            startDato = null,
            sluttDato = null,
            registrertDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
        )

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        context("when tilgjengelighet is set to Stengt") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Stengt
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
            }
        }

        context("when avslutningsstatus is set") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Stengt
            }
        }

        context("when there are no limits to available seats") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = null
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
            }
        }

        context("when there are no available seats") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 0
                    )
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
            }
        }

        context("when all available seats are occupied by deltakelser with status DELTAR") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 1
                    )
                ).shouldBeRight()

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.DELTAR))
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Venteliste
            }
        }

        context("when deltakelser are no longer DELTAR") {
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                        antallPlasser = 1
                    )
                ).shouldBeRight()

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.AVSLUTTET))
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(gjennomforing1.id)?.tilgjengelighet shouldBe Tilgjengelighetsstatus.Ledig
            }
        }
    }

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
                    startDato = LocalDate.of(2022, 1, 1),
                    tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                    antallPlasser = null
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, items) = tiltaksgjennomforinger.getAll(filter = filter)

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
                ),
                filter
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
                ),
                filter
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
                ),
                filter
            )

            items.size shouldBe 105
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }
    }
})
