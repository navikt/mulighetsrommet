package no.nav.mulighetsrommet.api.avtale.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Currency
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
        )

        test("upsert genererer nye løpenummer") {
            database.runAndRollback { session ->
                domain.setup(session)

                val avtale1 = AvtaleFixtures.oppfolging
                val avtale2 = AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID())

                queries.avtale.create(avtale1)
                queries.avtale.create(avtale2)

                val avtale1Avtalenummer = queries.avtale.getOrError(avtale1.id).avtalenummer.shouldNotBeNull()
                avtale1Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeGreaterThanOrEqual 10_000

                val avtale2Avtalenummer = queries.avtale.getOrError(avtale2.id).avtalenummer.shouldNotBeNull()
                avtale2Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeLessThan avtale2Avtalenummer.substring(5).toInt()
            }
        }

        test("upsert avtale uten arrangør") {
            database.runAndRollback { session ->
                domain.setup(session)

                val avtaleId = AvtaleFixtures.oppfolging.id

                queries.avtale.create(
                    AvtaleFixtures.oppfolging.copy(
                        detaljerDbo = AvtaleFixtures.detaljerDbo().copy(arrangor = null),
                    ),
                )

                queries.avtale.getOrError(avtaleId).arrangor.shouldBeNull()
            }
        }

        test("upsert setter opphav første gang avtalen lagres") {
            database.runAndRollback { session ->
                domain.setup(session)

                val id = UUID.randomUUID()
                queries.avtale.create(AvtaleFixtures.oppfolging.copy(id = id))
                queries.avtale.getOrError(id).should {
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                }
            }
        }

        test("oppdater status") {
            database.runAndRollback { session ->
                domain.setup(session)

                val id = AvtaleFixtures.oppfolging.id
                queries.avtale.create(AvtaleFixtures.oppfolging)

                val tidspunkt = LocalDate.now().atStartOfDay()
                queries.avtale.setStatus(
                    id = id,
                    status = AvtaleStatusType.AVBRUTT,
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytAvtaleAarsak.ANNET),
                    forklaring = ":)",
                )
                queries.avtale.getOrError(id).status shouldBe AvtaleStatus.Avbrutt(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytAvtaleAarsak.ANNET),
                    forklaring = ":)",
                )

                queries.avtale.setStatus(
                    id = id,
                    status = AvtaleStatusType.AVBRUTT,
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
                queries.avtale.getOrError(id).status shouldBe AvtaleStatus.Avbrutt(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.avtale.setStatus(
                    id = id,
                    status = AvtaleStatusType.AVSLUTTET,
                    tidspunkt = null,
                    aarsaker = null,
                    forklaring = null,
                )
                queries.avtale.getOrError(id).status shouldBe AvtaleStatus.Avsluttet
            }
        }

        test("administrator for avtale") {
            database.runAndRollback { session ->
                domain.setup(session)

                val ansatt1 = NavAnsattFixture.DonaldDuck
                val ansatt2 = NavAnsattFixture.MikkeMus

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(administratorer = listOf(ansatt1.navIdent)),
                )

                queries.avtale.create(avtale1)
                queries.avtale.getOrError(avtale1.id).administratorer shouldContainExactlyInAnyOrder listOf(
                    Avtale.Administrator(ansatt1.navIdent, "Donald Duck"),
                )

                queries.avtale.updateDetaljer(
                    avtale1.id,
                    AvtaleFixtures.detaljerDbo().copy(
                        administratorer = listOf(ansatt1.navIdent, ansatt2.navIdent),
                    ),
                )
                queries.avtale.getOrError(avtale1.id).administratorer shouldContainExactlyInAnyOrder listOf(
                    Avtale.Administrator(ansatt1.navIdent, "Donald Duck"),
                    Avtale.Administrator(ansatt2.navIdent, "Mikke Mus"),
                )

                queries.avtale.updateDetaljer(
                    avtale1.id,
                    AvtaleFixtures.detaljerDbo().copy(administratorer = listOf()),
                )
                queries.avtale.getOrError(avtale1.id).administratorer.shouldBeEmpty()
            }
        }

        test("avtalens nav-enheter hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                    navEnheter = setOf(
                        Innlandet.enhetsnummer,
                        Gjovik.enhetsnummer,
                        Sel.enhetsnummer,
                    ),
                ),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Sel),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).setup(session)

                queries.avtale.getOrError(avtale.id).kontorstruktur.shouldHaveSize(1).first().should {
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
                veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                    navEnheter = setOf(
                        Innlandet.enhetsnummer,
                        Gjovik.enhetsnummer,
                        Oslo.enhetsnummer,
                    ),
                ),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Oslo, Gjovik),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).setup(session)

                queries.avtale.getOrError(avtale.id).kontorstruktur.should { (first, second) ->
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
                detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                    arrangor = AvtaleFixtures.oppfolging.detaljerDbo.arrangor?.copy(
                        kontaktpersoner = listOf(p1.id),
                    ),
                ),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(p1, p2, p3),
                    avtaler = listOf(avtale),
                ).setup(session)

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactly listOf(toAvtaleArrangorKontaktperson(p1))
                }

                queries.avtale.updateDetaljer(
                    avtale.id,
                    AvtaleFixtures.detaljerDbo().copy(
                        arrangor = avtale.detaljerDbo.arrangor?.copy(kontaktpersoner = listOf(p2.id, p3.id)),
                    ),
                )

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toAvtaleArrangorKontaktperson(p2),
                        toAvtaleArrangorKontaktperson(p3),
                    )
                }

                queries.avtale.frikobleKontaktpersonFraAvtale(p3.id, avtale.id)
                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(toAvtaleArrangorKontaktperson(p2))
                }

                queries.avtale.updateDetaljer(
                    avtale.id,
                    AvtaleFixtures.detaljerDbo().copy(
                        arrangor = avtale.detaljerDbo.arrangor?.copy(kontaktpersoner = emptyList()),
                    ),
                )
                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner.shouldBeEmpty()
                }
            }
        }

        test("Personopplysninger") {
            database.runAndRollback { session ->
                domain.setup(session)

                var avtale = AvtaleFixtures.oppfolging.copy(
                    personvernDbo = AvtaleFixtures.personvernDbo(
                        personopplysninger = listOf(Personopplysning.NAVN),
                    ),
                )
                queries.avtale.create(avtale)
                queries.avtale.getOrError(avtale.id).should {
                    it.personopplysninger shouldContainExactly listOf(Personopplysning.NAVN)
                }

                queries.avtale.updatePersonvern(
                    avtale.id,
                    AvtaleFixtures.personvernDbo(
                        personopplysninger = listOf(Personopplysning.KJONN, Personopplysning.ADFERD),
                    ),
                )
                queries.avtale.getOrError(avtale.id).should {
                    it.personopplysninger shouldContainExactly listOf(Personopplysning.KJONN, Personopplysning.ADFERD)
                }

                queries.avtale.updatePersonvern(
                    avtale.id,
                    AvtaleFixtures.personvernDbo(personopplysninger = emptyList()),
                )
                queries.avtale.getOrError(avtale.id).should {
                    it.personopplysninger shouldHaveSize 0
                }
            }
        }

        test("Underenheter blir riktig med fra spørring") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                    arrangor = ArrangorDbo(
                        hovedenhet = ArrangorFixtures.hovedenhet.id,
                        underenheter = listOf(ArrangorFixtures.underenhet1.id, ArrangorFixtures.underenhet2.id),
                        kontaktpersoner = emptyList(),
                    ),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.avtale.create(avtale)

                queries.avtale.getOrError(avtale.id).should {
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
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        arrangor = ArrangorDbo(
                            hovedenhet = ArrangorFixtures.hovedenhet.id,
                            underenheter = listOf(underenhet1.id, underenhet2.id),
                            kontaktpersoner = listOf(p1.id, p2.id),
                        ),
                    ),
                )

                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(p1, p2),
                    avtaler = listOf(avtale),
                ).setup(session)

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.underenheter.shouldNotBeEmpty()
                    it.arrangor?.kontaktpersoner.shouldNotBeEmpty()
                }

                // Remove arrangor from avtale
                queries.avtale.updateDetaljer(avtale.id, AvtaleFixtures.detaljerDbo().copy(arrangor = null))

                // Verify that underenheter and kontaktpersoner are deleted
                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.underenheter.shouldBeNull()
                    it.arrangor?.kontaktpersoner.shouldBeNull()
                }
            }
        }

        test("gruppe amo kategorier") {
            database.runAndRollback { session ->
                domain.setup(session)

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
                val avtale = AvtaleFixtures.oppfolging.copy(
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(amoKategorisering = amoKategorisering),
                )
                queries.avtale.create(avtale)
                queries.avtale.getOrError(avtale.id).should {
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
                queries.avtale.updateDetaljer(
                    avtale.id,
                    AvtaleFixtures.detaljerDbo().copy(amoKategorisering = amoEndring),
                )
                queries.avtale.getOrError(avtale.id).should {
                    it.amoKategorisering shouldBe amoEndring
                }

                queries.avtale.updateDetaljer(avtale.id, AvtaleFixtures.detaljerDbo().copy(amoKategorisering = null))
                queries.avtale.getOrError(avtale.id).should {
                    it.amoKategorisering shouldBe null
                }
            }
        }

        test("endre prismodeller") {
            val prismodell1Dbo = PrismodellFixtures.createPrismodellDbo(
                type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                satser = listOf(AvtaltSats(LocalDate.of(2025, 7, 1), 1000, Currency.NOK)),
            )
            val prismodell1 = Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker(
                id = prismodell1Dbo.id,
                prisbetingelser = null,
                satser = listOf(AvtaltSatsDto(LocalDate.of(2025, 7, 1), 1000, Currency.NOK)),
            )
            val prismodell2Dbo = PrismodellFixtures.createPrismodellDbo(
                type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                satser = listOf(AvtaltSats(LocalDate.of(2025, 7, 1), 2000, Currency.NOK)),
            )

            database.runAndRollback { session ->
                domain.setup(session)
                queries.prismodell.upsert(prismodell1Dbo)
                queries.prismodell.upsert(prismodell2Dbo)

                val avtale = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodell1Dbo.id))
                queries.avtale.create(avtale)

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(
                    prismodell1,
                )

                queries.avtale.upsertPrismodell(avtale.id, prismodell2Dbo.id)

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(
                    prismodell1,
                    Prismodell.AvtaltPrisPerManedsverk(
                        id = prismodell2Dbo.id,
                        prisbetingelser = null,
                        satser = listOf(AvtaltSatsDto(LocalDate.of(2025, 7, 1), 2000, Currency.NOK)),
                    ),
                )

                queries.prismodell.upsert(
                    prismodell2Dbo.copy(type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK, prisbetingelser = "$"),
                )

                queries.avtale.deletePrismodell(avtale.id, prismodell1Dbo.id)

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(
                    Prismodell.AvtaltPrisPerHeleUkesverk(
                        id = prismodell2Dbo.id,
                        prisbetingelser = "$",
                        satser = listOf(AvtaltSatsDto(LocalDate.of(2025, 7, 1), 2000, Currency.NOK)),
                    ),
                )
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
            tiltakstyper = listOf(
                TiltakstypeFixtures.Oppfolging,
                TiltakstypeFixtures.AFT,
                TiltakstypeFixtures.GruppeAmo,
            ),
        )

        test("fritekstsøk på avtalenavn og avtalenummer") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val avtalenummer1 = "2024#1000"
                val avtalenummer2 = "2024#2000"

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        navn = "Avtale om opplæring av blinde krokodiller",
                    ),
                )

                val avtale2 = avtale1.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        navn = "Avtale om undervisning av underlige ulver",
                    ),
                )

                queries.avtale.create(avtale1)
                queries.avtale.upsertAvtalenummer(avtale1.id, avtalenummer1)
                queries.avtale.create(avtale2)
                queries.avtale.upsertAvtalenummer(avtale2.id, avtalenummer2)

                queries.avtale.getAll(search = "krokodillen").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale1.id
                }

                queries.avtale.getAll(search = "avtale").should {
                    it.totalCount shouldBe 2
                }

                queries.avtale.getAll(search = "avtale ulv").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.avtale.getAll(search = "krok").should {
                    it.totalCount shouldBe 1
                }

                queries.avtale.getAll(search = "avtale kråke").should {
                    it.totalCount shouldBe 0
                }

                queries.avtale.getAll(search = "2000").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.avtale.getAll(search = "2024").should {
                    it.totalCount shouldBe 2
                }
            }
        }

        test("administrator") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val a1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                    ),
                )
                val a2 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        administratorer = listOf(
                            NavAnsattFixture.DonaldDuck.navIdent,
                            NavAnsattFixture.MikkeMus.navIdent,
                        ),
                    ),
                )

                queries.avtale.create(a1)
                queries.avtale.create(a2)

                queries.avtale.getAll(administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a1.id, a2.id)
                }

                queries.avtale.getAll(administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a2.id)
                }
            }
        }

        test("filtrering på AvtaleStatus") {
            database.runAndRollback { session ->
                oppfolgingDomain.setup(session)

                val avtaleAktiv = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        status = AvtaleStatusType.AKTIV,
                    ),
                )
                queries.avtale.create(avtaleAktiv)

                val avtaleAvsluttet = AvtaleFixtures.AFT.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        status = AvtaleStatusType.AVSLUTTET,
                    ),
                )
                queries.avtale.create(avtaleAvsluttet)

                val avtaleAvbrutt = AvtaleFixtures.gruppeAmo.copy(
                    id = UUID.randomUUID(),
                )
                queries.avtale.create(avtaleAvbrutt)
                queries.avtale.setStatus(
                    avtaleAvbrutt.id,
                    AvtaleStatusType.AVBRUTT,
                    LocalDateTime.now(),
                    listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                    null,
                )

                val avtaleUtkast = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.detaljerDbo().copy(
                        status = AvtaleStatusType.UTKAST,
                    ),
                )
                queries.avtale.create(avtaleUtkast)

                forAll(
                    row(listOf(AvtaleStatusType.UTKAST), listOf(avtaleUtkast.id)),
                    row(listOf(AvtaleStatusType.AKTIV), listOf(avtaleAktiv.id)),
                    row(listOf(AvtaleStatusType.AVBRUTT), listOf(avtaleAvbrutt.id)),
                    row(listOf(AvtaleStatusType.AVSLUTTET), listOf(avtaleAvsluttet.id)),
                    row(
                        listOf(AvtaleStatusType.AVBRUTT, AvtaleStatusType.AVSLUTTET),
                        listOf(avtaleAvbrutt.id, avtaleAvsluttet.id),
                    ),
                ) { statuser, expected ->
                    val result = queries.avtale.getAll(statuser = statuser)
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
                    AvtaleFixtures.oppfolging.copy(
                        veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                            navEnheter = setOf(),
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(navEnheter = setOf())),
                    AvtaleFixtures.VTA.copy(veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(navEnheter = setOf())),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0300' where id = '${AvtaleFixtures.oppfolging.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0400' where id = '${AvtaleFixtures.AFT.id}'"))
                session.execute(Query("update avtale set arena_ansvarlig_enhet = '0502' where id = '${AvtaleFixtures.VTA.id}'"))

                queries.avtale.getAll(navEnheter = listOf(NavEnhetNummer("0300"))).should { (totalCount) ->
                    totalCount shouldBe 1
                }
                queries.avtale.getAll(navEnheter = listOf(NavEnhetNummer("0400"))).should { (totalCount) ->
                    totalCount shouldBe 2
                }
                queries.avtale.getAll(navEnheter = listOf(NavEnhetNummer("0502"))).should { (totalCount) ->
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
                        veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                            navEnheter = setOf(
                                Innlandet.enhetsnummer,
                                Gjovik.enhetsnummer,
                            ),
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(
                        veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                            navEnheter = setOf(
                                Innlandet.enhetsnummer,
                                Sel.enhetsnummer,
                            ),
                        ),
                    ),
                    AvtaleFixtures.VTA.copy(
                        veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                            navEnheter = setOf(
                                Innlandet.enhetsnummer,
                            ),
                        ),
                    ),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.avtale.getAll(
                    navEnheter = listOf(Innlandet.enhetsnummer),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }

                queries.avtale.getAll(
                    navEnheter = listOf(Gjovik.enhetsnummer, Sel.enhetsnummer),
                ).should { (totalCount, items) ->
                    totalCount shouldBe 2
                    items shouldContainExactlyIds listOf(AvtaleFixtures.oppfolging.id, AvtaleFixtures.AFT.id)
                }
            }
        }

        test("Filtrer på avtaletyper returnerer riktige avtaler") {
            val avtale1 = AvtaleFixtures.gruppeAmo.copy(
                id = UUID.randomUUID(),
                detaljerDbo = AvtaleFixtures.gruppeAmo.detaljerDbo.copy(
                    avtaletype = Avtaletype.AVTALE,
                ),

            )
            val avtale2 = avtale1.copy(
                id = UUID.randomUUID(),
                detaljerDbo = AvtaleFixtures.gruppeAmo.detaljerDbo.copy(
                    avtaletype = Avtaletype.RAMMEAVTALE,
                ),

            )
            val avtale3 = avtale1.copy(
                id = UUID.randomUUID(),
                detaljerDbo = AvtaleFixtures.gruppeAmo.detaljerDbo.copy(
                    avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                ),

            )

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeAmo),
                avtaler = listOf(avtale1, avtale2, avtale3),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.avtale.getAll(avtaletyper = listOf(Avtaletype.AVTALE)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(avtale1.id)
                }

                queries.avtale.getAll(avtaletyper = listOf(Avtaletype.AVTALE, Avtaletype.OFFENTLIG_OFFENTLIG)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(avtale1.id, avtale3.id)
                }

                queries.avtale.getAll(avtaletyper = listOf()).should {
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

                queries.avtale.getAll(
                    tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id),
                ).should { (totalCount, items) ->
                    totalCount shouldBe 2
                    items shouldContainExactlyIds listOf(domain.avtaler[0].id, domain.avtaler[1].id)
                }

                queries.avtale.getAll(
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
                        detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                            arrangor = AvtaleFixtures.oppfolging.detaljerDbo.arrangor?.copy(
                                hovedenhet = ArrangorFixtures.hovedenhet.id,
                            ),
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(
                        id = UUID.randomUUID(),
                        detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                            arrangor = AvtaleFixtures.oppfolging.detaljerDbo.arrangor?.copy(
                                hovedenhet = ArrangorFixtures.underenhet1.id,
                            ),
                        ),
                    ),
                    AvtaleFixtures.gruppeAmo.copy(
                        id = UUID.randomUUID(),
                        detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                            arrangor = AvtaleFixtures.oppfolging.detaljerDbo.arrangor?.copy(
                                hovedenhet = annenArrangor.id,
                            ),
                        ),
                    ),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.avtale.getAll(search = "enhet").totalCount shouldBe 2
                queries.avtale.getAll(search = "annen").totalCount shouldBe 1
            }
        }

        test("Filtrering på personvern_bekreftet") {
            val domain = MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(personvernDbo = AvtaleFixtures.personvernDbo(personvernBekreftet = true)),
                    AvtaleFixtures.AFT.copy(
                        id = UUID.randomUUID(),
                        personvernDbo = AvtaleFixtures.personvernDbo(personvernBekreftet = true),
                    ),
                    AvtaleFixtures.gruppeAmo.copy(
                        id = UUID.randomUUID(),
                        personvernDbo = AvtaleFixtures.personvernDbo(personvernBekreftet = false),
                    ),
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.avtale.getAll(personvernBekreftet = true).totalCount shouldBe 2
                queries.avtale.getAll(personvernBekreftet = false).totalCount shouldBe 1
                queries.avtale.getAll(personvernBekreftet = null).totalCount shouldBe 3
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
                    detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                        navn = "Avtale hos Anders",
                        arrangor = arrangorFromHovedenhet(arrangorA.id),
                        sluttDato = LocalDate.of(2010, 1, 31),
                    ),
                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                        navn = "Avtale hos Åse",
                        arrangor = arrangorFromHovedenhet(arrangorA.id),
                        sluttDato = LocalDate.of(2009, 1, 1),
                    ),

                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                        navn = "Avtale hos Øyvind",
                        arrangor = arrangorFromHovedenhet(arrangorB.id),
                        sluttDato = LocalDate.of(2010, 1, 1),
                    ),

                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                        navn = "Avtale hos Kjetil",
                        arrangor = arrangorFromHovedenhet(arrangorC.id),
                        sluttDato = LocalDate.of(2011, 1, 1),
                    ),

                ),
                AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                        navn = "Avtale hos Ærfuglen Ærle",
                        arrangor = arrangorFromHovedenhet(arrangorB.id),
                        sluttDato = LocalDate.of(2023, 1, 1),
                    ),
                ),
            ),
        )

        test("Sortering på navn fra a-å sorterer korrekt med æøå til slutt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val result = queries.avtale.getAll(sortering = "navn-ascending")

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

                val result = queries.avtale.getAll(sortering = "navn-descending")

                result.totalCount shouldBe 5
                result.items[0].navn shouldBe "Avtale hos Åse"
                result.items[1].navn shouldBe "Avtale hos Øyvind"
                result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[3].navn shouldBe "Avtale hos Kjetil"
                result.items[4].navn shouldBe "Avtale hos Anders"
            }
        }

        test("Sortering på arrangør sorterer korrekt") {
            val alvdal = Avtale.ArrangorHovedenhet(
                id = arrangorA.id,
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                navn = "alvdal",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val bjarne = Avtale.ArrangorHovedenhet(
                id = arrangorB.id,
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "bjarne",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )
            val chris = Avtale.ArrangorHovedenhet(
                id = arrangorC.id,
                organisasjonsnummer = Organisasjonsnummer("999888777"),
                navn = "chris",
                slettet = false,
                underenheter = listOf(),
                kontaktpersoner = emptyList(),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val ascending = queries.avtale.getAll(sortering = "arrangor-ascending")

                ascending.items[0].arrangor shouldBe alvdal
                ascending.items[1].arrangor shouldBe alvdal
                ascending.items[2].arrangor shouldBe bjarne
                ascending.items[3].arrangor shouldBe bjarne
                ascending.items[4].arrangor shouldBe chris

                val descending = queries.avtale.getAll(sortering = "arrangor-descending")
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

                val result = queries.avtale.getAll(sortering = "sluttdato-descending")
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

                val result = queries.avtale.getAll(sortering = "sluttdato-ascending")
                result.items[0].navn shouldBe "Avtale hos Åse"
                result.items[1].navn shouldBe "Avtale hos Øyvind"
                result.items[2].navn shouldBe "Avtale hos Anders"
                result.items[3].navn shouldBe "Avtale hos Kjetil"
                result.items[4].navn shouldBe "Avtale hos Ærfuglen Ærle"
            }
        }
    }
})

private fun toAvtaleArrangorKontaktperson(kontaktperson: ArrangorKontaktperson) = Avtale.ArrangorKontaktperson(
    id = kontaktperson.id,
    navn = kontaktperson.navn,
    beskrivelse = kontaktperson.beskrivelse,
    telefon = kontaktperson.telefon,
    epost = kontaktperson.epost,
)

private infix fun Collection<Avtale>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}

private fun arrangorFromHovedenhet(hovedenhet: UUID): ArrangorDbo {
    return ArrangorDbo(
        hovedenhet = hovedenhet,
        underenheter = emptyList(),
        kontaktpersoner = emptyList(),
    )
}
