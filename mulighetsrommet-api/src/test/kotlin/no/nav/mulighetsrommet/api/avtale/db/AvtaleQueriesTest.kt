package no.nav.mulighetsrommet.api.avtale.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.*
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotliquery.Query
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.*
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
        val arenaAvtale = ArenaAvtaleDbo(
            id = UUID.randomUUID(),
            navn = "Arena-avtale",
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            avtalenummer = "2023#1",
            arrangorOrganisasjonsnummer = "123456789",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(3),
            arenaAnsvarligEnhet = "9999",
            avtaletype = Avtaletype.RAMMEAVTALE,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            prisbetingelser = "Alt er dyrt",
        )

        val domain = MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
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
                    it.arrangor?.id shouldBe ArrangorFixtures.hovedenhet.id
                    it.arrangor?.organisasjonsnummer?.value shouldBe arenaAvtale.arrangorOrganisasjonsnummer

                    it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = null, enhetsnummer = "9999")
                    it.startDato shouldBe arenaAvtale.startDato
                    it.sluttDato shouldBe arenaAvtale.sluttDato
                    it.avtaletype shouldBe arenaAvtale.avtaletype
                    it.status shouldBe AvtaleStatusDto.Aktiv
                    it.opphav shouldBe ArenaMigrering.Opphav.ARENA
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
                avtale1Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeGreaterThanOrEqual 10_000

                val avtale2Avtalenummer = queries.get(avtale2Id).shouldNotBeNull().avtalenummer.shouldNotBeNull()
                avtale2Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeLessThan avtale2Avtalenummer.substring(5).toInt()
            }
        }

        test("upsert avtale uten arrangør") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val avtaleId = AvtaleFixtures.oppfolging.id

                queries.upsert(AvtaleFixtures.oppfolging.copy(arrangor = null))

                queries.get(avtaleId).shouldNotBeNull().arrangor.shouldBeNull()
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
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                }
            }
        }

        test("oppdater status") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val id = AvtaleFixtures.oppfolging.id
                queries.upsert(AvtaleFixtures.oppfolging)

                val tidspunkt = LocalDate.now().atStartOfDay()
                queries.setStatus(
                    id,
                    AvtaleStatus.AVBRUTT,
                    tidspunkt,
                    AarsakerOgForklaringRequest(listOf(AvbruttAarsak.ANNET), ":)"),
                )
                queries.get(id).shouldNotBeNull().status shouldBe AvtaleStatusDto.Avbrutt(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbruttAarsak.ANNET),
                    forklaring = ":)",
                )

                queries.setStatus(
                    id,
                    AvtaleStatus.AVBRUTT,
                    tidspunkt,
                    AarsakerOgForklaringRequest(listOf(AvbruttAarsak.FEILREGISTRERING), null),
                )
                queries.get(id).shouldNotBeNull().status shouldBe AvtaleStatusDto.Avbrutt(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbruttAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.setStatus(id, AvtaleStatus.AVSLUTTET, null, null)
                queries.get(id).shouldNotBeNull().status shouldBe AvtaleStatusDto.Avsluttet
            }
        }

        test("administrator for avtale") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                val ansatt1 = NavAnsattFixture.DonaldDuck
                val ansatt2 = NavAnsattFixture.MikkeMus

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
                    AvtaleDto.Administrator(ansatt2.navIdent, "Mikke Mus"),
                )

                queries.upsert(avtale1.copy(administratorer = listOf()))
                queries.get(avtale1.id).shouldNotBeNull().administratorer.shouldBeEmpty()
            }
        }

        test("avtalens nav-enheter hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Sel),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().kontorstruktur.shouldHaveSize(1).first().should {
                    it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                    it.kontorer.should { (first, second) ->
                        first.enhetsnummer shouldBe Gjovik.enhetsnummer
                        second.enhetsnummer shouldBe Sel.enhetsnummer
                    }
                }
            }
        }

        test("Nav-enheter uten overordnet enhet hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Oslo.enhetsnummer),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Oslo, Gjovik),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().kontorstruktur.should { (first, second) ->
                    first.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                    first.kontorer.shouldHaveSize(1).first().enhetsnummer shouldBe Gjovik.enhetsnummer

                    second.region.enhetsnummer shouldBe Oslo.enhetsnummer
                    second.kontorer.shouldBeEmpty()
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
                ansvarligFor = listOf(),
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
                arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                    kontaktpersoner = listOf(p1.id),
                ),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(p1, p2, p3),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.kontaktpersoner shouldContainExactly listOf(toAvtaleArrangorKontaktperson(p1))
                }
                val avtaleMedKontaktpersoner = avtale.copy(
                    arrangor = avtale.arrangor?.copy(
                        kontaktpersoner = listOf(p2.id, p3.id),
                    ),
                )

                queries.upsert(avtaleMedKontaktpersoner)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toAvtaleArrangorKontaktperson(p2),
                        toAvtaleArrangorKontaktperson(p3),
                    )
                }

                queries.frikobleKontaktpersonFraAvtale(p3.id, avtale.id)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(toAvtaleArrangorKontaktperson(p2))
                }

                val avtaleUtenKontaktpersoner = avtale.copy(
                    arrangor = avtale.arrangor?.copy(
                        kontaktpersoner = emptyList(),
                    ),
                )

                queries.upsert(avtaleUtenKontaktpersoner)
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.kontaktpersoner.shouldBeEmpty()
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
                arrangor = AvtaleDbo.Arrangor(
                    hovedenhet = ArrangorFixtures.hovedenhet.id,
                    underenheter = listOf(ArrangorFixtures.underenhet1.id, ArrangorFixtures.underenhet2.id),
                    kontaktpersoner = emptyList(),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.upsert(avtale)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.organisasjonsnummer shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
                    it.arrangor?.underenheter?.map { enhet -> enhet.organisasjonsnummer } shouldContainExactlyInAnyOrder listOf(
                        ArrangorFixtures.underenhet1.organisasjonsnummer,
                        ArrangorFixtures.underenhet2.organisasjonsnummer,
                    )
                }
            }
        }

        test("Underenheter and kontaktpersoner are deleted when arrangor is removed from avtale") {
            database.runAndRollback { session ->
                // Set up initial state
                val p1 = ArrangorKontaktperson(
                    id = UUID.randomUUID(),
                    arrangorId = ArrangorFixtures.hovedenhet.id,
                    navn = "Navn Navnesen",
                    telefon = "22232322",
                    epost = "navn@gmail.com",
                    beskrivelse = "beskrivelse",
                    ansvarligFor = listOf(),
                )
                val p2 = p1.copy(
                    id = UUID.randomUUID(),
                    navn = "Fredrik Navnesen",
                    telefon = "32322",
                )
                val underenhet1 = ArrangorFixtures.underenhet1
                val underenhet2 = ArrangorFixtures.underenhet2

                val avtale = AvtaleFixtures.oppfolging.copy(
                    arrangor = AvtaleDbo.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.id,
                        underenheter = listOf(underenhet1.id, underenhet2.id),
                        kontaktpersoner = listOf(p1.id, p2.id),
                    ),
                )

                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(p1, p2),
                    avtaler = listOf(avtale),
                ).setup(session)

                val queries = AvtaleQueries(session)

                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.underenheter.shouldNotBeEmpty()
                    it.arrangor?.kontaktpersoner.shouldNotBeEmpty()
                }
                // Remove arrangor from avtale
                queries.upsert(avtale.copy(arrangor = null))

                // Verify that underenheter and kontaktpersoner are deleted
                queries.get(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.underenheter.shouldBeNull()
                    it.arrangor?.kontaktpersoner.shouldBeNull()
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

        test("upsert prismodell med avtalte satser") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)
                val sats2 = AvtaltSats(LocalDate.of(2025, 7, 1), 2000)

                queries.upsert(
                    AvtaleFixtures.oppfolging.copy(
                        prismodell = Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
                        satser = listOf(sats2),
                    ),
                )

                queries.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().should { avtale ->
                    avtale.prismodell.shouldBeTypeOf<AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk>() should { it ->
                        it.satser shouldContainExactly listOf(
                            AvtaltSatsDto(
                                gjelderFra = LocalDate.of(2025, 7, 1),
                                pris = 2000,
                                valuta = "NOK",
                            ),
                        )
                    }
                }

                queries.upsert(
                    AvtaleFixtures.oppfolging.copy(
                        prismodell = Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                    ),
                )

                queries.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().should {
                    it.prismodell.shouldBeTypeOf<AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk>()
                }

                queries.upsert(
                    AvtaleFixtures.oppfolging.copy(
                        prismodell = Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                    ),
                )

                queries.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().should {
                    it.prismodell.shouldBeTypeOf<AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker>()
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
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                )

                queries.upsert(avtale1)

                queries.getAvtaleIdsByAdministrator(NavAnsattFixture.DonaldDuck.navIdent) shouldBe listOf(avtale1.id)
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
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                )
                val a2 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
                )

                queries.upsert(a1)
                queries.upsert(a2)

                queries.getAll(administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a1.id, a2.id)
                }

                queries.getAll(administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a2.id)
                }
            }
        }

        test("filtrering på AvtaleStatus") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val queries = AvtaleQueries(session)

                val avtaleAktiv = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.AKTIV,
                )
                queries.upsert(avtaleAktiv)

                val avtaleAvsluttet = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.AVSLUTTET,
                )
                queries.upsert(avtaleAvsluttet)

                val avtaleAvbrutt = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                )
                queries.upsert(avtaleAvbrutt)
                queries.setStatus(
                    avtaleAvbrutt.id,
                    AvtaleStatus.AVBRUTT,
                    LocalDateTime.now(),
                    aarsakerOgForklaring = AarsakerOgForklaringRequest(
                        listOf(AvbruttAarsak.FEILREGISTRERING),
                        null,
                    ),
                )

                val avtaleUtkast = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.UTKAST,
                )
                queries.upsert(avtaleUtkast)

                forAll(
                    row(listOf(AvtaleStatus.UTKAST), listOf(avtaleUtkast.id)),
                    row(listOf(AvtaleStatus.AKTIV), listOf(avtaleAktiv.id)),
                    row(listOf(AvtaleStatus.AVBRUTT), listOf(avtaleAvbrutt.id)),
                    row(listOf(AvtaleStatus.AVSLUTTET), listOf(avtaleAvsluttet.id)),
                    row(
                        listOf(AvtaleStatus.AVBRUTT, AvtaleStatus.AVSLUTTET),
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
                navEnheter = listOf(Oslo, Innlandet, Gjovik),
                tiltakstyper = listOf(
                    TiltakstypeFixtures.Oppfolging,
                    TiltakstypeFixtures.AFT,
                    TiltakstypeFixtures.VTA,
                ),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(navEnheter = setOf()),
                    AvtaleFixtures.AFT.copy(navEnheter = setOf()),
                    AvtaleFixtures.VTA.copy(navEnheter = setOf()),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${AvtaleFixtures.oppfolging.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0400' where id = '${AvtaleFixtures.AFT.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0502' where id = '${AvtaleFixtures.VTA.id}'"))

                queries.getAll(navRegioner = listOf(NavEnhetNummer("0300"))).should { (totalCount) ->
                    totalCount shouldBe 1
                }
                queries.getAll(navRegioner = listOf(NavEnhetNummer("0400"))).should { (totalCount) ->
                    totalCount shouldBe 2
                }
                queries.getAll(navRegioner = listOf(NavEnhetNummer("0502"))).should { (totalCount) ->
                    totalCount shouldBe 1
                }
            }
        }

        test("filtrering på Nav-enheter") {
            val domain = MulighetsrommetTestDomain(
                navEnheter = listOf(Innlandet, Gjovik, Sel),
                tiltakstyper = listOf(
                    TiltakstypeFixtures.Oppfolging,
                    TiltakstypeFixtures.AFT,
                    TiltakstypeFixtures.VTA,
                ),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(
                        navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                    ),
                    AvtaleFixtures.AFT.copy(
                        navEnheter = setOf(Innlandet.enhetsnummer, Sel.enhetsnummer),
                    ),
                    AvtaleFixtures.VTA.copy(navEnheter = setOf(Innlandet.enhetsnummer)),
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
                avtaletype = Avtaletype.AVTALE,
            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                avtaletype = Avtaletype.RAMMEAVTALE,
            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            )

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeAmo),
                avtaler = listOf(avtale1, avtale2, avtale3),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = AvtaleQueries(session)

                queries.getAll(avtaletyper = listOf(Avtaletype.AVTALE)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(avtale1.id)
                }

                queries.getAll(avtaletyper = listOf(Avtaletype.AVTALE, Avtaletype.OFFENTLIG_OFFENTLIG)).should {
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
                    AvtaleFixtures.oppfolging.copy(
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = ArrangorFixtures.hovedenhet.id,
                        ),
                    ),
                    AvtaleFixtures.oppfolging.copy(
                        id = UUID.randomUUID(),
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = ArrangorFixtures.underenhet1.id,
                        ),
                    ),
                    AvtaleFixtures.oppfolging.copy(
                        id = UUID.randomUUID(),
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = annenArrangor.id,
                        ),
                    ),
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
            organisasjonsform = "BEDR",
        )
        val arrangorB = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "bjarne",
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            organisasjonsform = "BEDR",
        )
        val arrangorC = ArrangorDto(
            id = UUID.randomUUID(),
            navn = "chris",
            organisasjonsnummer = Organisasjonsnummer("999888777"),
            organisasjonsform = "BEDR",
        )
        val domain = MulighetsrommetTestDomain(
            arrangorer = listOf(arrangorA, arrangorB, arrangorC),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.Jobbklubb),
            avtaler = listOf(
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Anders",
                    arrangor = arrangorFromHovedenhet(arrangorA.id),
                    sluttDato = LocalDate.of(2010, 1, 31),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Åse",
                    arrangor = arrangorFromHovedenhet(arrangorA.id),
                    sluttDato = LocalDate.of(2009, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Øyvind",
                    arrangor = arrangorFromHovedenhet(arrangorB.id),
                    sluttDato = LocalDate.of(2010, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Kjetil",
                    arrangor = arrangorFromHovedenhet(arrangorC.id),
                    sluttDato = LocalDate.of(2011, 1, 1),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale hos Ærfuglen Ærle",
                    arrangor = arrangorFromHovedenhet(arrangorB.id),
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
                ascending.items[1].arrangor shouldBe alvdal
                ascending.items[2].arrangor shouldBe bjarne
                ascending.items[3].arrangor shouldBe bjarne
                ascending.items[4].arrangor shouldBe chris

                val descending = queries.getAll(sortering = "arrangor-descending")
                descending.items[0].arrangor shouldBe chris
                descending.items[1].arrangor shouldBe bjarne
                descending.items[2].arrangor shouldBe bjarne
                descending.items[3].arrangor shouldBe alvdal
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
})

private fun toAvtaleArrangorKontaktperson(kontaktperson: ArrangorKontaktperson) = AvtaleDto.ArrangorKontaktperson(
    id = kontaktperson.id,
    navn = kontaktperson.navn,
    beskrivelse = kontaktperson.beskrivelse,
    telefon = kontaktperson.telefon,
    epost = kontaktperson.epost,
)

private infix fun Collection<AvtaleDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}

private fun arrangorFromHovedenhet(hovedenhet: UUID): AvtaleDbo.Arrangor {
    return AvtaleDbo.Arrangor(
        hovedenhet = hovedenhet,
        underenheter = emptyList(),
        kontaktpersoner = emptyList(),
    )
}
