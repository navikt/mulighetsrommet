package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.Kontorstruktur
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Personopplysning
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("CRUD") {
        val arenaAvtale: ArenaAvtaleDbo = AvtaleFixtures.oppfolging.run {
            ArenaAvtaleDbo(
                id = id,
                navn = navn,
                tiltakstypeId = tiltakstypeId,
                avtalenummer = avtalenummer,
                arrangorOrganisasjonsnummer = "123456789",
                startDato = startDato,
                sluttDato = sluttDato,
                arenaAnsvarligEnhet = "9999",
                avtaletype = avtaletype,
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                prisbetingelser = prisbetingelser,
            )
        }

        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("Upsert av Arena-avtaler") {
            val avtaler = AvtaleRepository(database.db)

            avtaler.upsertArenaAvtale(arenaAvtale)

            avtaler.get(arenaAvtale.id).shouldNotBeNull().should {
                it.id shouldBe arenaAvtale.id
                it.tiltakstype.id shouldBe arenaAvtale.tiltakstypeId
                it.navn shouldBe arenaAvtale.navn
                it.avtalenummer shouldBe arenaAvtale.avtalenummer
                it.arrangor.id shouldBe ArrangorFixtures.hovedenhet.id
                it.arrangor.organisasjonsnummer shouldBe arenaAvtale.arrangorOrganisasjonsnummer
                it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = null, enhetsnummer = "9999")
                it.startDato shouldBe arenaAvtale.startDato
                it.sluttDato shouldBe arenaAvtale.sluttDato
                it.avtaletype shouldBe arenaAvtale.avtaletype
                it.avtalestatus shouldBe Avtalestatus.AKTIV
                it.opphav shouldBe ArenaMigrering.Opphav.ARENA
                it.prisbetingelser shouldBe "Alt er dyrt"
            }
        }

        test("upsert genererer nye løpenummer") {
            val avtaler = AvtaleRepository(database.db)

            val avtale1Id = AvtaleFixtures.oppfolging.id
            val avtale2Id = UUID.randomUUID()

            avtaler.upsert(AvtaleFixtures.oppfolging)
            avtaler.upsert(AvtaleFixtures.oppfolging.copy(id = avtale2Id))

            val get = avtaler.get(avtale1Id)
            val avtale1Lopenummer = get.shouldNotBeNull().lopenummer.shouldNotBeNull()
            val avtale2Lopenummer = avtaler.get(avtale2Id).shouldNotBeNull().lopenummer.shouldNotBeNull()

            avtale1Lopenummer.getParts().first shouldBe LocalDate.now().year
            avtale2Lopenummer.getParts().first shouldBe LocalDate.now().year
            avtale1Lopenummer.getParts().second shouldBeGreaterThanOrEqual 10_000
            avtale1Lopenummer.getParts().second shouldBeLessThan avtale2Lopenummer.getParts().second
        }

        test("upsert setter opphav til MR_ADMIN_FLATE") {
            val avtaler = AvtaleRepository(database.db)

            avtaler.upsert(AvtaleFixtures.oppfolging)

            avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().should {
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
            }
        }

        test("upsert endrer ikke opphav om det allerede er satt") {
            val avtaler = AvtaleRepository(database.db)

            val id1 = UUID.randomUUID()
            avtaler.upsertArenaAvtale(arenaAvtale.copy(id = id1))
            avtaler.upsert(AvtaleFixtures.oppfolging.copy(id = id1))
            avtaler.get(id1).shouldNotBeNull().should {
                it.opphav shouldBe ArenaMigrering.Opphav.ARENA
            }

            val id2 = UUID.randomUUID()
            avtaler.upsert(AvtaleFixtures.oppfolging.copy(id = id2))
            avtaler.upsertArenaAvtale(arenaAvtale.copy(id = id2))
            avtaler.get(id2).shouldNotBeNull().should {
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
            }
        }

        test("administrator for avtale") {
            val avtaler = AvtaleRepository(database.db)

            val ansatt1 = NavAnsattFixture.ansatt1
            val ansatt2 = NavAnsattFixture.ansatt2
            val avtale1 = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(ansatt1.navIdent),
            )

            avtaler.upsert(avtale1)
            avtaler.get(avtale1.id)?.administratorer shouldContainExactlyInAnyOrder listOf(
                AvtaleAdminDto.Administrator(ansatt1.navIdent, "Donald Duck"),
            )

            avtaler.upsert(avtale1.copy(administratorer = listOf(ansatt1.navIdent, ansatt2.navIdent)))
            avtaler.get(avtale1.id)?.administratorer shouldContainExactlyInAnyOrder listOf(
                AvtaleAdminDto.Administrator(ansatt1.navIdent, "Donald Duck"),
                AvtaleAdminDto.Administrator(ansatt2.navIdent, "Dolly Duck"),
            )

            avtaler.upsert(avtale1.copy(administratorer = listOf()))
            avtaler.get(avtale1.id).shouldNotBeNull().administratorer.shouldBeEmpty()
        }

        test("Arrangør kontaktperson") {
            val arrangorRepository = ArrangorRepository(database.db)
            val arrangorKontaktperson = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
                navn = "Navn Navnesen",
                telefon = "22232322",
                epost = "navn@gmail.com",
                beskrivelse = "beskrivelse",
            )
            arrangorRepository.upsertKontaktperson(arrangorKontaktperson)

            val avtaler = AvtaleRepository(database.db)
            var avtale = AvtaleFixtures.oppfolging.copy(
                arrangorKontaktpersoner = listOf(arrangorKontaktperson.id),
            )
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner shouldContainExactly listOf(arrangorKontaktperson)
            }

            // Endre kontaktperson
            val nyPerson = arrangorKontaktperson.copy(
                id = UUID.randomUUID(),
                navn = "Fredrik Navnesen",
                telefon = "32322",
            )
            val nyPerson2 = arrangorKontaktperson.copy(
                id = UUID.randomUUID(),
                navn = "Thomas Navnesen",
                telefon = "84322",
            )
            arrangorRepository.upsertKontaktperson(nyPerson)
            arrangorRepository.upsertKontaktperson(nyPerson2)

            avtale = avtale.copy(arrangorKontaktpersoner = listOf(nyPerson.id, nyPerson2.id))
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(nyPerson, nyPerson2)
            }

            // Fjern kontaktperson
            avtale = avtale.copy(arrangorKontaktpersoner = emptyList())
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner shouldHaveSize 0
            }
        }

        test("Personopplysninger") {
            val avtaler = AvtaleRepository(database.db)
            var avtale = AvtaleFixtures.oppfolging.copy(personopplysninger = listOf(Personopplysning.NAVN))
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.personopplysninger shouldContainExactly listOf(Personopplysning.NAVN)
            }

            avtale = avtale.copy(personopplysninger = listOf(Personopplysning.KJONN, Personopplysning.ADFERD))
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.personopplysninger shouldContainExactly listOf(Personopplysning.KJONN, Personopplysning.ADFERD)
            }

            avtale = avtale.copy(personopplysninger = emptyList())
            avtaler.upsert(avtale)
            avtaler.get(avtale.id).shouldNotBeNull().should {
                it.personopplysninger shouldHaveSize 0
            }
        }

        test("Underenheter blir riktig med fra spørring") {
            val avtaler = AvtaleRepository(database.db)

            val avtale1 = AvtaleFixtures.oppfolging.copy(
                arrangorId = ArrangorFixtures.hovedenhet.id,
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id, ArrangorFixtures.underenhet2.id),
            )

            avtaler.upsert(avtale1)
            avtaler.get(avtale1.id).shouldNotBeNull().should {
                it.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
                it.arrangor.underenheter.map { it.organisasjonsnummer } shouldContainExactlyInAnyOrder listOf(
                    ArrangorFixtures.underenhet1.organisasjonsnummer,
                    ArrangorFixtures.underenhet2.organisasjonsnummer,
                )
            }
        }
    }

    context("Filter for avtaler") {
        val avtaler = AvtaleRepository(database.db)

        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        context("Avtalenavn") {
            test("Filtrere på avtalenavn skal returnere avtaler som matcher søket") {
                val avtale1 = AvtaleFixtures.oppfolging.copy(
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
                    tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id),
                    search = "Kroko",
                )

                result.totalCount shouldBe 1
                result.items[0].navn shouldBe "Avtale om opplæring av blinde krokodiller"
            }
        }

        test("administrator") {
            val a1 = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            val a2 = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
            )

            avtaler.upsert(a1)
            avtaler.upsert(a2)

            avtaler.getAll(administratorNavIdent = NavAnsattFixture.ansatt1.navIdent).should {
                it.items.map { tg -> tg.id } shouldContainExactlyInAnyOrder listOf(a1.id, a2.id)
            }

            avtaler.getAll(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent).should {
                it.items.map { tg -> tg.id } shouldContainExactlyInAnyOrder listOf(a2.id)
            }
        }

        context("Avtalestatus") {
            test("filtrering på Avtalestatus") {
                val avtaleAktiv = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                )
                avtaler.upsert(avtaleAktiv)

                val avtaleAvsluttet = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.now().minusDays(1),
                )
                avtaler.upsert(avtaleAvsluttet)

                val avtaleAvbrutt = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                )
                avtaler.upsert(avtaleAvbrutt)
                avtaler.setAvbruttTidspunkt(avtaleAvbrutt.id, LocalDateTime.now())

                val avtalePlanlagt = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now().plusDays(1),
                )
                avtaler.upsert(avtalePlanlagt)

                forAll(
                    row(listOf(Avtalestatus.AKTIV), listOf(avtaleAktiv.id, avtalePlanlagt.id)),
                    row(listOf(Avtalestatus.AVBRUTT), listOf(avtaleAvbrutt.id)),
                    row(listOf(Avtalestatus.AVSLUTTET), listOf(avtaleAvsluttet.id)),
                    row(
                        listOf(Avtalestatus.AVBRUTT, Avtalestatus.AVSLUTTET),
                        listOf(avtaleAvbrutt.id, avtaleAvsluttet.id),
                    ),
                ) { statuser, expected ->
                    val result = avtaler.getAll(statuser = statuser)
                    result.items.map { it.id } shouldContainExactlyInAnyOrder expected
                }
            }
        }

        context("NavEnhet") {
            test("filtrering på ansvarlig enhet i Arena") {
                MulighetsrommetTestDomain(
                    enheter = listOf(Oslo, Innlandet, Gjovik),
                    tiltakstyper = listOf(
                        TiltakstypeFixtures.Oppfolging,
                        TiltakstypeFixtures.AFT,
                        TiltakstypeFixtures.VTA,
                    ),
                    avtaler = listOf(
                        AvtaleFixtures.oppfolging.copy(navEnheter = listOf()),
                        AvtaleFixtures.AFT.copy(navEnheter = listOf()),
                        AvtaleFixtures.VTA.copy(navEnheter = listOf()),
                    ),
                ).initialize(database.db)

                Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${AvtaleFixtures.oppfolging.id}'")
                    .asUpdate.let { database.db.run(it) }
                Query("update avtale set arena_ansvarlig_enhet = '0400' where id = '${AvtaleFixtures.AFT.id}'")
                    .asUpdate.let { database.db.run(it) }
                Query("update avtale set arena_ansvarlig_enhet = '0502' where id = '${AvtaleFixtures.VTA.id}'")
                    .asUpdate.let { database.db.run(it) }

                avtaler.getAll(navRegioner = listOf("0300")).should { (totalCount) ->
                    totalCount shouldBe 1
                }
                avtaler.getAll(navRegioner = listOf("0400")).should { (totalCount) ->
                    totalCount shouldBe 2
                }
                avtaler.getAll(navRegioner = listOf("0502")).should { (totalCount) ->
                    totalCount shouldBe 1
                }
            }

            test("filtrering på NAV-enheter") {
                MulighetsrommetTestDomain(
                    enheter = listOf(Innlandet, Gjovik, Sel),
                    tiltakstyper = listOf(
                        TiltakstypeFixtures.Oppfolging,
                        TiltakstypeFixtures.AFT,
                        TiltakstypeFixtures.VTA,
                    ),
                    avtaler = listOf(
                        AvtaleFixtures.oppfolging.copy(
                            navEnheter = listOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                        ),
                        AvtaleFixtures.AFT.copy(
                            navEnheter = listOf(Innlandet.enhetsnummer, Sel.enhetsnummer),
                        ),
                        AvtaleFixtures.VTA.copy(navEnheter = listOf(Innlandet.enhetsnummer)),
                    ),
                ).initialize(database.db)

                avtaler.getAll(
                    navRegioner = listOf(Innlandet.enhetsnummer),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }

                avtaler.getAll(
                    navRegioner = listOf(Gjovik.enhetsnummer, Sel.enhetsnummer),
                ).should { (totalCount) ->
                    totalCount shouldBe 2
                }
            }

            test("Avtale NAV-enhet blir med riktig tilbake") {
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

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navEnheter = listOf("1900", "1901"),
                )
                avtaler.upsert(avtale1)
                avtaler.get(avtale1.id).should {
                    it!!.kontorstruktur shouldBe listOf(
                        Kontorstruktur(
                            region = NavEnhetDbo(
                                enhetsnummer = "1900",
                                navn = "Oppland",
                                type = Norg2Type.FYLKE,
                                overordnetEnhet = null,
                                status = NavEnhetStatus.AKTIV,
                            ),
                            kontorer = listOf(
                                NavEnhetDbo(
                                    enhetsnummer = "1901",
                                    navn = "Oppland 1",
                                    type = Norg2Type.LOKAL,
                                    overordnetEnhet = "1900",
                                    status = NavEnhetStatus.AKTIV,
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        test("Filtrer på avtaletyper returnerer riktige avtaler") {
            TiltakstypeRepository(database.db).upsert(TiltakstypeFixtures.GRUPPE_AMO)

            val avtale1 = AvtaleFixtures.gruppeAmo.copy(
                id = UUID.randomUUID(),
                avtaletype = Avtaletype.Avtale,
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                avtaletype = Avtaletype.Rammeavtale,
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                avtaletype = Avtaletype.OffentligOffentlig,
            )
            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)

            var result = avtaler.getAll(
                avtaletyper = listOf(Avtaletype.Avtale),
            )
            result.totalCount shouldBe 1
            result.items[0].id shouldBe avtale1.id

            result = avtaler.getAll(
                avtaletyper = listOf(Avtaletype.Avtale, Avtaletype.OffentligOffentlig),
            )
            result.totalCount shouldBe 2
            result.items.map { it.id } shouldContainExactlyInAnyOrder listOf(avtale1.id, avtale3.id)

            result = avtaler.getAll(
                avtaletyper = listOf(),
            )
            result.totalCount shouldBe 3
        }

        test("Filtrer på tiltakstypeId returnerer avtaler tilknyttet spesifikk tiltakstype") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.AFT),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging,
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID()),
                    AvtaleFixtures.AFT,
                ),
            ).initialize(database.db)

            avtaler.getAll(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id))
                .should { (totalCount, avtaler) ->
                    totalCount shouldBe 2
                    avtaler[0].tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
                    avtaler[1].tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
                }

            avtaler.getAll(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id, TiltakstypeFixtures.AFT.id))
                .should { (totalCount) ->
                    totalCount shouldBe 3
                }
        }

        test("Filtrering på tiltaksarrangørs navn gir treff") {
            val arrangorer = ArrangorRepository(database.db)
            val annenArrangor = ArrangorFixtures.underenhet1.copy(
                id = UUID.randomUUID(),
                navn = "Annen Arrangør AS",
                organisasjonsnummer = "667543265",
            )
            arrangorer.upsert(ArrangorFixtures.hovedenhet.copy(navn = "Hovedenhet AS"))
            arrangorer.upsert(annenArrangor)
            val avtale1 = AvtaleFixtures.oppfolging.copy(
                arrangorId = ArrangorFixtures.hovedenhet.id,
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                arrangorId = annenArrangor.id,
            )

            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)
            val result = avtaler.getAll(
                search = "enhet",
            )

            result.totalCount shouldBe 2
        }

        test("Filtrering på personvern_bekreftet") {
            val avtale1 = AvtaleFixtures.oppfolging.copy(
                personvernBekreftet = true,
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                personvernBekreftet = true,
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                personvernBekreftet = false,
            )

            avtaler.upsert(avtale1)
            avtaler.upsert(avtale2)
            avtaler.upsert(avtale3)

            var result = avtaler.getAll(personvernBekreftet = true)
            result.totalCount shouldBe 2

            result = avtaler.getAll(personvernBekreftet = false)
            result.totalCount shouldBe 1

            result = avtaler.getAll(personvernBekreftet = null)
            result.totalCount shouldBe 3
        }
    }

    context("Sortering") {
        val avtaler = AvtaleRepository(database.db)

        val arrangorA = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "alvdal",
            organisasjonsnummer = "987654321",
            postnummer = null,
            poststed = null,
        )
        val arrangorB = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "bjarne",
            organisasjonsnummer = "123456789",
            postnummer = null,
            poststed = null,
        )
        val arrangorC = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "chris",
            organisasjonsnummer = "999888777",
            postnummer = null,
            poststed = null,
        )
        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(arrangorA, arrangorB, arrangorC),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.Jobbklubb),
            avtaler = listOf(
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Anders",
                    arrangorId = arrangorB.id,
                    arrangorUnderenheter = emptyList(),
                    sluttDato = LocalDate.of(2010, 1, 31),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Åse",
                    arrangorId = arrangorA.id,
                    arrangorUnderenheter = emptyList(),
                    sluttDato = LocalDate.of(2009, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Øyvind",
                    arrangorId = arrangorB.id,
                    arrangorUnderenheter = emptyList(),
                    sluttDato = LocalDate.of(2010, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Kjetil",
                    arrangorId = arrangorC.id,
                    arrangorUnderenheter = emptyList(),
                    sluttDato = LocalDate.of(2011, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Ærfuglen Ærle",
                    arrangorId = arrangorB.id,
                    arrangorUnderenheter = emptyList(),
                    sluttDato = LocalDate.of(2023, 1, 1),
                ),
            ),
        )
        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()
        }

        test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
            val result = avtaler.getAll(sortering = "navn-ascending")

            result.totalCount shouldBe 5
            result.items[0].navn shouldBe "Avtale hos Anders"
            result.items[1].navn shouldBe "Avtale hos Kjetil"
            result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
            result.items[3].navn shouldBe "Avtale hos Øyvind"
            result.items[4].navn shouldBe "Avtale hos Åse"
        }

        test("Sortering på navn fra å-a sorterer korrekt") {
            val result = avtaler.getAll(sortering = "navn-descending")

            result.totalCount shouldBe 5
            result.items[0].navn shouldBe "Avtale hos Åse"
            result.items[1].navn shouldBe "Avtale hos Øyvind"
            result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
            result.items[3].navn shouldBe "Avtale hos Kjetil"
            result.items[4].navn shouldBe "Avtale hos Anders"
        }

        test("Sortering på arrangør sorterer korrekt") {
            val alvdal = AvtaleAdminDto.ArrangorHovedenhet(
                id = arrangorA.id,
                organisasjonsnummer = "987654321",
                navn = "alvdal",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val bjarne = AvtaleAdminDto.ArrangorHovedenhet(
                id = arrangorB.id,
                organisasjonsnummer = "123456789",
                navn = "bjarne",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val chris = AvtaleAdminDto.ArrangorHovedenhet(
                id = arrangorC.id,
                organisasjonsnummer = "999888777",
                navn = "chris",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )

            val ascending = avtaler.getAll(sortering = "arrangor-ascending")

            ascending.items[0].arrangor shouldBe alvdal
            ascending.items[1].arrangor shouldBe bjarne
            ascending.items[2].arrangor shouldBe bjarne
            ascending.items[3].arrangor shouldBe bjarne
            ascending.items[4].arrangor shouldBe chris

            val descending = avtaler.getAll(sortering = "arrangor-descending")

            descending.items[0].arrangor shouldBe chris
            descending.items[1].arrangor shouldBe bjarne
            descending.items[2].arrangor shouldBe bjarne
            descending.items[3].arrangor shouldBe bjarne
            descending.items[4].arrangor shouldBe alvdal
        }

        test("Sortering på sluttdato fra a-å sorterer korrekt") {
            val result = avtaler.getAll(sortering = "sluttdato-descending")

            result.items[0].sluttDato shouldBe LocalDate.of(2023, 1, 1)
            result.items[0].navn shouldBe "Avtale hos Ærfuglen Ærle"
            result.items[1].sluttDato shouldBe LocalDate.of(2011, 1, 1)
            result.items[1].navn shouldBe "Avtale hos Kjetil"
            result.items[2].sluttDato shouldBe LocalDate.of(2010, 1, 31)
            result.items[2].navn shouldBe "Avtale hos Anders"
            result.items[3].sluttDato shouldBe LocalDate.of(2010, 1, 1)
            result.items[3].navn shouldBe "Avtale hos Øyvind"
            result.items[4].sluttDato shouldBe LocalDate.of(2009, 1, 1)
            result.items[4].navn shouldBe "Avtale hos Åse"
        }

        test("Sortering på sluttdato fra å-a sorterer korrekt") {
            val result = avtaler.getAll(sortering = "sluttdato-ascending")

            result.items[0].sluttDato shouldBe LocalDate.of(2009, 1, 1)
            result.items[0].navn shouldBe "Avtale hos Åse"
            result.items[1].sluttDato shouldBe LocalDate.of(2010, 1, 1)
            result.items[1].navn shouldBe "Avtale hos Øyvind"
            result.items[2].sluttDato shouldBe LocalDate.of(2010, 1, 31)
            result.items[2].navn shouldBe "Avtale hos Anders"
            result.items[3].sluttDato shouldBe LocalDate.of(2011, 1, 1)
            result.items[3].navn shouldBe "Avtale hos Kjetil"
            result.items[4].sluttDato shouldBe LocalDate.of(2023, 1, 1)
            result.items[4].navn shouldBe "Avtale hos Ærfuglen Ærle"
        }
    }

    context("Notifikasjoner for avtaler") {
        context("Avtaler nærmer seg sluttdato") {
            val avtale6Mnd = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 11, 30),
            )
            val avtale3Mnd = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 8, 31),
            )
            val avtale14Dag = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 6, 14),
            )
            val avtale7Dag = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 6, 7),
            )
            val avtaleSomIkkeSkalMatche = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2022, 6, 7),
                sluttDato = LocalDate.of(2024, 1, 1),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(avtale6Mnd, avtale3Mnd, avtale14Dag, avtale7Dag, avtaleSomIkkeSkalMatche),
            )

            domain.initialize(database.db)

            val avtaler = AvtaleRepository(database.db)

            test("Skal returnere avtaler som har sluttdato om 6 mnd, 3 mnd, 14 dager og 7 dager") {

                val result = avtaler.getAllAvtalerSomNarmerSegSluttdato(
                    currentDate = LocalDate.of(2023, 5, 31),
                )

                result.map { it.id } shouldContainExactlyInAnyOrder listOf(
                    avtale6Mnd.id,
                    avtale3Mnd.id,
                    avtale14Dag.id,
                    avtale7Dag.id,
                )
            }
        }
    }

    context("Avslutningsstatus") {
        val avtale = AvtaleFixtures.oppfolging.copy(
            sluttDato = LocalDate.now().plusWeeks(1),
        )

        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(avtale),
        )

        domain.initialize(database.db)

        val avtaler = AvtaleRepository(database.db)

        test("set avbrutt_tidspunkt påvirker avtalestatus") {
            avtaler.get(AvtaleFixtures.oppfolging.id).should {
                it?.avtalestatus shouldBe Avtalestatus.AKTIV
            }

            avtaler.setAvbruttTidspunkt(avtale.id, LocalDateTime.now())
            avtaler.get(AvtaleFixtures.oppfolging.id).should {
                it?.avtalestatus shouldBe Avtalestatus.AVBRUTT
            }
        }
    }

    context("Frikoble kontaktperson fra arrangør") {
        val domain = MulighetsrommetTestDomain()
        val avtaler = AvtaleRepository(database.db)

        beforeEach {
            domain.initialize(database.db)
        }

        test("Skal fjerne kontaktperson fra koblingstabell") {
            avtaler.upsert(AvtaleFixtures.oppfolging)
            avtaler.upsert(AvtaleFixtures.VTA)

            val arrangorKontaktperson = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.underenhet1.id,
                navn = "Aran Goran",
                telefon = "",
                epost = "test@test.no",
                beskrivelse = "",
            )

            val arrangorKontaktperson2 = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.underenhet1.id,
                navn = "Gibli Bobli",
                telefon = "",
                epost = "test@test.no",
                beskrivelse = "",
            )

            @Language("PostgreSQL")
            val upsertKontaktpersonerQuery = """
                insert into arrangor_kontaktperson(id, navn, telefon, epost, beskrivelse, arrangor_id) values
                ('${arrangorKontaktperson.id}', '${arrangorKontaktperson.navn}', '${arrangorKontaktperson.telefon}', '${arrangorKontaktperson.epost}', '${arrangorKontaktperson.beskrivelse}', '${arrangorKontaktperson.arrangorId}'),
                ('${arrangorKontaktperson2.id}', '${arrangorKontaktperson2.navn}', '${arrangorKontaktperson2.telefon}', '${arrangorKontaktperson2.epost}', '${arrangorKontaktperson2.beskrivelse}', '${arrangorKontaktperson2.arrangorId}')
            """.trimIndent()
            queryOf(upsertKontaktpersonerQuery).asExecute.let { database.db.run(it) }

            @Language("PostgreSQL")
            val upsertQuery = """
             insert into avtale_arrangor_kontaktperson(arrangor_kontaktperson_id, avtale_id) values
             ('${arrangorKontaktperson.id}', '${AvtaleFixtures.oppfolging.id}'),
             ('${arrangorKontaktperson2.id}', '${AvtaleFixtures.VTA.id}')
            """.trimIndent()
            queryOf(upsertQuery).asExecute.let { database.db.run(it) }

            @Language("PostgreSQL")
            val selectQuery = """
                select arrangor_kontaktperson_id from avtale_arrangor_kontaktperson
            """.trimIndent()

            val results =
                queryOf(selectQuery).map { it.uuid("arrangor_kontaktperson_id") }.asList.let { database.db.run(it) }
            results.size shouldBe 2
            database.db.transaction { tx ->
                avtaler.frikobleKontaktpersonFraAvtale(arrangorKontaktperson.id, AvtaleFixtures.oppfolging.id, tx)
            }

            val resultsAfterFrikobling =
                queryOf(selectQuery).map { it.uuid("arrangor_kontaktperson_id") }.asList.let { database.db.run(it) }
            resultsAfterFrikobling.size shouldBe 1
        }
    }
})
