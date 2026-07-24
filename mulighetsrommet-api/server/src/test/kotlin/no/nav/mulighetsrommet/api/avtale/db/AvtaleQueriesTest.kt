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
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.tiltak.AvtaleDto
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.api.domain.tiltak.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.InnholdElementFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

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
            database.runAndRollback {
                domain.initialize()

                val avtale1 = AvtaleFixtures.oppfolging
                val avtale2 = AvtaleFixtures.oppfolging.copy(id = UUID.randomUUID())

                repository.avtale.save(avtale1)
                repository.avtale.save(avtale2)

                val avtale1Avtalenummer = queries.avtale.getOrError(avtale1.id).avtalenummer.shouldNotBeNull()
                avtale1Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeGreaterThanOrEqual 10_000

                val avtale2Avtalenummer = queries.avtale.getOrError(avtale2.id).avtalenummer.shouldNotBeNull()
                avtale2Avtalenummer.take(4).toInt() shouldBe LocalDate.now().year
                avtale1Avtalenummer.substring(5).toInt() shouldBeLessThan avtale2Avtalenummer.substring(5).toInt()
            }
        }

        test("upsert avtale uten arrangør") {
            database.runAndRollback {
                domain.initialize()

                val avtaleId = AvtaleFixtures.oppfolging.id

                repository.avtale.save(
                    AvtaleFixtures.oppfolging.copy(
                        arrangor = null,
                    ),
                )

                queries.avtale.getOrError(avtaleId).arrangor.shouldBeNull()
            }
        }

        test("oppdater status") {
            database.runAndRollback {
                domain.initialize()

                val id = AvtaleFixtures.oppfolging.id
                repository.avtale.save(AvtaleFixtures.oppfolging)

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
            database.runAndRollback {
                domain.initialize()

                val ansatt1 = NavAnsattFixture.DonaldDuck
                val ansatt2 = NavAnsattFixture.MikkeMus

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = setOf(ansatt1.navIdent),
                )

                repository.avtale.save(avtale1)
                queries.avtale.getOrError(avtale1.id).administratorer shouldContainExactlyInAnyOrder listOf(
                    ansatt1.navIdent,
                )

                repository.avtale.save(
                    avtale1.copy(administratorer = setOf(ansatt1.navIdent, ansatt2.navIdent)),
                )
                queries.avtale.getOrError(avtale1.id).administratorer shouldContainExactlyInAnyOrder listOf(
                    ansatt1.navIdent,
                    ansatt2.navIdent,
                )

                repository.avtale.save(
                    avtale1.copy(administratorer = emptySet()),
                )
                queries.avtale.getOrError(avtale1.id).administratorer.shouldBeEmpty()
            }
        }

        test("avtalens nav-enheter hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                veilederinfo = Avtale.VeilederInfo(
                    navEnheter = setOf(
                        Innlandet.enhetsnummer,
                        Gjovik.enhetsnummer,
                        Sel.enhetsnummer,
                    ),
                ),
            )

            database.runAndRollback {
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Sel),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).initialize()

                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull()
                    .kontorstruktur.shouldHaveSize(1).should { (first) ->
                        first.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        first.kontorer.should { (first, second) ->
                            first.enhetsnummer shouldBe Gjovik.enhetsnummer
                            second.enhetsnummer shouldBe Sel.enhetsnummer
                        }
                    }
            }
        }

        test("Nav-enheter uten overordnet enhet hentes med riktig kontorstruktur") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                veilederinfo = Avtale.VeilederInfo(
                    navEnheter = setOf(
                        Innlandet.enhetsnummer,
                        Gjovik.enhetsnummer,
                        Oslo.enhetsnummer,
                    ),
                ),
            )

            database.runAndRollback {
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Oslo, Gjovik),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(avtale),
                ).initialize()

                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().kontorstruktur.should { (first, second) ->
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
                arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(kontaktpersoner = listOf(p1.id)),
            )

            database.runAndRollback {
                MulighetsrommetTestDomain(
                    arrangorer = listOf(
                        ArrangorFixtures.hovedenhet.registrerKontaktpersoner(listOf(p1, p2, p3)),
                        ArrangorFixtures.underenhet1,
                    ),
                    avtaler = listOf(avtale),
                ).initialize()

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactly listOf(p1.id)
                }

                repository.avtale.save(
                    avtale.copy(arrangor = avtale.arrangor?.copy(kontaktpersoner = listOf(p2.id, p3.id))),
                )

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        p2.id,
                        p3.id,
                    )
                }

                queries.avtale.frikobleKontaktpersonFraAvtale(p3.id, avtale.id)
                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(p2.id)
                }

                repository.avtale.save(
                    avtale.copy(arrangor = avtale.arrangor?.copy(kontaktpersoner = emptyList())),
                )
                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.kontaktpersoner.shouldBeEmpty()
                }
            }
        }

        test("Personopplysninger") {
            database.runAndRollback {
                domain.initialize()

                val avtale = AvtaleFixtures.oppfolging.copy(
                    personvern = Avtale.Personvern(
                        personopplysninger = setOf(Personopplysning.Type.NAVN),
                        annetBeskrivelse = null,
                        erBekreftet = false,
                    ),
                )
                repository.avtale.save(avtale)
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger.map { it.type } shouldContainExactly listOf(Personopplysning.Type.NAVN)
                }

                queries.avtale.updatePersonvern(
                    avtale.id,
                    Avtale.Personvern(
                        personopplysninger = setOf(
                            Personopplysning.Type.KJONN,
                            Personopplysning.Type.ADFERD,
                        ),
                        annetBeskrivelse = null,
                        erBekreftet = false,
                    ),
                )
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger.map { it.type } shouldContainExactlyInAnyOrder listOf(
                        Personopplysning.Type.KJONN,
                        Personopplysning.Type.ADFERD,
                    )
                }

                queries.avtale.updatePersonvern(
                    avtale.id,
                    Avtale.Personvern(personopplysninger = emptySet(), annetBeskrivelse = null, erBekreftet = false),
                )
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.personopplysninger shouldHaveSize 0
                }
            }
        }

        test("Underenheter blir riktig med fra spørring") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                arrangor = Avtale.Arrangor(
                    hovedenhet = ArrangorFixtures.hovedenhet.id,
                    underenheter = listOf(ArrangorFixtures.underenhet1.id, ArrangorFixtures.underenhet2.id),
                ),
            )

            database.runAndRollback {
                domain.initialize()

                repository.avtale.save(avtale)

                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.arrangor?.organisasjonsnummer shouldBe ArrangorFixtures.hovedenhet.organisasjonsnummer
                    it.arrangor?.underenheter?.map { enhet -> enhet.organisasjonsnummer } shouldContainExactlyInAnyOrder listOf(
                        ArrangorFixtures.underenhet1.organisasjonsnummer,
                        ArrangorFixtures.underenhet2.organisasjonsnummer,
                    )
                }
            }
        }

        test("Underenheter and kontaktpersoner are deleted when arrangor is removed from avtale") {
            database.runAndRollback {
                val p1 = ArrangorFixtures.kontaktperson(arrangorId = ArrangorFixtures.hovedenhet.id)
                val p2 = p1.copy(
                    id = UUID.randomUUID(),
                    navn = "Fredrik Navnesen",
                    telefon = "32322",
                )
                val hovedenhet = ArrangorFixtures.hovedenhet.registrerKontaktpersoner(listOf(p1, p2))
                val underenhet1 = ArrangorFixtures.underenhet1
                val underenhet2 = ArrangorFixtures.underenhet2

                val avtale = AvtaleFixtures.oppfolging.copy(
                    arrangor = Avtale.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.id,
                        underenheter = listOf(underenhet1.id, underenhet2.id),
                        kontaktpersoner = listOf(p1.id, p2.id),
                    ),
                )

                MulighetsrommetTestDomain(
                    arrangorer = listOf(hovedenhet, underenhet1, underenhet2),
                    avtaler = listOf(avtale),
                ).initialize()

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor?.underenheter.shouldNotBeEmpty()
                    it.arrangor?.kontaktpersoner.shouldNotBeEmpty()
                }

                repository.avtale.save(avtale.copy(arrangor = null))

                queries.avtale.getOrError(avtale.id).should {
                    it.arrangor.shouldBeNull()
                }
            }
        }

        test("gruppe amo kategorier") {
            database.runAndRollback {
                domain.initialize()

                val kategorisering = OpplaringKategorisering(
                    kurstype = KurstypeFixtures.bransjeOgYrkesrettet.id,
                    bransje = BransjeFixtures.industriarbeid.id,
                    forerkort = emptySet(),
                    innholdElementer = setOf(InnholdElementFixtures.teoretiskOpplaring.id),
                    norskprove = null,
                    sertifiseringer = setOf(
                        Sertifisering(
                            konseptId = 1,
                            label = "label",
                        ),
                    ),
                    utdanningslop = null,
                )
                val avtale = AvtaleFixtures.oppfolging.copy(
                    opplaring = kategorisering,
                )
                repository.avtale.save(avtale)
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.opplaring shouldBe OpplaringKategoriseringDetaljer(
                        kurstype = KurstypeFixtures.bransjeOgYrkesrettet,
                        bransje = BransjeFixtures.industriarbeid,
                        innholdElementer = setOf(InnholdElementFixtures.teoretiskOpplaring),
                        sertifiseringer = setOf(Sertifisering(1, "label")),
                    )
                }

                repository.avtale.save(
                    avtale.copy(
                        opplaring = kategorisering.copy(
                            bransje = BransjeFixtures.helseOgPleier.id,
                            sertifiseringer = setOf(
                                Sertifisering(
                                    konseptId = 2,
                                    label = "label2",
                                ),
                            ),
                        ),
                    ),
                )
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.opplaring shouldBe OpplaringKategoriseringDetaljer(
                        kurstype = KurstypeFixtures.bransjeOgYrkesrettet,
                        bransje = BransjeFixtures.helseOgPleier,
                        innholdElementer = setOf(InnholdElementFixtures.teoretiskOpplaring),
                        sertifiseringer = setOf(Sertifisering(2, "label2")),
                    )
                }

                repository.avtale.save(
                    avtale.copy(opplaring = null),
                )
                queries.avtale.getAvtaleDto(avtale.id).shouldNotBeNull().should {
                    it.opplaring shouldBe null
                }
            }
        }

        test("endre prismodeller") {
            val prismodell1 = PrismodellFixtures.createPrismodell(
                type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                satser = listOf(
                    AvtaltSats(
                        LocalDate.of(2025, 7, 1),
                        1000.NOK,
                    ),
                ),
            )
            val prismodell2 = PrismodellFixtures.createPrismodell(
                type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                satser = listOf(
                    AvtaltSats(LocalDate.of(2025, 7, 1), 2000.NOK),
                ),
            )

            database.runAndRollback {
                domain.initialize()

                val avtale = AvtaleFixtures.oppfolging.copy(prismodeller = listOf(prismodell1))
                repository.avtale.save(avtale)

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(prismodell1)

                repository.avtale.save(avtale.copy(prismodeller = listOf(prismodell1, prismodell2)))

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(
                    prismodell1,
                    prismodell2,
                )

                val prismodell3 = Prismodell.AvtaltPrisPerHeleUkesverk(
                    id = prismodell2.id,
                    prisbetingelser = "$",
                    satser = listOf(AvtaltSats(LocalDate.of(2025, 7, 1), 2000.NOK)),
                    valuta = Valuta.NOK,
                )
                repository.avtale.save(avtale.copy(prismodeller = listOf(prismodell3)))

                queries.avtale.getOrError(avtale.id).prismodeller shouldContainExactlyInAnyOrder listOf(prismodell3)
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
            database.runAndRollback {
                oppfolgingDomain.initialize()

                val avtalenummer1 = "2024#1000"
                val avtalenummer2 = "2024#2000"

                val avtale1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om opplæring av blinde krokodiller",
                )

                val avtale2 = avtale1.copy(
                    id = UUID.randomUUID(),
                    navn = "Avtale om undervisning av underlige ulver",
                )

                repository.avtale.save(avtale1)
                queries.avtale.upsertAvtalenummer(avtale1.id, avtalenummer1)
                repository.avtale.save(avtale2)
                queries.avtale.upsertAvtalenummer(avtale2.id, avtalenummer2)

                queries.avtale.getAllAvtaleDto(search = "krokodillen").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale1.id
                }

                queries.avtale.getAllAvtaleDto(search = "avtale").should {
                    it.totalCount shouldBe 2
                }

                queries.avtale.getAllAvtaleDto(search = "avtale ulv").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.avtale.getAllAvtaleDto(search = "krok").should {
                    it.totalCount shouldBe 1
                }

                queries.avtale.getAllAvtaleDto(search = "avtale kråke").should {
                    it.totalCount shouldBe 0
                }

                queries.avtale.getAllAvtaleDto(search = "2000").should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe avtale2.id
                }

                queries.avtale.getAllAvtaleDto(search = "2024").should {
                    it.totalCount shouldBe 2
                }
            }
        }

        test("administrator") {
            database.runAndRollback {
                oppfolgingDomain.initialize()

                val a1 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = setOf(NavAnsattFixture.DonaldDuck.navIdent),
                )
                val a2 = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    administratorer = setOf(
                        NavAnsattFixture.DonaldDuck.navIdent,
                        NavAnsattFixture.MikkeMus.navIdent,
                    ),
                )

                repository.avtale.save(a1)
                repository.avtale.save(a2)

                queries.avtale.getAllAvtaleDto(administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a1.id, a2.id)
                }

                queries.avtale.getAllAvtaleDto(administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent).should {
                    it.items shouldContainExactlyIds listOf(a2.id)
                }
            }
        }

        test("filtrering på AvtaleStatus") {
            database.runAndRollback {
                oppfolgingDomain.initialize()

                val avtaleAktiv = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.Aktiv,
                )
                repository.avtale.save(avtaleAktiv)

                val avtaleAvsluttet = AvtaleFixtures.AFT.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.Avsluttet,
                )
                repository.avtale.save(avtaleAvsluttet)

                val avtaleAvbrutt = AvtaleFixtures.gruppeAmo.copy(
                    id = UUID.randomUUID(),
                )
                repository.avtale.save(avtaleAvbrutt)
                queries.avtale.setStatus(
                    avtaleAvbrutt.id,
                    AvtaleStatusType.AVBRUTT,
                    LocalDateTime.now(),
                    listOf(AvbrytAvtaleAarsak.FEILREGISTRERING),
                    null,
                )

                val avtaleUtkast = AvtaleFixtures.oppfolging.copy(
                    id = UUID.randomUUID(),
                    status = AvtaleStatus.Utkast,
                )
                repository.avtale.save(avtaleUtkast)

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
                    val result = queries.avtale.getAllAvtaleDto(statuser = statuser)
                    result.items shouldContainExactlyIds expected
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
                        veilederinfo = Avtale.VeilederInfo(
                            navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(
                        veilederinfo = Avtale.VeilederInfo(
                            navEnheter = setOf(Innlandet.enhetsnummer, Sel.enhetsnummer),
                        ),
                    ),
                    AvtaleFixtures.VTA.copy(
                        veilederinfo = Avtale.VeilederInfo(
                            navEnheter = setOf(Innlandet.enhetsnummer),
                        ),
                    ),
                ),
            )

            database.runAndRollback {
                domain.initialize()

                queries.avtale.getAllAvtaleDto(
                    navEnheter = listOf(Innlandet.enhetsnummer),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }

                queries.avtale.getAllAvtaleDto(
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

            database.runAndRollback {
                domain.initialize()

                queries.avtale.getAllAvtaleDto(avtaletyper = listOf(Avtaletype.AVTALE)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(avtale1.id)
                }

                queries.avtale.getAllAvtaleDto(avtaletyper = listOf(Avtaletype.AVTALE, Avtaletype.OFFENTLIG_OFFENTLIG))
                    .should {
                        it.totalCount shouldBe 2
                        it.items shouldContainExactlyIds listOf(avtale1.id, avtale3.id)
                    }

                queries.avtale.getAllAvtaleDto(avtaletyper = listOf()).should {
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

            database.runAndRollback {
                domain.initialize()

                queries.avtale.getAllAvtaleDto(
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.id),
                ).should { (totalCount, items) ->
                    totalCount shouldBe 2
                    items shouldContainExactlyIds listOf(domain.avtaler[0].id, domain.avtaler[1].id)
                }

                queries.avtale.getAllAvtaleDto(
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.id, TiltakstypeFixtures.AFT.id),
                ).should { (totalCount) ->
                    totalCount shouldBe 3
                }
            }
        }

        test("Filtrering på tiltaksarrangørs navn gir treff") {
            val annenArrangor = Arrangor.Norsk.opprett(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("667543265"),
                organisasjonsform = ArrangorFixtures.underenhet1.organisasjonsform,
                navn = "Annen Arrangør AS",
                overordnetEnhet = ArrangorFixtures.underenhet1.overordnetEnhet,
            )

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(
                    TiltakstypeFixtures.Oppfolging,
                    TiltakstypeFixtures.AFT,
                    TiltakstypeFixtures.GruppeAmo,
                ),
                arrangorer = listOf(
                    ArrangorFixtures.hovedenhet,
                    ArrangorFixtures.underenhet1,
                    annenArrangor,
                ),
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = ArrangorFixtures.hovedenhet.id,
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(
                        id = UUID.randomUUID(),
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = ArrangorFixtures.underenhet1.id,
                        ),
                    ),
                    AvtaleFixtures.gruppeAmo.copy(
                        id = UUID.randomUUID(),
                        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                            hovedenhet = annenArrangor.id,
                        ),
                    ),
                ),
            )

            database.runAndRollback {
                domain.initialize()

                queries.avtale.getAllAvtaleDto(search = "enhet").totalCount shouldBe 2
                queries.avtale.getAllAvtaleDto(search = "annen").totalCount shouldBe 1
            }
        }

        test("Filtrering på personvern_bekreftet") {
            val domain = MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.oppfolging.copy(
                        personvern = Avtale.Personvern(
                            personopplysninger = emptySet(),
                            annetBeskrivelse = null,
                            erBekreftet = true,
                        ),
                    ),
                    AvtaleFixtures.AFT.copy(
                        id = UUID.randomUUID(),
                        personvern = Avtale.Personvern(
                            personopplysninger = emptySet(),
                            annetBeskrivelse = null,
                            erBekreftet = true,
                        ),
                    ),
                    AvtaleFixtures.gruppeAmo.copy(
                        id = UUID.randomUUID(),
                        personvern = Avtale.Personvern(
                            personopplysninger = emptySet(),
                            annetBeskrivelse = null,
                            erBekreftet = false,
                        ),
                    ),
                ),
            )

            database.runAndRollback {
                domain.initialize()

                queries.avtale.getAllAvtaleDto(personvernBekreftet = true).totalCount shouldBe 2
                queries.avtale.getAllAvtaleDto(personvernBekreftet = false).totalCount shouldBe 1
                queries.avtale.getAllAvtaleDto(personvernBekreftet = null).totalCount shouldBe 3
            }
        }
    }

    context("Sortering") {
        val arrangorA = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            navn = "alvdal",
            organisasjonsnummer = Organisasjonsnummer("987654321"),
            organisasjonsform = "BEDR",
        )
        val arrangorB = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            navn = "bjarne",
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            organisasjonsform = "BEDR",
        )
        val arrangorC = Arrangor.Norsk.opprett(
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
            database.runAndRollback {
                domain.initialize()

                val result = queries.avtale.getAllAvtaleDto(sortering = "navn-ascending")

                result.totalCount shouldBe 5
                result.items[0].navn shouldBe "Avtale hos Anders"
                result.items[1].navn shouldBe "Avtale hos Kjetil"
                result.items[2].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[3].navn shouldBe "Avtale hos Øyvind"
                result.items[4].navn shouldBe "Avtale hos Åse"
            }
        }

        test("Sortering på navn fra å-a sorterer korrekt") {
            database.runAndRollback {
                domain.initialize()

                val result = queries.avtale.getAllAvtaleDto(sortering = "navn-descending")

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

            database.runAndRollback {
                domain.initialize()

                val ascending = queries.avtale.getAllAvtaleDto(sortering = "arrangor-ascending")

                ascending.items[0].arrangor shouldBe alvdal
                ascending.items[1].arrangor shouldBe alvdal
                ascending.items[2].arrangor shouldBe bjarne
                ascending.items[3].arrangor shouldBe bjarne
                ascending.items[4].arrangor shouldBe chris

                val descending = queries.avtale.getAllAvtaleDto(sortering = "arrangor-descending")
                descending.items[0].arrangor shouldBe chris
                descending.items[1].arrangor shouldBe bjarne
                descending.items[2].arrangor shouldBe bjarne
                descending.items[3].arrangor shouldBe alvdal
                descending.items[4].arrangor shouldBe alvdal
            }
        }

        test("Sortering på sluttdato fra a-å sorterer korrekt") {
            database.runAndRollback {
                domain.initialize()

                val result = queries.avtale.getAllAvtaleDto(sortering = "sluttdato-descending")
                result.items[0].navn shouldBe "Avtale hos Ærfuglen Ærle"
                result.items[1].navn shouldBe "Avtale hos Kjetil"
                result.items[2].navn shouldBe "Avtale hos Anders"
                result.items[3].navn shouldBe "Avtale hos Øyvind"
                result.items[4].navn shouldBe "Avtale hos Åse"
            }
        }

        test("Sortering på sluttdato fra å-a sorterer korrekt") {
            database.runAndRollback {
                domain.initialize()

                val result = queries.avtale.getAllAvtaleDto(sortering = "sluttdato-ascending")
                result.items[0].navn shouldBe "Avtale hos Åse"
                result.items[1].navn shouldBe "Avtale hos Øyvind"
                result.items[2].navn shouldBe "Avtale hos Anders"
                result.items[3].navn shouldBe "Avtale hos Kjetil"
                result.items[4].navn shouldBe "Avtale hos Ærfuglen Ærle"
            }
        }
    }
})

private infix fun Collection<AvtaleDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}

private fun arrangorFromHovedenhet(hovedenhet: UUID): Avtale.Arrangor {
    return Avtale.Arrangor(
        hovedenhet = hovedenhet,
        underenheter = emptyList(),
        kontaktpersoner = emptyList(),
    )
}
