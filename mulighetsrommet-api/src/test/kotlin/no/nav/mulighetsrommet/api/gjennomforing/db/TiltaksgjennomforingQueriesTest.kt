package no.nav.mulighetsrommet.api.gjennomforing.db

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
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingKontaktperson
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.GjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging),
        )

        test("lagre gjennomføring") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TiltaksgjennomforingQueries(session)

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
                    it.navEnheter shouldBe listOf(Gjovik)
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                    it.kontaktpersoner shouldBe listOf()
                    it.stedForGjennomforing shouldBe "Oslo"
                    it.navRegion shouldBe Innlandet
                    it.faneinnhold shouldBe null
                    it.beskrivelse shouldBe null
                    it.createdAt shouldNotBe null
                }

                queries.delete(Oppfolging1.id)

                queries.get(Oppfolging1.id) shouldBe null
            }
        }

        test("Administratorer crud") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                val gjennomforing = Oppfolging1.copy(
                    administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
                )
                queries.upsert(gjennomforing)

                queries.get(gjennomforing.id)?.administratorer.shouldContainExactlyInAnyOrder(
                    GjennomforingDto.Administrator(
                        navIdent = NavAnsattFixture.ansatt1.navIdent,
                        navn = "Donald Duck",
                    ),
                )
            }
        }

        test("navEnheter crud") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    enheter = listOf(Innlandet, Gjovik, Lillehammer, Sel),
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.upsert(
                    Oppfolging1.copy(navEnheter = listOf(Gjovik.enhetsnummer, Sel.enhetsnummer)),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.navEnheter shouldContainExactlyInAnyOrder listOf(Gjovik, Sel)
                }

                queries.upsert(
                    Oppfolging1.copy(navEnheter = listOf(Gjovik.enhetsnummer, Lillehammer.enhetsnummer)),
                )
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.navEnheter shouldContainExactlyInAnyOrder listOf(Gjovik, Lillehammer)
                }

                queries.upsert(Oppfolging1.copy(navEnheter = listOf()))
                queries.get(Oppfolging1.id).shouldNotBeNull().should {
                    it.navEnheter.shouldBeEmpty()
                }
            }
        }

        test("tilgjengelig for arrangør") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.setTilgjengeligForArrangorFraOgMedDato(Oppfolging1.id, LocalDate.now())
                queries.get(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.tilgjengeligForArrangorFraOgMedDato shouldBe LocalDate.now()
                }
            }
        }

        test("Nav kontaktperson") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.upsert(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.ansatt1.navIdent,
                                navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                                beskrivelse = "hei hei kontaktperson",
                            ),
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.ansatt2.navIdent,
                                navEnheter = listOf(NavAnsattFixture.ansatt2.hovedenhet),
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.get(Oppfolging1.id)?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    TiltaksgjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        navEnheter = listOf("0400"),
                        hovedenhet = "0400",
                        beskrivelse = "hei hei kontaktperson",
                    ),
                    TiltaksgjennomforingKontaktperson(
                        navIdent = NavIdent("DD2"),
                        navn = "Dolly Duck",
                        mobilnummer = "48243214",
                        epost = "dolly.duck@nav.no",
                        navEnheter = listOf("0400"),
                        hovedenhet = "0400",
                        beskrivelse = null,
                    ),
                )

                queries.upsert(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.ansatt1.navIdent,
                                navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.get(Oppfolging1.id)?.kontaktpersoner shouldBe listOf(
                    TiltaksgjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        navEnheter = listOf("0400"),
                        hovedenhet = "0400",
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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

                queries.upsert(Oppfolging1)

                queries.get(Oppfolging1.id)?.publisert shouldBe false

                queries.setPublisert(Oppfolging1.id, true)
                queries.get(Oppfolging1.id)?.publisert shouldBe true
            }
        }

        test("skal sette åpent for påmelding") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.upsert(Oppfolging1)
                queries.get(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe true

                queries.setApentForPamelding(Oppfolging1.id, false)
                queries.get(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe false
            }
        }

        test("avpubliseres når gjennomføring blir avsluttet") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

                queries.getAll().should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe Oppfolging1.id
                }
            }
        }

        // TODO: kan all logikk basert på opphav fjernes? Trenger vel ikke egen logikk basert på dette lengre?
        test("filtrering på opphav") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(Oppfolging1, Oppfolging2),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.setOpphav(Oppfolging1.id, ArenaMigrering.Opphav.ARENA)

                queries.getAll(opphav = null).should {
                    it.totalCount shouldBe 2
                }

                queries.getAll(opphav = ArenaMigrering.Opphav.ARENA).should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe Oppfolging1.id
                }

                queries.getAll(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE).should {
                    it.totalCount shouldBe 1
                    it.items[0].id shouldBe Oppfolging2.id
                }
            }
        }

        test("filtrering på avtale") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT),
                    gjennomforinger = listOf(Oppfolging1, AFT1),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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
                    enheter = listOf(Innlandet, Lillehammer, Gjovik),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf(Lillehammer.enhetsnummer)),
                        Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf(Gjovik.enhetsnummer)),
                        Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf()),
                    ),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.updateArenaData(
                    id = domain.gjennomforinger[2].id,
                    tiltaksnummer = "2024/1",
                    arenaAnsvarligEnhet = Lillehammer.enhetsnummer,
                )

                queries.getAll(navEnheter = listOf(Lillehammer.enhetsnummer)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(domain.gjennomforinger[0].id, domain.gjennomforinger[2].id)
                }
            }
        }

        test("administrator") {
            database.runAndRollback { session ->
                val domain = MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
                        ),
                        Oppfolging1.copy(
                            id = UUID.randomUUID(),
                            administratorer = listOf(
                                NavAnsattFixture.ansatt1.navIdent,
                                NavAnsattFixture.ansatt2.navIdent,
                            ),
                        ),
                    ),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

                queries.getAll(administratorNavIdent = NavAnsattFixture.ansatt1.navIdent)
                    .totalCount shouldBe 2

                queries.getAll(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent)
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

                val queries = TiltaksgjennomforingQueries(session)

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
                    enheter = listOf(Innlandet, Gjovik, Lillehammer, Sel),
                    avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(navEnheter = listOf(Gjovik.enhetsnummer)),
                        VTA1.copy(navEnheter = listOf(Lillehammer.enhetsnummer)),
                        AFT1.copy(navEnheter = listOf(Sel.enhetsnummer, Gjovik.enhetsnummer)),
                    ),
                ).setup(session)

                val queries = TiltaksgjennomforingQueries(session)

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

            val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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

                val queries = TiltaksgjennomforingQueries(session)

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
