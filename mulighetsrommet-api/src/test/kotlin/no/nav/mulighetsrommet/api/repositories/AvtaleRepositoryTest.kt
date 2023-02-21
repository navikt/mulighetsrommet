package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import java.time.LocalDate

class AvtaleRepositoryTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))
    val avtaleFixture = AvtaleFixtures(database)

    context("Filter for avtaler") {

        beforeContainer {
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

                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(search = "Kroko", avtalestatus = Avtalestatus.Aktiv, enhet = null)
                )

                result.second shouldHaveSize 1
                result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            }
        }

        context("Avtalestatus") {
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

            test("filtrer på avbrutt") {
                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(
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
                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(
                        search = null,
                        avtalestatus = Avtalestatus.Avsluttet,
                        enhet = null,
                        dagensDato = LocalDate.of(2023, 2, 1)
                    )
                )

                result.second shouldHaveSize 2
                result.second.map { it.id }.shouldContainAll(avtaleAvsluttetStatus.id, avtaleAvsluttetDato.id)
            }

            test("filtrer på planlagt") {
                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(
                        search = null,
                        avtalestatus = Avtalestatus.Planlagt,
                        enhet = null,
                        dagensDato = LocalDate.of(2023, 2, 1)
                    )
                )

                result.second shouldHaveSize 1
                result.second[0].id shouldBe avtalePlanlagt.id
            }

            test("filtrer på aktiv") {
                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(
                        search = null,
                        avtalestatus = Avtalestatus.Aktiv,
                        enhet = null,
                        dagensDato = LocalDate.of(2023, 2, 1)
                    )
                )

                result.second shouldHaveSize 1
                result.second[0].id shouldBe avtaleAktiv.id
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
                val result = avtaleRepository.getAvtalerForTiltakstype(
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    filter = AvtaleFilter(search = null, avtalestatus = Avtalestatus.Aktiv, enhet = "1801")
                )

                result.second shouldHaveSize 1
                result.second[0].enhet shouldBe "1801"
            }
        }
    }
})
