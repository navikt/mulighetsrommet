package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))
    val avtaleFixture = AvtaleFixtures(database)

    context("Filter for avtaler") {

        beforeEach {
            avtaleFixture.runBeforeTests()
        }

        context("Avtalenavn") {
            test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale om opplæring av blinde krokodiller"
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale om undervisning av underlige ulver",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))

                val result = avtaleRepository.getAll(

                    filter = AvtaleFilter(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        search = "Kroko",
                        avtalestatus = Avtalestatus.Aktiv,
                        enhet = null
                    )
                )

                result.second shouldHaveSize 1
                result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            }
        }

        context("Avtalestatus") {
            test("filtrer på avbrutt") {
                val avtaleAktiv = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                )
                val avtaleAvsluttetStatus = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                )
                val avtaleAvsluttetDato = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    sluttDato = LocalDate.of(2023, 1, 31)
                )
                val avtaleAvbrutt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVBRUTT,
                )
                val avtalePlanlagt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    startDato = LocalDate.of(2023, 2, 2)
                )

                val avtaleRepository = avtaleFixture.upsertAvtaler(
                    listOf(
                        avtaleAktiv,
                        avtaleAvbrutt,
                        avtalePlanlagt,
                        avtaleAvsluttetDato,
                        avtaleAvsluttetStatus
                    )
                )
                val result = avtaleRepository.getAll(
                    filter = AvtaleFilter(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        search = null,
                        avtalestatus = Avtalestatus.Avbrutt,
                        enhet = null,
                        dagensDato = LocalDate.of(2023, 2, 1)
                    )
                )

                result.second shouldHaveSize 1
                result.second[0].id shouldBe avtaleAvbrutt.id
            }

            test("filtrer på avsluttet") {
                val avtaleAktiv = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                )
                val avtaleAvsluttetStatus = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                )
                val avtaleAvsluttetDato = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    sluttDato = LocalDate.of(2023, 1, 31)
                )
                val avtaleAvbrutt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVBRUTT,
                )
                val avtalePlanlagt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    startDato = LocalDate.of(2023, 2, 2)
                )

                val avtaleRepository = avtaleFixture.upsertAvtaler(
                    listOf(
                        avtaleAktiv,
                        avtaleAvbrutt,
                        avtalePlanlagt,
                        avtaleAvsluttetDato,
                        avtaleAvsluttetStatus
                    )
                )
                val result = avtaleRepository.getAll(
                    filter = AvtaleFilter(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        search = null,
                        avtalestatus = Avtalestatus.Avsluttet,
                        enhet = null,
                        dagensDato = LocalDate.of(2023, 2, 1)
                    )
                )

                result.second shouldHaveSize 2
                result.second.map { it.id }.shouldContainAll(avtaleAvsluttetStatus.id, avtaleAvsluttetDato.id)
            }
        }
        context("Enhet") {
            test("Filtrere på enhet returnerer avtaler for gitt enhet") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    enhet = "1801"
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    enhet = "1900"
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))
                val result = avtaleRepository.getAll(

                    filter = AvtaleFilter(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        search = null,
                        avtalestatus = Avtalestatus.Aktiv,
                        enhet = "1801"
                    )
                )
                result.second shouldHaveSize 1
                result.second[0].navEnhet.enhetsnummer shouldBe "1801"
            }
        }

        test("Filtrer på tiltakstypeId returnerer avtaler tilknyttet spesifikk tiltakstype") {
            val tiltakstypeId: UUID = avtaleFixture.tiltakstypeId
            val tiltakstypeIdForAvtale3: UUID = UUID.randomUUID()
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeId
            )
            val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeId
            )
            val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeIdForAvtale3
            )
            avtaleFixture.upserTiltakstype(
                listOf(
                    TiltakstypeDbo(
                        tiltakstypeIdForAvtale3,
                        "",
                        "",
                        rettPaaTiltakspenger = true,
                        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                        fraDato = LocalDate.of(2023, 1, 11),
                        tilDato = LocalDate.of(2023, 1, 12)
                    )
                )
            )
            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3))
            val result = avtaleRepository.getAll(
                filter = AvtaleFilter(
                    tiltakstypeId = tiltakstypeId
                )
            )

            result.second shouldHaveSize 2
            result.second[0].tiltakstype.id shouldBe tiltakstypeId
            result.second[1].tiltakstype.id shouldBe tiltakstypeId
        }
    }
})
