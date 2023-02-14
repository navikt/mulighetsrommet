package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))
    val tiltakstypeId = UUID.fromString("0c565576-6a74-4bc2-ad5a-765580014ef9")

    context("Filter for avtaler") {

        beforeTest {
            database.db.clean()
            database.db.migrate()
            val tiltakstypeRepository = TiltakstypeRepository(database.db)

            tiltakstypeRepository.upsert(
                TiltakstypeDbo(
                    tiltakstypeId,
                    "",
                    "",
                    rettPaaTiltakspenger = true,
                    registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    fraDato = LocalDate.of(2023, 1, 11),
                    tilDato = LocalDate.of(2023, 1, 12)
                )
            )
        }

        test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
            val avtaleRepository = AvtaleRepository(database.db)

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                    avtalenummer = "2023#1",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1801",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Aktiv,
                    prisbetingelser = null
                )
            )

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om kurs for elleville elefanter",
                    avtalenummer = "2023#2",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1801",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Aktiv,
                    prisbetingelser = null
                )
            )

            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = tiltakstypeId,
                filter = AvtaleFilter(search = "Kroko", avtalestatus = Avtalestatus.Aktiv, enhet = null)
            )

            result.second shouldHaveSize 1
            result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            result.second[0].avtaletype shouldBe Avtaletype.Rammeavtale
            result.second[0].avtalestatus shouldBe Avtalestatus.Aktiv
        }

        test("Filtrere på avtalestatus returnere avtaler med korrekt status") {
            val avtaleRepository = AvtaleRepository(database.db)

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                    avtalenummer = "2023#1",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1801",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Aktiv,
                    prisbetingelser = null
                )
            )

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om kurs for elleville elefanter",
                    avtalenummer = "2023#2",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1801",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Avbrutt,
                    prisbetingelser = null
                )
            )

            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = tiltakstypeId,
                filter = AvtaleFilter(search = null, avtalestatus = Avtalestatus.Avbrutt, enhet = null)
            )

            result.second shouldHaveSize 1
            result.second[0].navn shouldBe "Avtale om kurs for elleville elefanter"
            result.second[0].avtaletype shouldBe Avtaletype.Rammeavtale
            result.second[0].avtalestatus shouldBe Avtalestatus.Avbrutt
        }

        test("Filtrere på enhet returnerer avtaler for gitt enhet") {
            val avtaleRepository = AvtaleRepository(database.db)

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                    avtalenummer = "2023#1",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1801",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Aktiv,
                    prisbetingelser = null
                )
            )

            avtaleRepository.upsert(
                AvtaleDbo(
                    id = UUID.randomUUID(),
                    navn = "Avtale om kurs for elleville elefanter",
                    avtalenummer = "2023#2",
                    tiltakstypeId = tiltakstypeId,
                    leverandorOrganisasjonsnummer = "12345678910",
                    startDato = LocalDate.of(2023, 1, 11),
                    sluttDato = LocalDate.of(2023, 2, 28),
                    enhet = "1900",
                    avtaletype = Avtaletype.Rammeavtale,
                    avtalestatus = Avtalestatus.Avbrutt,
                    prisbetingelser = null
                )
            )

            val result = avtaleRepository.getAvtalerForTiltakstype(
                tiltakstypeId = tiltakstypeId,
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
