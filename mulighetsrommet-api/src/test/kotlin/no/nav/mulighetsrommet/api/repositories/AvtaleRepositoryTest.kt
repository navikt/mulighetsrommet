package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val avtaleFixture = AvtaleFixtures(database)

    beforeEach {
        avtaleFixture.runBeforeTests()
    }

    context("Avtaleansvarlig") {
        test("Ansvarlig blir satt i egen tabell") {
            val ident = "N12343"
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                ansvarlige = listOf(ident),
            )
            avtaleFixture.upsertAvtaler(listOf(avtale1))
            avtaleFixture.upsertAvtaler(listOf(avtale1))
            database.assertThat("avtale_ansvarlig").row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("navident").isEqualTo(ident)
        }

        test("Ansvarlig tabell blir oppdatert til å reflektere listen i dbo") {
            val ident = "N12343"
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                ansvarlige = listOf(ident),
            )
            avtaleFixture.upsertAvtaler(listOf(avtale1))

            avtaleFixture.upsertAvtaler(listOf(avtale1.copy(ansvarlige = listOf("M12343", "L12343"))))

            database.assertThat("avtale_ansvarlig").row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("navident").isEqualTo("L12343")
                .row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("navident").isEqualTo("M12343")

            database.assertThat("avtale_ansvarlig").hasNumberOfRows(2)
        }
    }

    context("Underenheter for leverandør til avtalen") {
        test("Underenheter blir populert i korrekt tabell") {
            val underenhet = "123456789"
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                leverandorUnderenheter = listOf(underenhet),
            )
            avtaleFixture.upsertAvtaler(listOf(avtale1))
            avtaleFixture.upsertAvtaler(listOf(avtale1))
            database.assertThat("avtale_underleverandor").row()
                .value("organisasjonsnummer").isEqualTo(underenhet)
                .value("avtale_id").isEqualTo(avtale1.id)
        }

        test("Underenheter blir riktig med fra spørring") {
            val virksomhetRepository = VirksomhetRepository(database.db)
            virksomhetRepository.upsertOverordnetEnhet(
                OverordnetEnhetDbo(
                    organisasjonsnummer = "999999999",
                    navn = "overordnet",
                    underenheter = listOf(
                        VirksomhetDto(
                            organisasjonsnummer = "888888888",
                            navn = "u8",
                        ),
                        VirksomhetDto(
                            organisasjonsnummer = "777777777",
                            navn = "u7",
                        ),
                    ),
                ),
            )

            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                leverandorOrganisasjonsnummer = "999999999",
                leverandorUnderenheter = listOf("888888888", "777777777"),
            )

            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1))
            avtaleRepository.get(avtale1.id).shouldBeRight().should {
                it!!.leverandorUnderenheter shouldContainExactlyInAnyOrder listOf(
                    AvtaleAdminDto.Leverandor(
                        organisasjonsnummer = "777777777",
                        navn = "u7",
                    ),
                    AvtaleAdminDto.Leverandor(
                        organisasjonsnummer = "888888888",
                        navn = "u8",
                    ),
                )
                it.leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "999999999",
                    navn = "overordnet",
                )
            }
        }
    }

    context("Filter for avtaler") {

        val defaultFilter = AvtaleFilter(
            dagensDato = LocalDate.of(2023, 2, 1),
        )

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
                    ),
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
                    sluttDato = LocalDate.of(2023, 1, 31),
                )
                val avtaleAvbrutt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVBRUTT,
                )
                val avtalePlanlagt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    startDato = LocalDate.of(2023, 2, 2),
                )

                val avtaleRepository = avtaleFixture.upsertAvtaler(
                    listOf(
                        avtaleAktiv,
                        avtaleAvbrutt,
                        avtalePlanlagt,
                        avtaleAvsluttetDato,
                        avtaleAvsluttetStatus,
                    ),
                )
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        avtalestatus = Avtalestatus.Avbrutt,
                    ),
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
                    sluttDato = LocalDate.of(2023, 1, 31),
                )
                val avtaleAvbrutt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.AVBRUTT,
                )
                val avtalePlanlagt = avtaleFixture.createAvtaleForTiltakstype(
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                    startDato = LocalDate.of(2023, 2, 2),
                )

                val avtaleRepository = avtaleFixture.upsertAvtaler(
                    listOf(
                        avtaleAktiv,
                        avtaleAvbrutt,
                        avtalePlanlagt,
                        avtaleAvsluttetDato,
                        avtaleAvsluttetStatus,
                    ),
                )
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        avtalestatus = Avtalestatus.Avsluttet,
                    ),
                )

                result.second shouldHaveSize 2
                result.second.map { it.id }.shouldContainAll(avtaleAvsluttetStatus.id, avtaleAvsluttetDato.id)
            }
        }

        context("NavEnhet") {
            test("Filtrere på region returnerer avtaler for gitt region") {
                val navEnhetRepository = NavEnhetRepository(database.db)
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "Oppland",
                        enhetsnummer = "1900",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.FYLKE,
                        overordnetEnhet = null,
                    ),
                )
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "Vestland",
                        enhetsnummer = "1801",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.FYLKE,
                        overordnetEnhet = null,
                    ),
                )

                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = "1801",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = "1900",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))
                val aa = avtaleRepository.get(avtale1.id).shouldBeRight()
                aa?.navRegion?.enhetsnummer shouldBe "1801"

                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = avtaleFixture.tiltakstypeId,
                        navRegion = "1801",
                    ),
                )
                result.second shouldHaveSize 1
                result.second[0].navRegion?.enhetsnummer shouldBe "1801"
                result.second[0].navRegion?.navn shouldBe "Vestland"
            }

            test("Avtale navenhet blir med riktig tilbake") {
                val navEnhetRepository = NavEnhetRepository(database.db)
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "Oppland",
                        enhetsnummer = "1900",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.FYLKE,
                        overordnetEnhet = null,
                    ),
                )
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "Oppland 1",
                        enhetsnummer = "1901",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = "1900",
                    ),
                )

                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = "1900",
                    navEnheter = listOf("1901"),
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1))
                avtaleRepository.get(avtale1.id).shouldBeRight().should {
                    it!!.navRegion?.enhetsnummer shouldBe "1900"
                    it.navEnheter shouldContainExactly listOf(NavEnhet(enhetsnummer = "1901", navn = "Oppland 1"))
                }
            }
        }

        test("Filtrer på tiltakstypeId returnerer avtaler tilknyttet spesifikk tiltakstype") {
            val tiltakstypeId: UUID = avtaleFixture.tiltakstypeId
            val tiltakstypeIdForAvtale3: UUID = UUID.randomUUID()
            val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeId,
            )
            val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeId,
            )
            val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                tiltakstypeId = tiltakstypeIdForAvtale3,
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
                        tilDato = LocalDate.of(2023, 1, 12),
                    ),
                ),
            )
            val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3))
            val result = avtaleRepository.getAll(
                filter = defaultFilter.copy(
                    tiltakstypeId = tiltakstypeId,
                ),
            )

            result.second shouldHaveSize 2
            result.second[0].tiltakstype.id shouldBe tiltakstypeId
            result.second[1].tiltakstype.id shouldBe tiltakstypeId
        }

        context("Sortering") {
            test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Åse",
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Øyvind",
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Kjetil",
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Ærfuglen Ærle",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "navn-ascending",
                    ),
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
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Åse",
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Øyvind",
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Kjetil",
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    navn = "Avtale hos Ærfuglen Ærle",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "navn-descending",
                    ),
                )

                result.second shouldHaveSize 5
                result.second[0].navn shouldBe "Avtale hos Åse"
                result.second[1].navn shouldBe "Avtale hos Øyvind"
                result.second[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.second[3].navn shouldBe "Avtale hos Kjetil"
                result.second[4].navn shouldBe "Avtale hos Anders"
            }

            test("Sortering på nav_enhet sorterer korrekt") {
                val navEnhetRepository = NavEnhetRepository(database.db)
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "alvdal",
                        enhetsnummer = "1",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.FYLKE,
                        overordnetEnhet = null,
                    ),
                )
                navEnhetRepository.upsert(
                    NavEnhetDbo(
                        navn = "zorro",
                        enhetsnummer = "2",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.FYLKE,
                        overordnetEnhet = null,
                    ),
                )
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = "1",
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = "2",
                    navn = "Avtale hos Åse",
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    navRegion = null,
                    navn = "Avtale hos Øyvind",
                )
                val avtaleRepository =
                    avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3))

                val ascending = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "nav-enhet-ascending",
                    ),
                )

                ascending.second shouldHaveSize 3
                ascending.second[0].navRegion shouldBe NavEnhet(enhetsnummer = "1", navn = "alvdal")
                ascending.second[1].navRegion shouldBe NavEnhet(enhetsnummer = "2", navn = "zorro")
                ascending.second[2].navRegion shouldBe null

                val descending = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "nav-enhet-descending",
                    ),
                )

                descending.second shouldHaveSize 3
                descending.second[0].navRegion shouldBe null
                descending.second[1].navRegion shouldBe NavEnhet(enhetsnummer = "2", navn = "zorro")
                descending.second[2].navRegion shouldBe NavEnhet(enhetsnummer = "1", navn = "alvdal")
            }

            test("Sortering på leverandor sorterer korrekt") {
                val virksomhetRepository = VirksomhetRepository(database.db)
                virksomhetRepository.upsert(
                    VirksomhetDto(
                        navn = "alvdal",
                        organisasjonsnummer = "987654321",
                    ),
                )
                virksomhetRepository.upsert(
                    VirksomhetDto(
                        navn = "bjarne",
                        organisasjonsnummer = "123456789",
                    ),
                )
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    leverandorOrganisasjonsnummer = "123456789",
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    leverandorOrganisasjonsnummer = "987654321",
                    navn = "Avtale hos Åse",
                )
                val avtaleRepository = avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2))

                val ascending = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "leverandor-ascending",
                    ),
                )

                ascending.second shouldHaveSize 2
                ascending.second[0].leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "987654321",
                    navn = "alvdal",
                )
                ascending.second[1].leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "123456789",
                    navn = "bjarne",
                )

                val descending = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "leverandor-descending",
                    ),
                )

                descending.second shouldHaveSize 2
                descending.second[0].leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "123456789",
                    navn = "bjarne",
                )
                descending.second[1].leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "987654321",
                    navn = "alvdal",
                )
            }

            test("Sortering på sluttdato fra a-å sorterer korrekt") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2010, 1, 31),
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2009, 1, 1),
                    navn = "Avtale hos Åse",
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2010, 1, 1),
                    navn = "Avtale hos Øyvind",
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2011, 1, 1),
                    navn = "Avtale hos Kjetil",
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2023, 1, 1),
                    navn = "Avtale hos Benny",
                )
                val avtale6 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2023, 1, 1),
                    navn = "Avtale hos Christina",
                )
                val avtaleRepository =
                    avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5, avtale6))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "sluttdato-descending",
                    ),
                )

                result.second shouldHaveSize 6
                result.second[0].sluttDato shouldBe LocalDate.of(2023, 1, 1)
                result.second[0].navn shouldBe "Avtale hos Benny"
                result.second[1].sluttDato shouldBe LocalDate.of(2023, 1, 1)
                result.second[1].navn shouldBe "Avtale hos Christina"
                result.second[2].sluttDato shouldBe LocalDate.of(2011, 1, 1)
                result.second[2].navn shouldBe "Avtale hos Kjetil"
                result.second[3].sluttDato shouldBe LocalDate.of(2010, 1, 31)
                result.second[3].navn shouldBe "Avtale hos Anders"
                result.second[4].sluttDato shouldBe LocalDate.of(2010, 1, 1)
                result.second[4].navn shouldBe "Avtale hos Øyvind"
                result.second[5].sluttDato shouldBe LocalDate.of(2009, 1, 1)
                result.second[5].navn shouldBe "Avtale hos Åse"
            }

            test("Sortering på sluttdato fra å-a sorterer korrekt") {
                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2010, 1, 31),
                    navn = "Avtale hos Anders",
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2009, 1, 1),
                    navn = "Avtale hos Åse",
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2010, 1, 1),
                    navn = "Avtale hos Øyvind",
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2011, 1, 1),
                    navn = "Avtale hos Kjetil",
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2023, 1, 1),
                    navn = "Avtale hos Benny",
                )
                val avtale6 = avtaleFixture.createAvtaleForTiltakstype(
                    sluttDato = LocalDate.of(2023, 1, 1),
                    navn = "Avtale hos Christina",
                )
                val avtaleRepository =
                    avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5, avtale6))
                val result = avtaleRepository.getAll(
                    filter = defaultFilter.copy(
                        sortering = "sluttdato-ascending",
                    ),
                )

                result.second shouldHaveSize 6
                result.second[0].sluttDato shouldBe LocalDate.of(2009, 1, 1)
                result.second[0].navn shouldBe "Avtale hos Åse"
                result.second[1].sluttDato shouldBe LocalDate.of(2010, 1, 1)
                result.second[1].navn shouldBe "Avtale hos Øyvind"
                result.second[2].sluttDato shouldBe LocalDate.of(2010, 1, 31)
                result.second[2].navn shouldBe "Avtale hos Anders"
                result.second[3].sluttDato shouldBe LocalDate.of(2011, 1, 1)
                result.second[3].navn shouldBe "Avtale hos Kjetil"
                result.second[4].sluttDato shouldBe LocalDate.of(2023, 1, 1)
                result.second[4].navn shouldBe "Avtale hos Benny"
                result.second[5].sluttDato shouldBe LocalDate.of(2023, 1, 1)
                result.second[5].navn shouldBe "Avtale hos Christina"
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
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2022, 10, 15),
                )
                val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = avtaleFixture.tiltakstypeId,
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                    avtaleId = avtale.id,
                )
                val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                )
                val gjennomforing4 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                    id = UUID.randomUUID(),
                    tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                )

                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
                tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

                avtaleFixture.upsertAvtaler(listOf(avtale, avtale2))

                tiltaksgjennomforingRepository.upsert(gjennomforing1).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing2).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing3).getOrThrow()
                tiltaksgjennomforingRepository.upsert(gjennomforing4).getOrThrow()
                val filter = AdminTiltaksgjennomforingFilter()
                val gjennomforinger = tiltaksgjennomforingRepository.getAll(filter = filter).shouldBeRight()
                gjennomforinger.first shouldBe 3

                val antallGjennomforingerForAvtale =
                    avtaleRepository.countTiltaksgjennomforingerForAvtaleWithId(avtale.id)
                antallGjennomforingerForAvtale shouldBe 1
            }

            test("Skal telle korrekt antall avtaler for en tiltakstype") {
                val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

                val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = avtaleFixture.tiltakstypeId)
                val tiltakstypeUtenAvtaler = TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

                val avtale1 = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2022, 10, 15),
                )
                val avtale2 = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                )
                val avtale3 = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                )
                val avtale4 = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2050, 10, 15),
                )
                val avtale5 = avtaleFixture.createAvtaleForTiltakstype(tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche)
                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
                tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

                avtaleFixture.upsertAvtaler(listOf(avtale1, avtale2, avtale3, avtale4, avtale5))

                val alleAvtaler = avtaleRepository.getAll(filter = defaultFilter)
                alleAvtaler.first shouldBe 5

                val countAvtaler =
                    avtaleRepository.countAktiveAvtalerForTiltakstypeWithId(tiltakstype.id, LocalDate.of(2023, 3, 14))
                countAvtaler shouldBe 3
            }
        }
    }

    context("Notifikasjoner for avtaler") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)

        context("Avtaler nærmer seg sluttdato") {
            test("Skal returnere avtaler som har sluttdato om 6 mnd, 3 mnd, 14 dager og 7 dager") {
                val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = avtaleFixture.tiltakstypeId)
                val avtale6Mnd = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 11, 30),
                    ansvarlige = listOf("OLE"),
                )
                val avtale3Mnd = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 8, 31),
                    ansvarlige = listOf("OLE"),
                )
                val avtale14Dag = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 6, 14),
                    ansvarlige = listOf("OLE"),
                )
                val avtale7Dag = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 6, 7),
                    ansvarlige = listOf("OLE"),
                )
                val avtaleSomIkkeSkalMatche = avtaleFixture.createAvtaleForTiltakstype(
                    startDato = LocalDate.of(2022, 6, 7),
                    sluttDato = LocalDate.of(2024, 1, 1),
                    ansvarlige = listOf("OLE"),
                )

                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()

                avtaleFixture.upsertAvtaler(
                    listOf(
                        avtale6Mnd,
                        avtale3Mnd,
                        avtale14Dag,
                        avtale7Dag,
                        avtaleSomIkkeSkalMatche,
                    ),
                )

                val result =
                    avtaleRepository.getAllAvtalerSomNarmerSegSluttdato(currentDate = LocalDate.of(2023, 5, 31))
                result.size shouldBe 4
                result[0].ansvarlige[0] shouldBe "OLE"
            }
        }
    }
})
