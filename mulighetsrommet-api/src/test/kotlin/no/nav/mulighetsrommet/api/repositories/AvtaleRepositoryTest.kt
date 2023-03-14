package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val avtaleFixture = AvtaleFixtures(database)

    context("Filter for avtaler") {

        val defaultFilter = AvtaleFilter(
            dagensDato = LocalDate.of(2023, 2, 1)
        )

        beforeEach {
            avtaleFixture.runBeforeTests()
        }

        context("Avtalenavn") {
            test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale om opplæring av blinde krokodiller",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale om undervisning av underlige ulver",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))

                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        search = "Kroko",
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
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        avtalestatus = Avtalestatus.Avbrutt,
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
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        avtalestatus = Avtalestatus.Avsluttet,
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

                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
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
            avtaleFixture.upsertTiltakstype(
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
                filter = defaultFilter.copy(
                    tiltakstypeId = tiltakstypeId
                )
            )

            result.second shouldHaveSize 2
            result.second[0].tiltakstype.id shouldBe tiltakstypeId
            result.second[1].tiltakstype.id shouldBe tiltakstypeId
        }

        context("Sortering") {
            test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Anders"
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Åse"
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Øyvind"
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Kjetil"
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Ærfuglen Ærle"
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "navn-ascending"
                    )
                )

                result.second shouldHaveSize 5
                result.second[0].navn shouldBe "Avtale hos Anders"
                result.second[1].navn shouldBe "Avtale hos Kjetil"
                result.second[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.second[3].navn shouldBe "Avtale hos Øyvind"
                result.second[4].navn shouldBe "Avtale hos Åse"
            }

            test("Sortering på navn fra å-a sorterer korrekt") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Anders"
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Åse"
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Øyvind"
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Kjetil"
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Ærfuglen Ærle"
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "navn-descending"
                    )
                )

                result.second shouldHaveSize 5
                result.second[0].navn shouldBe "Avtale hos Åse"
                result.second[1].navn shouldBe "Avtale hos Øyvind"
                result.second[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.second[3].navn shouldBe "Avtale hos Kjetil"
                result.second[4].navn shouldBe "Avtale hos Anders"
            }
        }

        context("Nøkkeltall") {
            val tiltakstypeRepository = TiltakstypeRepository(database.db)
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            val avtaleRepository = AvtaleRepository(database.db)

            test("Skal telle korrekt antall tiltaksgjennomføringer tilknyttet en avtale") {
                val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

                val avtale = avtaleFixture.createAvtaleForTiltakstype()
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche)

                val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = avtaleFixture.tiltakstypeId)
                val tiltakstypeUtenAvtaler = TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

                val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = avtaleFixture.tiltakstypeId
                )
                val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = avtaleFixture.tiltakstypeId
                )
                val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche
                )
                val gjennomforing4 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche
                )

                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
                tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

                avtaleFixture.upsertAvtaler(listOf(avtale, avtale2))

                tiltaksgjennomforingRepository.upsert(gjennomforing1).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing2).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing3).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing4).getOrThrow()

                val gjennomforinger = tiltaksgjennomforingRepository.getAll()
                gjennomforinger.first shouldBe 4

                val antallGjennomforingerForAvtale = avtaleRepository.countTiltaksgjennomforingerForAvtaleWithId(avtale.id)
                antallGjennomforingerForAvtale shouldBe 2
            }
            test("Skal telle korrekt antall avtaler for en tiltakstype") {
                val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

                val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = avtaleFixture.tiltakstypeId)
                val tiltakstypeUtenAvtaler = TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

                val avtale1 = avtaleFixture.createAvtaleForTiltakstype()
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype()
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype()
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype()
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche)
                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
                tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

                avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))

                val alleAvtaler = avtaleRepository.getAll(filter = defaultFilter)
                alleAvtaler.first shouldBe 5

                val countAvtaler = avtaleRepository.countAvtalerForTiltakstypeWithId(tiltakstype.id)
                countAvtaler shouldBe 4
            }
        }
    }
})
