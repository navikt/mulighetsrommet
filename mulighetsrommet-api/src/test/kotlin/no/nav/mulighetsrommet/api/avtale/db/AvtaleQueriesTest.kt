package no.nav.mulighetsrommet.api.avtale.db

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
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

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
            ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(),
        )

        test("Upsert av Arena-avtaler") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.upsertArenaAvtale(arenaAvtale)

                queries.get(arenaAvtale.id).shouldNotBeNull().should {
                    it.id shouldBe arenaAvtale.id
                    it.tiltakstype.id shouldBe arenaAvtale.tiltakstypeId
                    it.navn shouldBe arenaAvtale.navn
                    it.avtalenummer shouldBe arenaAvtale.avtalenummer
                    it.arrangor.id shouldBe ArrangorFixtures.hovedenhet.id
                    it.arrangor.organisasjonsnummer.value shouldBe arenaAvtale.arrangorOrganisasjonsnummer
                    it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = null, enhetsnummer = "9999")
                    it.startDato shouldBe arenaAvtale.startDato
                    it.sluttDato shouldBe arenaAvtale.sluttDato
                    it.avtaletype shouldBe arenaAvtale.avtaletype
                    it.status shouldBe AvtaleStatus.AKTIV
                    it.opphav shouldBe ArenaMigrering.Opphav.ARENA
                    it.prisbetingelser shouldBe "Alt er dyrt"
                }
            }
        }

        test("upsert genererer nye løpenummer") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val avtale1Id = AvtaleFixtures.oppfolging.id
                val avtale2Id = UUID.randomUUID()

                queries.upsert(AvtaleFixtures.oppfolging.copy(avtalenummer = null))
                queries.upsert(AvtaleFixtures.oppfolging.copy(id = avtale2Id, avtalenummer = null))

                val avtale1Avtalenummer = queries.get(avtale1Id).shouldNotBeNull().avtalenummer.shouldNotBeNull()
                avtale1Avtalenummer.substring(0, 4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeGreaterThanOrEqual 10_000

                val avtale2Avtalenummer = queries.get(avtale2Id).shouldNotBeNull().avtalenummer.shouldNotBeNull()
                avtale2Avtalenummer.substring(0, 4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeLessThan avtale2Avtalenummer.substring(5).toInt()
            }
        }

        test("upsert setter opphav første gang avtalen lagres") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val id1 = UUID.randomUUID()
                queries.upsertArenaAvtale(arenaAvtale.copy(id = id1))
                queries.upsert(AvtaleFixtures.oppfolging.copy(id = id1))
                queries.get(id1).shouldNotBeNull().should {
                    it.opphav shouldBe ArenaMigrering.Opphav.ARENA
                }

                val id2 = UUID.randomUUID()
                queries.upsert(AvtaleFixtures.oppfolging.copy(id = id2))
                queries.upsertArenaAvtale(arenaAvtale.copy(id = id2))
                queries.get(id2).shouldNotBeNull().should {
                    it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                }
            }
        }

        test("administrator for avtale") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val ansatt1 = NavAnsattFixture.ansatt1
                val ansatt2 = NavAnsattFixture.ansatt2

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = listOf(ansatt1.navIdent),
                )

                queries.upsert(avtale1)
                queries.get(avtale1.id)?.administratorer shouldContainExactlyInAnyOrder listOf(
                    AvtaleDto.Administrator(ansatt1.navIdent, "Donald Duck"),
                )

                queries.upsert(avtale1.copy(administratorer = listOf(ansatt1.navIdent, ansatt2.navIdent)))
                queries.get(avtale1.id)?.administratorer shouldContainExactlyInAnyOrder listOf(
                    AvtaleDto.Administrator(ansatt1.navIdent, "Donald Duck"),
                    AvtaleDto.Administrator(ansatt2.navIdent, "Dolly Duck"),
                )

                queries.upsert(avtale1.copy(administratorer = listOf()))
                queries.get(avtale1.id).shouldNotBeNull().administratorer.shouldBeEmpty()
            }
        }

        test("avtalens nav-enheter hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navEnheter = listOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    enheter = listOf(Innlandet, Gjovik, Sel),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.kontorstruktur shouldBe listOf(
                        Kontorstruktur(
                            region = Innlandet,
                            kontorer = listOf(Gjovik, Sel),
                        ),
                    )
                }
            }
        }

        test("Arrangør kontaktperson") {
            val p1 = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
                navn = "Navn Navnesen",
                telefon = "22232322",
                epost = "navn@gmail.com",
                beskrivelse = "beskrivelse",
            )
            val p2 = p1.copy(
                id = UUID.randomUUID(),
                navn = "Fredrik Navnesen",
                telefon = "32322",
            )
            val p3 = p1.copy(
                id = UUID.randomUUID(),
                navn = "Thomas Navnesen",
                telefon = "84322",
            )
            val avtale = AvtaleFixtures.oppfolging.copy(
                arrangorKontaktpersoner = listOf(p1.id),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(p1, p2, p3),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactly listOf(p1)
                }

                queries.upsert(avtale.copy(arrangorKontaktpersoner = listOf(p2.id, p3.id)))
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(p2, p3)
                }

                queries.frikobleKontaktpersonFraAvtale(p3.id, avtale.id)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(p2)
                }

                queries.upsert(avtale.copy(arrangorKontaktpersoner = emptyList()))
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldHaveSize 0
                }
            }
        }

        test("Personopplysninger") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                var avtale = AvtaleFixtures.oppfolging.copy(personopplysninger = listOf(Personopplysning.NAVN))
                queries.upsert(avtale)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger shouldContainExactly listOf(Personopplysning.NAVN)
                }

                avtale = avtale.copy(personopplysninger = listOf(Personopplysning.KJONN, Personopplysning.ADFERD))
                queries.upsert(avtale)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger shouldContainExactly listOf(Personopplysning.KJONN, Personopplysning.ADFERD)
                }

                avtale = avtale.copy(personopplysninger = emptyList())
                queries.upsert(avtale)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger shouldHaveSize 0
                }
            }
        }

        test("Underenheter blir riktig med fra spørring") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                arrangorId = ArrangorFixtures.hovedenhet.id,
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id, ArrangorFixtures.underenhet2.id),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.upsert(avtale)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
                    it.arrangor.underenheter.map { enhet -> enhet.organisasjonsnummer } shouldContainExactlyInAnyOrder listOf(
                        ArrangorFixtures.underenhet1.organisasjonsnummer,
                        ArrangorFixtures.underenhet2.organisasjonsnummer,
                    )
                }
            }
        }

        test("gruppe amo kategorier") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val amoKategorisering = AmoKategorisering.BransjeOgYrkesrettet(
                    bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.INDUSTRIARBEID,
                    forerkort = emptyList(),
                    sertifiseringer = listOf(
                        AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(
                            konseptId = 1,
                            label = "label",
                        ),
                    ),
                    innholdElementer = listOf(AmoKategorisering.InnholdElement.TEORETISK_OPPLAERING),
                )
                val avtale = AvtaleFixtures.oppfolging.copy(amoKategorisering = amoKategorisering)
                queries.upsert(avtale)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe amoKategorisering
                }

                val amoEndring = amoKategorisering.copy(
                    bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.HELSE_PLEIE_OG_OMSORG,
                    sertifiseringer = listOf(
                        AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(
                            konseptId = 2,
                            label = "label2",
                        ),
                    ),
                )
                queries.upsert(avtale.copy(amoKategorisering = amoEndring))
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe amoEndring
                }
                queries.upsert(avtale.copy(amoKategorisering = null))
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe null
                }
            }
        }
    }

    context("Filter for avtaler") {
        val oppfolgingDomain = MulighetsrommetTestDomain(
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(),
        )

        test("getAvtaleIdsByAdministrator") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val queries = AvtaleQueries(session)

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
                )

                queries.upsert(avtale1)

                queries.getAvtaleIdsByAdministrator(NavAnsattFixture.ansatt1.navIdent) shouldBe listOf(avtale1.id)
            }
        }

        test("fritekstsøk på avtalenavn og avtalenummer") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val queries = AvtaleQueries(session)

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                    avtalenummer = "2024#1000",
                )
                val avtale2 = avtale1.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om undervisning av underlige ulver",
                    avtalenummer = "2024#2000",
                )
                queries.upsert(avtale1)
                queries.upsert(avtale2)

                queries.getAll(search = "krokodillen").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale1.id
                }

                queries.getAll(search = "avtale").should {
                    it.totalCount shouldBe 2
                }

                queries.getAll(search = "avtale ulv").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.getAll(search = "krok").should {
                    it.totalCount shouldBe 1
                }

                queries.getAll(search = "avtale kråke").should {
                    it.totalCount shouldBe 0
                }

                queries.getAll(search = "2000").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.getAll(search = "2024").should {
                    it.totalCount shouldBe 2
                }
            }
        }

        test("administrator") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val queries = AvtaleQueries(session)

                val a1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
                )
                val a2 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
                )

                queries.upsert(a1)
                queries.upsert(a2)

                queries.getAll(administratorNavIdent = NavAnsattFixture.ansatt1.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a1.id, a2.id)
                }

                queries.getAll(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a2.id)
                }
            }
        }

        test("filtrering på Avtalestatus") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val queries = AvtaleQueries(session)

                val avtaleAktiv = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                )
                queries.upsert(avtaleAktiv)

                val avtaleAvsluttet = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.now().minusDays(1),
                )
                queries.upsert(avtaleAvsluttet)

                val avtaleAvbrutt = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                )
                queries.upsert(avtaleAvbrutt)
                queries.avbryt(avtaleAvbrutt.id, LocalDateTime.now(), AvbruttAarsak.Feilregistrering)

                val avtalePlanlagt = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now().plusDays(1),
                )
                queries.upsert(avtalePlanlagt)

                forAll(
                    row(listOf(AvtaleStatus.Enum.AKTIV), listOf(avtaleAktiv.id, avtalePlanlagt.id)),
                    row(listOf(AvtaleStatus.Enum.AVBRUTT), listOf(avtaleAvbrutt.id)),
                    row(listOf(AvtaleStatus.Enum.AVSLUTTET), listOf(avtaleAvsluttet.id)),
                    row(
                        listOf(AvtaleStatus.Enum.AVBRUTT, AvtaleStatus.Enum.AVSLUTTET),
                        listOf(avtaleAvbrutt.id, avtaleAvsluttet.id),
                    ),
                ) { statuser, expected ->
                    val result = queries.getAll(statuser = statuser)
                    result.items shouldContainExactlyIds expected
                }
            }
        }

        test("filtrering på ansvarlig enhet i Arena") {
            val domain = MulighetsrommetTestDomain(
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
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${AvtaleFixtures.oppfolging.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0400' where id = '${AvtaleFixtures.AFT.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0502' where id = '${AvtaleFixtures.VTA.id}'"))

                queries.getAll(navRegioner = listOf("0300")).should { (totalCount) ->
                    totalCount shouldBe 1
                }
                queries.getAll(navRegioner = listOf("0400")).should { (totalCount) ->
                    totalCount shouldBe 2
                }
                queries.getAll(navRegioner = listOf("0502")).should { (totalCount) ->
                    totalCount shouldBe 1
                }
            }
        }

        test("filtrering på Nav-enheter") {
            val domain = MulighetsrommetTestDomain(
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
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(
                    navRegioner = listOf(Innlandet.enhetsnummer),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }

                queries.getAll(
                    navRegioner = listOf(Gjovik.enhetsnummer, Sel.enhetsnummer),
                ).should { (totalCount, items) ->
                    totalCount shouldBe 2
                    items shouldContainExactlyIds listOf(AvtaleFixtures.oppfolging.id, AvtaleFixtures.AFT.id)
                }
            }
        }

        test("Filtrer på avtaletyper returnerer riktige avtaler") {
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

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeAmo),
                avtaler = listOf(avtale1, avtale2, avtale3),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(avtaletyper = listOf(Avtaletype.Avtale)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(avtale1.id)
                }

                queries.getAll(avtaletyper = listOf(Avtaletype.Avtale, Avtaletype.OffentligOffentlig)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(avtale1.id, avtale3.id)
                }

                queries.getAll(avtaletyper = listOf()).should {
                    it.totalCount shouldBe 3
                }
            }
        }

        test("Filtrer på tiltakstypeId returnerer avtaler tilknyttet spesifikk tiltakstype") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.AFT),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging,
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID()),
                    AvtaleFixtures.AFT,
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(
                    tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id),
                ).should { (totalCount, items) ->
                    totalCount shouldBe 2
                    items shouldContainExactlyIds listOf(domain.avtaler[0].id, domain.avtaler[1].id)
                }

                queries.getAll(
                    tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id, TiltakstypeFixtures.AFT.id),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }
            }
        }

        test("Filtrering på tiltaksarrangørs navn gir treff") {
            val annenArrangor = ArrangorFixtures.underenhet1.copy(
                id = UUID.randomUUID(),
                navn = "Annen Arrangør AS",
                organisasjonsnummer = Organisasjonsnummer("667543265"),
            )

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.AFT),
                arrangorer = listOf(
                    ArrangorFixtures.hovedenhet.copy(navn = "Hovedenhet AS"),
                    ArrangorFixtures.underenhet1,
                    annenArrangor,
                ),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(arrangorId = ArrangorFixtures.hovedenhet.id),
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID(), arrangorId = ArrangorFixtures.hovedenhet.id),
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID(), arrangorId = annenArrangor.id),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(search = "enhet").totalCount shouldBe 2
                queries.getAll(search = "annen").totalCount shouldBe 1
            }
        }

        test("Filtrering på personvern_bekreftet") {
            val domain = MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(personvernBekreftet = true),
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID(), personvernBekreftet = true),
                    AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID(), personvernBekreftet = false),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(personvernBekreftet = true).totalCount shouldBe 2
                queries.getAll(personvernBekreftet = false).totalCount shouldBe 1
                queries.getAll(personvernBekreftet = null).totalCount shouldBe 3
            }
        }
    }

    context("Sortering") {
        val arrangorA = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "alvdal",
            organisasjonsnummer = Organisasjonsnummer("987654321"),
        )
        val arrangorB = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "bjarne",
            organisasjonsnummer = Organisasjonsnummer("123456789"),
        )
        val arrangorC = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "chris",
            organisasjonsnummer = Organisasjonsnummer("999888777"),
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

        test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val result = queries.getAll(sortering = "navn-ascending")

                result.totalCount shouldBe 5
                result.items[0].navn shouldBe "Avtale hos Anders"
                result.items[1].navn shouldBe "Avtale hos Kjetil"
                result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[3].navn shouldBe "Avtale hos Øyvind"
                result.items[4].navn shouldBe "Avtale hos Åse"
            }
        }

        test("Sortering på navn fra å-a sorterer korrekt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val result = queries.getAll(sortering = "navn-descending")

                result.totalCount shouldBe 5
                result.items[0].navn shouldBe "Avtale hos Åse"
                result.items[1].navn shouldBe "Avtale hos Øyvind"
                result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[3].navn shouldBe "Avtale hos Kjetil"
                result.items[4].navn shouldBe "Avtale hos Anders"
            }
        }

        test("Sortering på arrangør sorterer korrekt") {
            val alvdal = AvtaleDto.ArrangorHovedenhet(
                id = arrangorA.id,
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                navn = "alvdal",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val bjarne = AvtaleDto.ArrangorHovedenhet(
                id = arrangorB.id,
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "bjarne",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val chris = AvtaleDto.ArrangorHovedenhet(
                id = arrangorC.id,
                organisasjonsnummer = Organisasjonsnummer("999888777"),
                navn = "chris",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val ascending = queries.getAll(sortering = "arrangor-ascending")
                ascending.items[0].arrangor shouldBe alvdal
                ascending.items[1].arrangor shouldBe bjarne
                ascending.items[2].arrangor shouldBe bjarne
                ascending.items[3].arrangor shouldBe bjarne
                ascending.items[4].arrangor shouldBe chris

                val descending = queries.getAll(sortering = "arrangor-descending")
                descending.items[0].arrangor shouldBe chris
                descending.items[1].arrangor shouldBe bjarne
                descending.items[2].arrangor shouldBe bjarne
                descending.items[3].arrangor shouldBe bjarne
                descending.items[4].arrangor shouldBe alvdal
            }
        }

        test("Sortering på sluttdato fra a-å sorterer korrekt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val result = queries.getAll(sortering = "sluttdato-descending")
                result.items[0].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[1].navn shouldBe "Avtale hos Kjetil"
                result.items[2].navn shouldBe "Avtale hos Anders"
                result.items[3].navn shouldBe "Avtale hos Øyvind"
                result.items[4].navn shouldBe "Avtale hos Åse"
            }
        }

        test("Sortering på sluttdato fra å-a sorterer korrekt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val result = queries.getAll(sortering = "sluttdato-ascending")
                result.items[0].navn shouldBe "Avtale hos Åse"
                result.items[1].navn shouldBe "Avtale hos Øyvind"
                result.items[2].navn shouldBe "Avtale hos Anders"
                result.items[3].navn shouldBe "Avtale hos Kjetil"
                result.items[4].navn shouldBe "Avtale hos Ærfuglen Ærle"
            }
        }
    }

    context("Status på avtale") {
        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(),
        )

        val dagensDato = LocalDate.now()
        val enManedFrem = dagensDato.plusMonths(1)
        val enManedTilbake = dagensDato.minusMonths(1)
        val toManederFrem = dagensDato.plusMonths(2)
        val toManederTilbake = dagensDato.minusMonths(1)

        test("status utleds fra avtalens datoer") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                forAll(
                    row(dagensDato, enManedFrem, AvtaleStatus.AKTIV),
                    row(enManedFrem, toManederFrem, AvtaleStatus.AKTIV),
                    row(enManedTilbake, dagensDato, AvtaleStatus.AKTIV),
                    row(toManederTilbake, enManedTilbake, AvtaleStatus.AVSLUTTET),
                ) { startDato, sluttDato, expectedStatus ->
                    queries.upsert(AvtaleFixtures.oppfolging.copy(startDato = startDato, sluttDato = sluttDato))

                    queries.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().status shouldBe expectedStatus
                }
            }
        }

        test("avbrutt-tidspunkt påvirker avtalestatus") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                forAll(
                    row(dagensDato, enManedFrem, dagensDato, AvtaleStatus.Enum.AVBRUTT),
                    row(enManedFrem, toManederFrem, dagensDato, AvtaleStatus.Enum.AVBRUTT),
                    row(toManederTilbake, enManedTilbake, dagensDato, AvtaleStatus.Enum.AVBRUTT),
                    row(enManedTilbake, enManedFrem, enManedFrem.plusDays(1), AvtaleStatus.Enum.AVBRUTT),
                ) { startDato, sluttDato, avbruttDato, expectedStatus ->
                    queries.upsert(AvtaleFixtures.oppfolging.copy(startDato = startDato, sluttDato = sluttDato))

                    queries.avbryt(
                        AvtaleFixtures.oppfolging.id,
                        avbruttDato.atStartOfDay(),
                        AvbruttAarsak.Annet("Min årsak"),
                    )

                    queries.get(AvtaleFixtures.oppfolging.id)
                        .shouldNotBeNull().status.enum shouldBe expectedStatus
                }
            }
        }
    }
})

private infix fun Collection<AvtaleDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
