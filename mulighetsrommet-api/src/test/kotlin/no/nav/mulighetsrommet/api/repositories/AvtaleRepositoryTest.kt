package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    context("CRUD") {
        test("Upsert av Arena-avtaler") {
            val tiltakstypeRepository = TiltakstypeRepository(database.db)
            val avtaler = AvtaleRepository(database.db)

            val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = TiltakstypeFixtures.Oppfolging.id)
            tiltakstypeRepository.upsert(tiltakstype).shouldBeRight()

            val avtaleId = UUID.randomUUID()
            val avtale = ArenaAvtaleDbo(
                id = avtaleId,
                navn = "Avtale til test",
                tiltakstypeId = tiltakstype.id,
                avtalenummer = "2023#123",
                leverandorOrganisasjonsnummer = "123456789",
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = "0400",
                avtaletype = Avtaletype.Avtale,
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                opphav = ArenaMigrering.Opphav.ARENA,
                prisbetingelser = "Alt er dyrt",
            )

            avtaler.upsertArenaAvtale(avtale)
            val upsertedAvtale = avtaler.get(avtale.id)
            upsertedAvtale.shouldNotBeNull()
            upsertedAvtale.should {
                it.id shouldBe avtaleId
                it.tiltakstype shouldBe AvtaleAdminDto.Tiltakstype(
                    id = tiltakstype.id,
                    navn = tiltakstype.navn,
                    arenaKode = tiltakstype.tiltakskode,
                )
                it.navn shouldBe "Avtale til test"
                it.avtalenummer shouldBe "2023#123"
                it.leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "123456789",
                    navn = null,
                    slettet = true,
                )
                it.startDato shouldBe LocalDate.of(2023, 1, 1)
                it.sluttDato shouldBe LocalDate.of(2023, 2, 2)
                it.avtaletype shouldBe Avtaletype.Avtale
                it.avtalestatus shouldBe Avtalestatus.Avsluttet
                it.opphav shouldBe ArenaMigrering.Opphav.ARENA
                it.prisbetingelser shouldBe "Alt er dyrt"
            }
        }

        test("administrator for avtale") {
            val avtaler = AvtaleRepository(database.db)

            val ansatt1 = NavAnsattFixture.ansatt1
            val ansatt2 = NavAnsattFixture.ansatt2
            val avtale1 = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(ansatt1.navIdent),
            )

            avtaler.upsert(avtale1)

            database.assertThat("avtale_administrator").row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("nav_ident").isEqualTo(ansatt1.navIdent)

            avtaler.upsert(avtale1.copy(administratorer = listOf(ansatt1.navIdent, ansatt2.navIdent)))

            database.assertThat("avtale_administrator")
                .hasNumberOfRows(2)
                .row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("nav_ident").isEqualTo(ansatt1.navIdent)
                .row()
                .value("avtale_id").isEqualTo(avtale1.id)
                .value("nav_ident").isEqualTo(ansatt2.navIdent)
        }

        test("Leverandør kontaktperson") {
            val virksomhetRepository = VirksomhetRepository(database.db)
            virksomhetRepository.upsert(
                VirksomhetDto(
                    organisasjonsnummer = "999888777",
                    navn = "Rema 1000",
                ),
            )
            val leverandorKontaktperson = VirksomhetKontaktperson(
                id = UUID.randomUUID(),
                organisasjonsnummer = "999888777",
                navn = "Navn Navnesen",
                telefon = "22232322",
                epost = "navn@gmail.com",
                beskrivelse = "beskrivelse",
            )
            virksomhetRepository.upsertKontaktperson(leverandorKontaktperson)

            val avtaler = AvtaleRepository(database.db)
            var avtale = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                leverandorKontaktpersonId = leverandorKontaktperson.id,
            )
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).should {
                it!!.leverandorKontaktperson shouldBe leverandorKontaktperson
            }

            // Endre kontaktperson
            val nyPerson = leverandorKontaktperson.copy(
                navn = "Fredrik Navnesen",
                telefon = "32322",
            )
            virksomhetRepository.upsertKontaktperson(nyPerson)

            avtale = avtale.copy(
                leverandorKontaktpersonId = nyPerson.id,
            )
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).should {
                it!!.leverandorKontaktperson shouldBe nyPerson
            }

            // Fjern kontaktperson
            avtale = avtale.copy(
                leverandorKontaktpersonId = null,
            )
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).should {
                it!!.leverandorKontaktperson shouldBe null
            }
        }

        test("Underenheter blir populert i korrekt tabell") {
            val avtaler = AvtaleRepository(database.db)
            val underenhet = "123456789"
            val avtale1 = AvtaleFixtures.avtale1.copy(
                leverandorUnderenheter = listOf(underenhet),
            )
            avtaler.upsert(avtale1)
            database.assertThat("avtale_underleverandor").row()
                .value("organisasjonsnummer").isEqualTo(underenhet)
                .value("avtale_id").isEqualTo(avtale1.id)
        }

        test("Underenheter blir riktig med fra spørring") {
            val virksomhetRepository = VirksomhetRepository(database.db)
            val avtaler = AvtaleRepository(database.db)
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

            val avtale1 = AvtaleFixtures.avtale1.copy(
                leverandorOrganisasjonsnummer = "999999999",
                leverandorUnderenheter = listOf("888888888", "777777777"),
            )

            avtaler.upsert(avtale1)
            avtaler.get(avtale1.id).should {
                it!!.leverandorUnderenheter shouldContainExactlyInAnyOrder listOf(
                    AvtaleAdminDto.LeverandorUnderenhet(
                        organisasjonsnummer = "777777777",
                        navn = "u7",
                    ),
                    AvtaleAdminDto.LeverandorUnderenhet(
                        organisasjonsnummer = "888888888",
                        navn = "u8",
                    ),
                )
                it.leverandor shouldBe AvtaleAdminDto.Leverandor(
                    organisasjonsnummer = "999999999",
                    navn = "overordnet",
                    slettet = false,
                )
            }
        }
    }

    context("Filter for avtaler") {
        val defaultFilter = AvtaleFilter(
            dagensDato = LocalDate.of(2023, 2, 1),
        )
        val avtaler = AvtaleRepository(database.db)

        context("Avtalenavn") {
            test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
                val avtale1 = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                )
                val avtale2 = avtale1.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om undervisning av underlige ulver",
                )
                avtaler.upsert(avtale1)
                avtaler.upsert(avtale2)
                val result = avtaler.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                        search = "Kroko",
                    ),
                )

                result.second shouldHaveSize 1
                result.second[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            }
        }

        test("administrator") {
            val a1 = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            val a2 = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
            )

            avtaler.upsert(a1)
            avtaler.upsert(a2)

            avtaler.getAll(filter = AvtaleFilter(administratorNavIdent = NavAnsattFixture.ansatt1.navIdent)).should {
                it.second.map { tg -> tg.id } shouldContainExactlyInAnyOrder listOf(a1.id, a2.id)
            }

            avtaler.getAll(filter = AvtaleFilter(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent)).should {
                it.second.map { tg -> tg.id } shouldContainExactlyInAnyOrder listOf(a2.id)
            }
        }

        context("Avtalestatus") {
            test("filtrering på Avtalestatus") {
                val avtaleAktiv = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                )
                avtaler.upsert(avtaleAktiv)
                avtaler.setAvslutningsstatus(avtaleAktiv.id, Avslutningsstatus.IKKE_AVSLUTTET)

                val avtaleAvsluttetStatus = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                )
                avtaler.upsert(avtaleAvsluttetStatus)
                avtaler.setAvslutningsstatus(avtaleAvsluttetStatus.id, Avslutningsstatus.AVSLUTTET)

                val avtaleAvsluttetDato = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.of(2023, 1, 31),
                )
                avtaler.upsert(avtaleAvsluttetDato)
                avtaler.setAvslutningsstatus(avtaleAvsluttetDato.id, Avslutningsstatus.IKKE_AVSLUTTET)

                val avtaleAvbrutt = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                )
                avtaler.upsert(avtaleAvbrutt)
                avtaler.setAvslutningsstatus(avtaleAvbrutt.id, Avslutningsstatus.AVBRUTT)

                val avtalePlanlagt = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2023, 2, 2),
                )
                avtaler.upsert(avtalePlanlagt)
                avtaler.setAvslutningsstatus(avtalePlanlagt.id, Avslutningsstatus.IKKE_AVSLUTTET)

                forAll(
                    row(Avtalestatus.Avbrutt, listOf(avtaleAvbrutt.id)),
                    row(Avtalestatus.Avsluttet, listOf(avtaleAvsluttetStatus.id, avtaleAvsluttetDato.id)),
                ) { status, expected ->
                    val result = avtaler.getAll(
                        filter = defaultFilter.copy(avtalestatus = status),
                    )
                    result.second.map { it.id } shouldContainExactlyInAnyOrder expected
                }
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

                val avtale1 = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    navEnheter = listOf("1801"),
                )
                val avtale2 = avtale1.copy(
                    id = UUID.randomUUID(),
                    navEnheter = listOf("1900"),
                )

                avtaler.upsert(avtale1)
                avtaler.upsert(avtale2)

                val aa = avtaler.get(avtale1.id)
                aa!!.navEnheter shouldContain NavEnhet(
                    enhetsnummer = "1801",
                    navn = "Vestland",
                )

                val result = avtaler.getAll(
                    filter = defaultFilter.copy(
                        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                        navRegion = "1801",
                    ),
                )
                result.second shouldHaveSize 1
                result.second[0].navEnheter shouldContain NavEnhet(
                    enhetsnummer = "1801",
                    navn = "Vestland",
                )
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

                val avtale1 = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    navEnheter = listOf("1900", "1901"),
                )
                avtaler.upsert(avtale1)
                avtaler.get(avtale1.id).should {
                    it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                        NavEnhet(
                            enhetsnummer = "1900",
                            navn = "Oppland",
                        ),
                        NavEnhet(enhetsnummer = "1901", navn = "Oppland 1"),
                    )
                }
            }
        }

        test("Filtrer på tiltakstypeId returnerer avtaler tilknyttet spesifikk tiltakstype") {
            val tiltakstyper = TiltakstypeRepository(database.db)
            val tiltakstypeId: UUID = TiltakstypeFixtures.Oppfolging.id
            val tiltakstypeIdForAvtale3: UUID = UUID.randomUUID()
            val avtale1 = AvtaleFixtures.avtale1.copy(
                tiltakstypeId = tiltakstypeId,
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeId,
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeIdForAvtale3,
            )

            tiltakstyper.upsert(
                TiltakstypeFixtures.Oppfolging.copy(
                    id = tiltakstypeIdForAvtale3,
                    navn = "",
                    tiltakskode = "",
                    rettPaaTiltakspenger = true,
                    registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    fraDato = LocalDate.of(2023, 1, 11),
                    tilDato = LocalDate.of(2023, 1, 12),
                ),
            )

            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            val result = avtaler.getAll(
                filter = defaultFilter.copy(
                    tiltakstypeId = tiltakstypeId,
                ),
            )

            result.second shouldHaveSize 2
            result.second[0].tiltakstype.id shouldBe tiltakstypeId
            result.second[1].tiltakstype.id shouldBe tiltakstypeId
        }
    }

    context("Sortering") {
        val avtaler = AvtaleRepository(database.db)

        test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
            val avtale1 = AvtaleFixtures.avtale1.copy(
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Øyvind",
            )
            val avtale4 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Kjetil",
            )
            val avtale5 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Ærfuglen Ærle",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            avtaler.upsert(avtale4)
            avtaler.upsert(avtale5)
            val result = avtaler.getAll(
                filter = AvtaleFilter(
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
            val avtale1 = AvtaleFixtures.avtale1.copy(
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Øyvind",
            )
            val avtale4 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Kjetil",
            )
            val avtale5 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Ærfuglen Ærle",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            avtaler.upsert(avtale4)
            avtaler.upsert(avtale5)
            val result = avtaler.getAll(
                filter = AvtaleFilter(
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

        test("Filtrer på tiltakstype og nav-region forholder seg til korrekt logikk i filter-spørring") {
            val tiltakstypeId = UUID.randomUUID()

            val navEnhetRepository = NavEnhetRepository(database.db)
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(
                TiltakstypeFixtures.Oppfolging.copy(
                    id = tiltakstypeId,
                    navn = "",
                    tiltakskode = "",
                    rettPaaTiltakspenger = true,
                    registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    fraDato = LocalDate.of(2023, 1, 11),
                    tilDato = LocalDate.of(2023, 1, 12),
                ),
            )

            navEnhetRepository.upsert(
                NavEnhetDbo(
                    navn = "NAV Oslo",
                    "0300",
                    NavEnhetStatus.AKTIV,
                    Norg2Type.FYLKE,
                    null,
                ),
            ).getOrThrow()

            val avtale1 = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Øyvind",
                tiltakstypeId = tiltakstypeId,
            )

            avtaler.upsert(avtale1)
            Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${avtale1.id}'").asUpdate.let {
                database.db.run(it)
            }
            avtaler.upsert(avtale2)
            Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${avtale2.id}'").asUpdate.let {
                database.db.run(it)
            }
            avtaler.upsert(avtale3)
            Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${avtale3.id}'").asUpdate.let {
                database.db.run(it)
            }

            val result = avtaler.getAll(
                filter = AvtaleFilter(
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    navRegion = "0300",
                ),
            )

            result.second shouldHaveSize 2
        }

        // TODO Fikse sortering på regioner for avtaler når avtaler kan ha flere regioner...
        xtest("Sortering på nav_enhet sorterer korrekt") {
            val navEnhetRepository = NavEnhetRepository(database.db)
            navEnhetRepository.upsert(
                NavEnhetDbo(
                    navn = "alvdal",
                    enhetsnummer = "0100",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            )
            navEnhetRepository.upsert(
                NavEnhetDbo(
                    navn = "zorro",
                    enhetsnummer = "0200",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            )
            val avtale1 = AvtaleFixtures.avtale1.copy(
                navEnheter = listOf("0100"),
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                navEnheter = listOf("0200"),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale hos Øyvind",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)

            val ascending = avtaler.getAll(
                filter = AvtaleFilter(
                    sortering = "nav-enhet-ascending",
                ),
            )

            ascending.second shouldHaveSize 3
            ascending.second[0].navEnheter shouldContain NavEnhet(enhetsnummer = "0100", navn = "alvdal")
            ascending.second[1].navEnheter shouldContain NavEnhet(enhetsnummer = "0100", navn = "alvdal")
            ascending.second[2].navEnheter shouldContain NavEnhet(enhetsnummer = "0200", navn = "zorro")

            val descending = avtaler.getAll(
                filter = AvtaleFilter(
                    sortering = "nav-enhet-descending",
                ),
            )

            descending.second shouldHaveSize 3
            descending.second[0].navEnheter shouldContain NavEnhet(enhetsnummer = "0200", navn = "zorro")
            descending.second[1].navEnheter shouldContain NavEnhet(enhetsnummer = "0100", navn = "alvdal")
            descending.second[2].navEnheter shouldContain NavEnhet(enhetsnummer = "0100", navn = "alvdal")
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
            val avtale1 = AvtaleFixtures.avtale1.copy(
                leverandorOrganisasjonsnummer = "123456789",
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                leverandorOrganisasjonsnummer = "987654321",
                navn = "Avtale hos Åse",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)

            val ascending = avtaler.getAll(
                filter = AvtaleFilter(
                    sortering = "leverandor-ascending",
                ),
            )

            ascending.second shouldHaveSize 2
            ascending.second[0].leverandor shouldBe AvtaleAdminDto.Leverandor(
                organisasjonsnummer = "987654321",
                navn = "alvdal",
                slettet = false,
            )
            ascending.second[1].leverandor shouldBe AvtaleAdminDto.Leverandor(
                organisasjonsnummer = "123456789",
                navn = "bjarne",
                slettet = false,
            )

            val descending = avtaler.getAll(
                filter = AvtaleFilter(
                    sortering = "leverandor-descending",
                ),
            )

            descending.second shouldHaveSize 2
            descending.second[0].leverandor shouldBe AvtaleAdminDto.Leverandor(
                organisasjonsnummer = "123456789",
                navn = "bjarne",
                slettet = false,
            )
            descending.second[1].leverandor shouldBe AvtaleAdminDto.Leverandor(
                organisasjonsnummer = "987654321",
                navn = "alvdal",
                slettet = false,
            )
        }

        test("Sortering på sluttdato fra a-å sorterer korrekt") {
            val avtale1 = AvtaleFixtures.avtale1.copy(
                sluttDato = LocalDate.of(2010, 1, 31),
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2009, 1, 1),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2010, 1, 1),
                navn = "Avtale hos Øyvind",
            )
            val avtale4 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2011, 1, 1),
                navn = "Avtale hos Kjetil",
            )
            val avtale5 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 1, 1),
                navn = "Avtale hos Benny",
            )
            val avtale6 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 1, 1),
                navn = "Avtale hos Christina",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            avtaler.upsert(avtale4)
            avtaler.upsert(avtale5)
            avtaler.upsert(avtale6)

            val result = avtaler.getAll(
                filter = AvtaleFilter(
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
            val avtale1 = AvtaleFixtures.avtale1.copy(
                sluttDato = LocalDate.of(2010, 1, 31),
                navn = "Avtale hos Anders",
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2009, 1, 1),
                navn = "Avtale hos Åse",
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2010, 1, 1),
                navn = "Avtale hos Øyvind",
            )
            val avtale4 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2011, 1, 1),
                navn = "Avtale hos Kjetil",
            )
            val avtale5 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 1, 1),
                navn = "Avtale hos Benny",
            )
            val avtale6 = avtale1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 1, 1),
                navn = "Avtale hos Christina",
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            avtaler.upsert(avtale4)
            avtaler.upsert(avtale5)
            avtaler.upsert(avtale6)

            val result = avtaler.getAll(
                filter = AvtaleFilter(
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
        val avtaler = AvtaleRepository(database.db)

        test("Skal telle korrekt antall tiltaksgjennomføringer tilknyttet en avtale") {
            val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

            val avtale1 =
                AvtaleFixtures.avtale1.copy(id = UUID.randomUUID(), tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche)
            val tiltakstypeUtenAvtaler = TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)
            val avtaleId = UUID.randomUUID()

            val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 10, 15),
            )
            val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
                avtaleId = avtaleId,
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

            tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

            avtaler.upsert(avtale1.copy(id = avtaleId))

            tiltaksgjennomforingRepository.upsert(gjennomforing1)
            tiltaksgjennomforingRepository.upsert(gjennomforing2)
            tiltaksgjennomforingRepository.upsert(gjennomforing3)
            tiltaksgjennomforingRepository.upsert(gjennomforing4)

            val gjennomforinger = tiltaksgjennomforingRepository.getAll()
            gjennomforinger.first shouldBe 4

            val antallGjennomforingerForAvtale =
                avtaler.countTiltaksgjennomforingerForAvtaleWithId(avtaleId)
            antallGjennomforingerForAvtale shouldBe 1
        }

        test("Skal telle korrekt antall avtaler for en tiltakstype") {
            val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

            val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = TiltakstypeFixtures.Oppfolging.id)
            val tiltakstypeUtenAvtaler = TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

            val avtale1 = AvtaleFixtures.avtale1.copy(
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2022, 10, 15),
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val avtale4 = avtale1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val avtale5 = avtale1.copy(id = UUID.randomUUID(), tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche)
            tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
            tiltakstypeRepository.upsert(tiltakstypeUtenAvtaler).getOrThrow()

            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            avtaler.upsert(avtale4)
            avtaler.upsert(avtale5)

            val alleAvtaler = avtaler.getAll(filter = AvtaleFilter())
            alleAvtaler.first shouldBe 5

            val countAvtaler =
                avtaler.countAktiveAvtalerForTiltakstypeWithId(tiltakstype.id, LocalDate.of(2023, 3, 14))
            countAvtaler shouldBe 3
        }
    }

    context("Notifikasjoner for avtaler") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val avtaler = AvtaleRepository(database.db)

        context("Avtaler nærmer seg sluttdato") {
            test("Skal returnere avtaler som har sluttdato om 6 mnd, 3 mnd, 14 dager og 7 dager") {
                val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = TiltakstypeFixtures.Oppfolging.id)
                val avtale6Mnd = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 11, 30),
                )
                val avtale3Mnd = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 8, 31),
                )
                val avtale14Dag = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 6, 14),
                )
                val avtale7Dag = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2023, 6, 7),
                )
                val avtaleSomIkkeSkalMatche = AvtaleFixtures.avtale1.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.of(2022, 6, 7),
                    sluttDato = LocalDate.of(2024, 1, 1),
                )

                tiltakstypeRepository.upsert(tiltakstype).getOrThrow()

                avtaler.upsert(avtale6Mnd)
                avtaler.upsert(avtale3Mnd)
                avtaler.upsert(avtale14Dag)
                avtaler.upsert(avtale7Dag)
                avtaler.upsert(avtaleSomIkkeSkalMatche)

                val result = avtaler.getAllAvtalerSomNarmerSegSluttdato(
                    currentDate = LocalDate.of(2023, 5, 31),
                )
                result.size shouldBe 4
            }
        }
    }

    context("Avslutningsstatus") {
        test("endringer på avslutningsstatus påvirker avtalestatus") {
            MulighetsrommetTestDomain(
                avtale = AvtaleFixtures.avtale1.copy(
                    sluttDato = LocalDate.now().plusWeeks(1),
                ),
            ).initialize(database.db)

            val avtaler = AvtaleRepository(database.db)

            avtaler.get(AvtaleFixtures.avtale1.id).should {
                it?.avtalestatus shouldBe Avtalestatus.Aktiv
            }

            avtaler.setAvslutningsstatus(AvtaleFixtures.avtale1.id, Avslutningsstatus.AVBRUTT)
            avtaler.get(AvtaleFixtures.avtale1.id).should {
                it?.avtalestatus shouldBe Avtalestatus.Avbrutt
            }

            avtaler.setAvslutningsstatus(AvtaleFixtures.avtale1.id, Avslutningsstatus.AVSLUTTET)
            avtaler.get(AvtaleFixtures.avtale1.id).should {
                it?.avtalestatus shouldBe Avtalestatus.Avsluttet
            }
        }
    }
})
