package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype

class AvtaleRepositoryTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))
    val avtaleFixture = AvtaleFixtures(database)

    context("Filter for avtaler") {

        beforeTest {
            avtaleFixture.runBeforeTests()
        }

        test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om opplæring av blinde krokodiller"
            )
            val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om undervisning av underlige ulver"
            )
            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))

            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = avtaleFixture.tiltakstypeId,
                filter = AvtaleFilter(search = "Kroko", avtalestatus = Avtalestatus.Aktiv, enhet = null)
            )

            result.second shouldHaveSize 1
            result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            result.second[0].avtaletype shouldBe Avtaletype.Rammeavtale
            result.second[0].avtalestatus shouldBe Avtalestatus.Aktiv
        }

        test("Filtrere på avtalestatus returnere avtaler med korrekt status") {
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om opplæring av blinde krokodiller",
                avtalestatus = Avtalestatus.Aktiv,
            )
            val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om undervisning av underlige ulver",
                avtalestatus = Avtalestatus.Avbrutt,
            )
            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))

            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = avtaleFixture.tiltakstypeId,
                filter = AvtaleFilter(search = null, avtalestatus = Avtalestatus.Avbrutt, enhet = null)
            )

            result.second shouldHaveSize 1
            result.second[0].navn shouldBe "Avtale om undervisning av underlige ulver"
            result.second[0].avtaletype shouldBe Avtaletype.Rammeavtale
            result.second[0].avtalestatus shouldBe Avtalestatus.Avbrutt
        }

        test("Filtrere på enhet returnerer avtaler for gitt enhet") {
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om opplæring av blinde krokodiller",
                enhet = "1801"
            )
            val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                navn = "Avtale om undervisning av underlige ulver",
                enhet = "1900"
            )
            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))
            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = avtaleFixture.tiltakstypeId,
                filter = AvtaleFilter(search = null, avtalestatus = Avtalestatus.Aktiv, enhet = "1801")
            )

            result.second shouldHaveSize 1
            result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            result.second[0].avtaletype shouldBe Avtaletype.Rammeavtale
            result.second[0].avtalestatus shouldBe Avtalestatus.Aktiv
            result.second[0].enhet shouldBe "1801"
        }
    }
})
