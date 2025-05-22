package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.IT
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktperson
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging),
        )

        test("lagre gjennomføring") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)

                queries.get(Oppfolging1.id) should {
                    it.shouldNotBeNull()
                    it.id shouldBe Oppfolging1.id
                    it.tiltakstype shouldBe GjennomforingDto.Tiltakstype(
                        id = TiltakstypeFixtures.Oppfolging.id,
                        navn = TiltakstypeFixtures.Oppfolging.navn,
                        tiltakskode = Tiltakskode.OPPFOLGING,
                    )
                    it.navn shouldBe Oppfolging1.navn
                    it.tiltaksnummer shouldBe null
                    it.arrangor shouldBe GjennomforingDto.ArrangorUnderenhet(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                        kontaktpersoner = emptyList(),
                    )
                    it.startDato shouldBe Oppfolging1.startDato
                    it.sluttDato shouldBe Oppfolging1.sluttDato
                    it.arenaAnsvarligEnhet shouldBe null
                    it.status.status shouldBe GjennomforingStatus.GJENNOMFORES
                    it.apentForPamelding shouldBe true
                    it.antallPlasser shouldBe 12
                    it.avtaleId shouldBe Oppfolging1.avtaleId
                    it.administratorer shouldBe listOf(
                        GjennomforingDto.Administrator(
                            navIdent = NavIdent("DD1"),
                            navn = "Donald Duck",
                        ),
                    )
                    it.kontorstruktur shouldBe listOf(Kontorstruktur(region = Innlandet, kontorer = listOf(Gjovik)))
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                    it.kontaktpersoner shouldBe listOf()
                    it.stedForGjennomforing shouldBe "Oslo"
                    it.faneinnhold shouldBe null
                    it.beskrivelse shouldBe null
                }

                queries.delete(Oppfolging1.id)

                queries.get(Oppfolging1.id) shouldBe null
            }
        }

        test("Administratorer crud") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                val gjennomforing = Oppfolging1.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                )
                queries.upsert(gjennomforing)

                queries.get(gjennomforing.id)?.administratorer.shouldContainExactlyInAnyOrder(
                    GjennomforingDto.Administrator(
                        navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                        navn = "Donald Duck",
                    ),
                )
            }
        }

        test("navEnheter crud") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Lillehammer, Sel, IT),
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(
                    Oppfolging1.copy(navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer)),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].region shouldBe Innlandet
                }
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].kontorer shouldContainExactlyInAnyOrder setOf(Gjovik, Sel)
                }

                queries.upsert(
                    Oppfolging1.copy(
                        navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Lillehammer.enhetsnummer),
                    ),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].region shouldBe Innlandet
                }
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].kontorer shouldContainExactlyInAnyOrder setOf(Gjovik, Lillehammer)
                }

                queries.upsert(
                    Oppfolging1.copy(navEnheter = setOf(IT.enhetsnummer)),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].region shouldBe IT
                }
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.kontorstruktur[0].kontorer.shouldBeEmpty()
                }

                queries.upsert(Oppfolging1.copy(navEnheter = setOf()))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.kontorstruktur.shouldBeEmpty()
                }
            }
        }

        test("tilgjengelig for arrangør") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.setTilgjengeligForArrangorDato(Oppfolging1.id, LocalDate.now())
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.tilgjengeligForArrangorDato shouldBe LocalDate.now()
                }
            }
        }

        test("Nav kontaktperson") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                                navEnheter = listOf(NavAnsattFixture.DonaldDuck.hovedenhet),
                                beskrivelse = "hei hei kontaktperson",
                            ),
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.MikkeMus.navIdent,
                                navEnheter = listOf(NavAnsattFixture.MikkeMus.hovedenhet),
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.get(Oppfolging1.id)?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        navEnheter = listOf(NavEnhetNummer("0400")),
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = "hei hei kontaktperson",
                    ),
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD2"),
                        navn = "Mikke Mus",
                        mobilnummer = "48243214",
                        epost = "mikke.mus@nav.no",
                        navEnheter = listOf(NavEnhetNummer("0400")),
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = null,
                    ),
                )

                queries.upsert(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                                navEnheter = listOf(NavAnsattFixture.DonaldDuck.hovedenhet),
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.get(Oppfolging1.id)?.kontaktpersoner shouldBe listOf(
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        navEnheter = listOf(NavEnhetNummer("0400")),
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = null,
                    ),
                )
            }
        }

        test("arrangør kontaktperson") {
            val thomas = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
                navn = "Thomas",
                telefon = "22222222",
                epost = "thomas@thetrain.co.uk",
                beskrivelse = "beskrivelse",
            )
            val jens = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
                navn = "Jens",
                telefon = "22222224",
                epost = "jens@theshark.co.uk",
                beskrivelse = "beskrivelse2",
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(thomas, jens),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1.copy(arrangorKontaktpersoner = listOf(thomas.id)))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactly listOf(thomas)
                }

                queries.upsert(Oppfolging1.copy(arrangorKontaktpersoner = emptyList()))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldHaveSize 0
                }

                queries.upsert(
                    Oppfolging1.copy(
                        arrangorKontaktpersoner = listOf(thomas.id, jens.id),
                    ),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(thomas, jens)
                }
            }
        }

        test("Publisert må settes eksplisitt") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)

                queries.get(Oppfolging1.id)?.publisert shouldBe false

                queries.setPublisert(Oppfolging1.id, true)
                queries.get(Oppfolging1.id)?.publisert shouldBe true
            }
        }

        test("skal sette åpent for påmelding") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.get(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe true

                queries.setApentForPamelding(Oppfolging1.id, false)
                queries.get(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe false
            }
        }

        test("avpubliseres når gjennomføring blir avsluttet") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.setPublisert(Oppfolging1.id, true)
                queries.setApentForPamelding(Oppfolging1.id, true)

                queries.setAvsluttet(
                    Oppfolging1.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                )

                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.publisert shouldBe false
                    it.apentForPamelding shouldBe false
                }
            }
        }

        test("lagre faneinnhold") {
            val faneinnhold = Json.decodeFromString<Faneinnhold>(
                """ {
                "forHvem": [{
                    "_key": "edcad230384e",
                    "markDefs": [],
                    "children": [
                    {
                        "marks": [],
                        "text": "Oppl\u00e6ringen er beregnet p\u00e5 arbeidss\u00f8kere som \u00f8nsker og er egnet til \u00e5 ta arbeid som maskinf\u00f8rer. Deltakerne b\u00f8r ha f\u00f8rerkort kl. B.",
                        "_key": "0e5849bf79a70",
                        "_type": "span"
                    }
                    ],
                    "_type": "block",
                    "style": "normal"
                }]
            }
            """,
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1.copy(faneinnhold = faneinnhold))

                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.faneinnhold.shouldNotBeNull().forHvem shouldBe faneinnhold.forHvem
                }
            }
        }

        test("lagre amoKategoriserng") {
            val amo = AmoKategorisering.Norskopplaering(
                norskprove = true,
                innholdElementer = listOf(
                    AmoKategorisering.InnholdElement.ARBEIDSMARKEDSKUNNSKAP,
                    AmoKategorisering.InnholdElement.PRAKSIS,
                ),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1.copy(amoKategorisering = amo))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe amo
                }

                queries.upsert(Oppfolging1.copy(amoKategorisering = null))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe null
                }
            }
        }

        test("stengt hos arrangør lagres og hentes i periodens rekkefølge") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )
                queries.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 7, 1)),
                    "Sommerferie",
                )
                queries.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2024, 12, 1)),
                    "Forrige juleferie",
                )

                queries.get(Oppfolging1.id).shouldNotBeNull().stengt shouldContainExactly listOf(
                    GjennomforingDto.StengtPeriode(
                        3,
                        LocalDate.of(2024, 12, 1),
                        LocalDate.of(2024, 12, 31),
                        "Forrige juleferie",
                    ),
                    GjennomforingDto.StengtPeriode(
                        1,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31),
                        "Januarferie",
                    ),
                    GjennomforingDto.StengtPeriode(
                        2,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 7, 31),
                        "Sommerferie",
                    ),
                )
            }
        }

        test("tillater ikke lagring av overlappende perioder med stengt hos arrangør") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.upsert(Oppfolging1)

                queries.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )

                query {
                    queries.setStengtHosArrangor(
                        Oppfolging1.id,
                        Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        "Sommerferie",
                    )
                }.shouldBeLeft().shouldBeTypeOf<IntegrityConstraintViolation.ExclusionViolation>()
            }
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        test("filtrering på arrangør") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(
                        ArrangorFixtures.hovedenhet,
                        ArrangorFixtures.underenhet1,
                        ArrangorFixtures.underenhet2,
                    ),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                        Oppfolging2.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe Oppfolging1.id
                }

                queries.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe Oppfolging2.id
                }
            }
        }

        test("søk på tiltaksarrangørs navn") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(
                        ArrangorFixtures.hovedenhet,
                        ArrangorFixtures.underenhet1.copy(navn = "Underenhet Bergen"),
                        ArrangorFixtures.underenhet2.copy(navn = "Underenhet Ålesund"),
                    ),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                        Oppfolging2.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(search = "bergen").should {
                    it.items.size shouldBe 1
                    it.items[0].arrangor.navn shouldBe "Underenhet Bergen"
                }

                queries.getAll(search = "under").should {
                    it.items.size shouldBe 2
                }
            }
        }

        test("skal migreres henter kun der tiltakstypen har egen tiltakskode") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.EnkelAmo),
                    gjennomforinger = listOf(Oppfolging1, EnkelAmo1),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll().should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe Oppfolging1.id
                }
            }
        }

        test("filtrering på avtale") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT),
                    gjennomforinger = listOf(Oppfolging1, AFT1),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(avtaleId = AvtaleFixtures.oppfolging.id)
                    .items.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)

                queries.getAll(avtaleId = AvtaleFixtures.AFT.id)
                    .items.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }
        }

        test("filtrer vekk gjennomføringer basert på sluttdato") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(sluttDato = LocalDate.of(2023, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 6, 29)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2022, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = null),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate)
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 3
                        gjennomforinger.map { it.sluttDato } shouldContainExactlyInAnyOrder listOf(
                            LocalDate.of(2023, 12, 31),
                            LocalDate.of(2023, 6, 29),
                            null,
                        )
                    }
            }
        }

        test("filtrer på nav_enhet") {
            database.runAndRollback { session ->
                val domain = MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Lillehammer, Gjovik),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            navEnheter = setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                        ),
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                        ),
                        Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = setOf()),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.updateArenaData(
                    id = domain.gjennomforinger[2].id,
                    tiltaksnummer = "2024/1",
                    arenaAnsvarligEnhet = Lillehammer.enhetsnummer.value,
                )

                queries.getAll(navEnheter = listOf(Lillehammer.enhetsnummer)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(domain.gjennomforinger[0].id, domain.gjennomforinger[2].id)
                }
            }
        }

        test("administrator og koordinator filtrering") {
            database.runAndRollback { session ->
                val domain = MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                        ),
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            administratorer = listOf(
                                NavAnsattFixture.DonaldDuck.navIdent,
                                NavAnsattFixture.MikkeMus.navIdent,
                            ),
                        ),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(
                    administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                )
                    .totalCount shouldBe 2

                queries.getAll(
                    administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                    koordinatorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                )
                    .should {
                        it.totalCount shouldBe 1
                        it.items shouldContainExactlyIds listOf(domain.gjennomforinger[1].id)
                    }
            }
        }

        test("filtrering på tiltakstype") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
                    gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 1
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id)
                    }

                queries.getAll(
                    tiltakstypeIder = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.VTA.id),
                ).should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 2
                    gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                }
            }
        }

        test("filtrering på Nav-enhet") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Lillehammer, Sel),
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer)),
                        VTA1.copy(navEnheter = setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer)),
                        AFT1.copy(navEnheter = setOf(Innlandet.enhetsnummer, Sel.enhetsnummer, Gjovik.enhetsnummer)),
                    ),
                ).setup(session)

                val queries = GjennomforingQueries(session)

                queries.getAll(navEnheter = listOf(Gjovik.enhetsnummer))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                    }

                queries.getAll(navEnheter = listOf(Lillehammer.enhetsnummer, Sel.enhetsnummer))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                    }

                queries.getAll(navEnheter = listOf(Innlandet.enhetsnummer))
                    .should { (totalCount) ->
                        totalCount shouldBe 3
                    }
            }
        }
    }

    test("pagination") {
        database.runAndRollback { session ->
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).setup(session)

            val queries = GjennomforingQueries(session)

            (1..10).forEach {
                queries.upsert(
                    Oppfolging1.copy(id = UUID.randomUUID(), navn = "$it".padStart(2, '0')),
                )
            }

            forAll(
                row(Pagination.all(), 10, "01", "10", 10),
                row(Pagination.of(page = 1, size = 20), 10, "01", "10", 10),
                row(Pagination.of(page = 1, size = 2), 2, "01", "02", 10),
                row(Pagination.of(page = 3, size = 2), 2, "05", "06", 10),
                row(Pagination.of(page = 3, size = 4), 2, "09", "10", 10),
                row(Pagination.of(page = 2, size = 20), 0, null, null, 0),
            ) { pagination, expectedSize, expectedFirst, expectedLast, expectedTotalCount ->
                val (totalCount, items) = queries.getAll(pagination)

                items.size shouldBe expectedSize
                items.firstOrNull()?.navn shouldBe expectedFirst
                items.lastOrNull()?.navn shouldBe expectedLast

                totalCount shouldBe expectedTotalCount
            }
        }
    }

    context("status på gjennomføring") {
        val dagensDato = LocalDate.of(2024, 6, 1)
        val enManedFrem = dagensDato.plusMonths(1)
        val enManedTilbake = dagensDato.minusMonths(1)
        val toManederFrem = dagensDato.plusMonths(2)
        val toManederTilbake = dagensDato.minusMonths(2)

        test("status AVLYST, AVBRUTT, AVSLUTTET utledes fra avsluttet-tidspunkt") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(avtaler = listOf(AvtaleFixtures.AFT)).setup(session)

                val queries = GjennomforingQueries(session)

                forAll(
                    row(enManedTilbake, enManedFrem, enManedTilbake.minusDays(1), GjennomforingStatus.AVLYST),
                    row(enManedTilbake, null, enManedTilbake.minusDays(1), GjennomforingStatus.AVLYST),
                    row(enManedFrem, toManederFrem, dagensDato, GjennomforingStatus.AVLYST),
                    row(dagensDato, toManederFrem, dagensDato, GjennomforingStatus.AVBRUTT),
                    row(enManedTilbake, enManedFrem, enManedTilbake.plusDays(3), GjennomforingStatus.AVBRUTT),
                    row(enManedTilbake, enManedFrem, enManedFrem, GjennomforingStatus.AVBRUTT),
                    row(enManedTilbake, null, enManedFrem, GjennomforingStatus.AVBRUTT),
                    row(enManedFrem, toManederFrem, enManedFrem.plusMonths(2), GjennomforingStatus.AVSLUTTET),
                    row(enManedTilbake, enManedFrem, enManedFrem.plusDays(1), GjennomforingStatus.AVSLUTTET),
                ) { startDato, sluttDato, avbruttDato, expectedStatus ->
                    queries.upsert(AFT1.copy(startDato = startDato, sluttDato = sluttDato))

                    queries.setAvsluttet(
                        AFT1.id,
                        avbruttDato.atStartOfDay(),
                        AvbruttAarsak.Feilregistrering,
                    )

                    queries.get(AFT1.id).shouldNotBeNull().status.status shouldBe expectedStatus
                }
            }
        }

        test("hvis ikke avsluttet så blir status GJENNOMFORES") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(avtaler = listOf(AvtaleFixtures.AFT)).setup(session)

                val queries = GjennomforingQueries(session)

                forAll(
                    row(toManederTilbake, enManedTilbake, GjennomforingStatus.GJENNOMFORES),
                    row(enManedTilbake, null, GjennomforingStatus.GJENNOMFORES),
                    row(dagensDato, dagensDato, GjennomforingStatus.GJENNOMFORES),
                    row(enManedFrem, toManederFrem, GjennomforingStatus.GJENNOMFORES),
                    row(enManedFrem, null, GjennomforingStatus.GJENNOMFORES),
                ) { startDato, sluttDato, status ->
                    queries.upsert(AFT1.copy(startDato = startDato, sluttDato = sluttDato))

                    queries.get(AFT1.id).shouldNotBeNull().status.status shouldBe status
                }
            }
        }
    }

    context("Frikoble kontaktperson fra arrangør") {
        val kontaktperson1 = ArrangorKontaktperson(
            id = UUID.randomUUID(),
            arrangorId = ArrangorFixtures.underenhet1.id,
            navn = "Aran Goran",
            telefon = "",
            epost = "test@test.no",
            beskrivelse = "",
        )

        val kontaktperson2 = ArrangorKontaktperson(
            id = UUID.randomUUID(),
            arrangorId = ArrangorFixtures.underenhet1.id,
            navn = "Gibli Bobli",
            telefon = "",
            epost = "test@test.no",
            beskrivelse = "",
        )

        val testDomain = MulighetsrommetTestDomain(
            arrangorKontaktpersoner = listOf(kontaktperson1, kontaktperson2),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                Oppfolging1.copy(arrangorKontaktpersoner = listOf(kontaktperson1.id)),
                Oppfolging2.copy(arrangorKontaktpersoner = listOf(kontaktperson2.id)),
            ),
        )

        test("Skal fjerne kontaktperson fra koblingstabell") {
            database.runAndRollback { session ->
                testDomain.setup(session)

                val queries = GjennomforingQueries(session)

                queries.get(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson1.id)
                }
                queries.get(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
                }

                queries.frikobleKontaktpersonFraGjennomforing(kontaktperson1.id, Oppfolging1.id)

                queries.get(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.shouldBeEmpty()
                }
                queries.get(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
                }
            }
        }
    }
})

private infix fun Collection<GjennomforingDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
