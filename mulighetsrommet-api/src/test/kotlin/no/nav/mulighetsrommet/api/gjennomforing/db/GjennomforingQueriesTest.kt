package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.EnkeltplassFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltakKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktperson
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

class GjennomforingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging),
        )

        test("lagre gjennomføring") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id) should {
                    it.shouldNotBeNull()
                    it.id shouldBe Oppfolging1.id
                    it.tiltakstype shouldBe Gjennomforing.Tiltakstype(
                        id = TiltakstypeFixtures.Oppfolging.id,
                        navn = TiltakstypeFixtures.Oppfolging.navn,
                        tiltakskode = Tiltakskode.OPPFOLGING,
                    )
                    it.navn shouldBe Oppfolging1.navn
                    it.arrangor shouldBe Gjennomforing.ArrangorUnderenhet(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                        kontaktpersoner = emptyList(),
                    )
                    it.startDato shouldBe Oppfolging1.startDato
                    it.sluttDato shouldBe Oppfolging1.sluttDato
                    it.status.type shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.apentForPamelding shouldBe true
                    it.antallPlasser shouldBe 12
                    it.avtaleId shouldBe Oppfolging1.avtaleId
                    it.administratorer shouldBe listOf(
                        GjennomforingGruppetiltak.Administrator(
                            navIdent = NavIdent("DD1"),
                            navn = "Donald Duck",
                        ),
                    )
                    it.kontorstruktur.shouldNotBeNull()
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                    it.kontaktpersoner shouldBe listOf()
                    it.oppmoteSted shouldBe "Munch museet"
                    it.faneinnhold shouldBe null
                    it.beskrivelse shouldBe null
                    it.arena?.tiltaksnummer shouldBe null
                    it.arena?.ansvarligNavEnhet shouldBe null
                }

                queries.gjennomforing.delete(Oppfolging1.id)

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id) shouldBe null
            }
        }

        test("Administratorer crud") {
            database.runAndRollback { session ->
                domain.setup(session)

                val gjennomforing = Oppfolging1.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                )
                queries.gjennomforing.upsertGruppetiltak(gjennomforing)

                queries.gjennomforing.getGruppetiltak(gjennomforing.id)?.administratorer.shouldContainExactlyInAnyOrder(
                    GjennomforingGruppetiltak.Administrator(
                        navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                        navn = "Donald Duck",
                    ),
                )
            }
        }

        test("navEnheter crud") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Lillehammer, Sel, Oslo),
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(navEnheter = setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer)),
                )
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().kontorstruktur.shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        it.kontorer.should { (first, second) ->
                            first.enhetsnummer shouldBe Gjovik.enhetsnummer
                            second.enhetsnummer shouldBe Sel.enhetsnummer
                        }
                    }

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(
                        navEnheter = setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                    ),
                )
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().kontorstruktur.shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        it.kontorer.shouldHaveSize(1).first().enhetsnummer shouldBe Lillehammer.enhetsnummer
                    }

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(navEnheter = setOf(Oslo.enhetsnummer)),
                )
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().kontorstruktur.shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Oslo.enhetsnummer
                        it.kontorer.shouldBeEmpty()
                    }

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(navEnheter = setOf()))
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().kontorstruktur.shouldBeEmpty()
            }
        }

        test("tilgjengelig for arrangør") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)
                queries.gjennomforing.setTilgjengeligForArrangorDato(Oppfolging1.id, LocalDate.now())
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().shouldNotBeNull().should {
                    it.tilgjengeligForArrangorDato shouldBe LocalDate.now()
                }
            }
        }

        test("Nav kontaktperson") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                                beskrivelse = "hei hei kontaktperson",
                            ),
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.MikkeMus.navIdent,
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id)?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = "hei hei kontaktperson",
                    ),
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD2"),
                        navn = "Mikke Mus",
                        mobilnummer = "48243214",
                        epost = "mikke.mus@nav.no",
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = null,
                    ),
                )

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(
                        kontaktpersoner = listOf(
                            GjennomforingKontaktpersonDbo(
                                navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                                beskrivelse = null,
                            ),
                        ),
                    ),
                )

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id)?.kontaktpersoner shouldBe listOf(
                    GjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
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
                ansvarligFor = listOf(),
            )
            val jens = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = ArrangorFixtures.hovedenhet.id,
                navn = "Jens",
                telefon = "22222224",
                epost = "jens@theshark.co.uk",
                beskrivelse = "beskrivelse2",
                ansvarligFor = listOf(),
            )

            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorKontaktpersoner = listOf(thomas, jens),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(arrangorKontaktpersoner = listOf(thomas.id)))
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactly listOf(toGjennomforingArrangorKontaktperson(thomas))
                }

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(arrangorKontaktpersoner = emptyList()))
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldHaveSize 0
                }

                queries.gjennomforing.upsertGruppetiltak(
                    Oppfolging1.copy(
                        arrangorKontaktpersoner = listOf(thomas.id, jens.id),
                    ),
                )
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toGjennomforingArrangorKontaktperson(thomas),
                        toGjennomforingArrangorKontaktperson(jens),
                    )
                }
            }
        }

        test("Publisert må settes eksplisitt") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id)?.publisert shouldBe false

                queries.gjennomforing.setPublisert(Oppfolging1.id, true)
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id)?.publisert shouldBe true
            }
        }

        test("skal sette åpent for påmelding") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe true

                queries.gjennomforing.setApentForPamelding(Oppfolging1.id, false)
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().apentForPamelding shouldBe false
            }
        }

        test("oppdater status") {
            database.runAndRollback { session ->
                domain.setup(session)

                val id = Oppfolging1.id
                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)

                val tidspunkt = LocalDate.now().atStartOfDay()
                queries.gjennomforing.setStatus(
                    id,
                    GjennomforingStatusType.AVBRUTT,
                    tidspunkt,
                    listOf(AvbrytGjennomforingAarsak.ANNET),
                    ":)",
                )
                queries.gjennomforing.getGruppetiltak(id).shouldNotBeNull().status shouldBe GjennomforingStatus.Avbrutt(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytGjennomforingAarsak.ANNET),
                    forklaring = ":)",
                )

                queries.gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.AVLYST,
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
                queries.gjennomforing.getGruppetiltak(id).shouldNotBeNull().status shouldBe GjennomforingStatus.Avlyst(
                    tidspunkt = tidspunkt,
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.GJENNOMFORES,
                    tidspunkt = tidspunkt,
                    aarsaker = null,
                    forklaring = null,
                )
                queries.gjennomforing.getGruppetiltak(id)
                    .shouldNotBeNull().status shouldBe GjennomforingStatus.Gjennomfores
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

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(faneinnhold = faneinnhold))

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
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

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(amoKategorisering = amo))
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe amo
                }

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1.copy(amoKategorisering = null))
                queries.gjennomforing.getGruppetiltak(Oppfolging1.id).shouldNotBeNull().should {
                    it.amoKategorisering shouldBe null
                }
            }
        }

        test("stengt hos arrangør lagres og hentes i periodens rekkefølge") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)
                queries.gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 7, 1)),
                    "Sommerferie",
                )
                queries.gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2024, 12, 1)),
                    "Forrige juleferie",
                )

                queries.gjennomforing.getGruppetiltak(Oppfolging1.id)
                    .shouldNotBeNull().stengt shouldContainExactly listOf(
                    GjennomforingGruppetiltak.StengtPeriode(
                        3,
                        LocalDate.of(2024, 12, 1),
                        LocalDate.of(2024, 12, 31),
                        "Forrige juleferie",
                    ),
                    GjennomforingGruppetiltak.StengtPeriode(
                        1,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31),
                        "Januarferie",
                    ),
                    GjennomforingGruppetiltak.StengtPeriode(
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

                queries.gjennomforing.upsertGruppetiltak(Oppfolging1)

                queries.gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )

                query {
                    queries.gjennomforing.setStengtHosArrangor(
                        Oppfolging1.id,
                        Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                        "Sommerferie",
                    )
                }.shouldBeLeft().shouldBeTypeOf<IntegrityConstraintViolation.ExclusionViolation>()
            }
        }
    }

    context("enkeltplasser") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(),
        )

        val enkelAmo1 = EnkeltplassFixtures.EnkelAmo.copy(
            navn = "Arena-navn",
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
        )
        val enkelAmo2 = EnkeltplassFixtures.EnkelAmo2

        test("lagre enkeltplass") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertEnkeltplass(enkelAmo1)

                queries.gjennomforing.getEnkeltplass(enkelAmo1.id).shouldNotBeNull().should {
                    it.id shouldBe enkelAmo1.id
                    it.tiltakstype shouldBe Gjennomforing.Tiltakstype(
                        id = TiltakstypeFixtures.EnkelAmo.id,
                        navn = TiltakstypeFixtures.EnkelAmo.navn,
                        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                    )
                    it.arrangor shouldBe Gjennomforing.ArrangorUnderenhet(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                        kontaktpersoner = listOf(),
                    )
                    it.arena?.tiltaksnummer.shouldBeNull()
                    it.arena?.ansvarligNavEnhet.shouldBeNull()
                    it.navn shouldBe "Arena-navn"
                    it.startDato shouldBe LocalDate.of(2025, 1, 1)
                    it.sluttDato.shouldBeNull()
                    it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                }

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = enkelAmo1.id,
                        tiltaksnummer = Tiltaksnummer("2025#1"),
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                queries.gjennomforing.getEnkeltplass(enkelAmo1.id).shouldNotBeNull().should {
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2025#1")
                    it.arena?.ansvarligNavEnhet?.enhetsnummer shouldBe "0400"
                }

                queries.gjennomforing.delete(enkelAmo1.id)

                queries.gjennomforing.getEnkeltplass(enkelAmo1.id) shouldBe null
            }
        }

        test("hent alle enkeltplasser") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsertEnkeltplass(enkelAmo1)
                queries.gjennomforing.upsertEnkeltplass(enkelAmo2)

                queries.gjennomforing.getAllEnkeltplass().totalCount shouldBe 2
                queries.gjennomforing.getAllEnkeltplass(tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo.id)).totalCount shouldBe 2
                queries.gjennomforing.getAllEnkeltplass(tiltakstyper = listOf(TiltakstypeFixtures.AFT.id)).totalCount shouldBe 0
            }
        }
    }

    context("free text search") {
        test("løpenummer og tiltaksnummer blir automatisk med i fritekstsøket") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    arrangorer = listOf(
                        ArrangorFixtures.hovedenhet,
                        ArrangorFixtures.underenhet1,
                    ),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                    ),
                ).setup(session)

                val lopenummer = queries.gjennomforing.getGruppetiltakOrError(Oppfolging1.id).lopenummer

                queries.gjennomforing.getAllGruppetiltakKompakt(search = lopenummer.value).items.shouldHaveSize(0)

                queries.gjennomforing.setFreeTextSearch(Oppfolging1.id, listOf("foo"))

                queries.gjennomforing.getAllGruppetiltakKompakt(search = lopenummer.value)
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAllGruppetiltakKompakt(search = lopenummer.aar.toString())
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAllGruppetiltakKompakt(search = lopenummer.lopenummer.toString())
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAllGruppetiltakKompakt(search = "foo")
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAllGruppetiltakKompakt(search = "bar").items.shouldHaveSize(0)
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

                queries.gjennomforing.getAllGruppetiltakKompakt(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe Oppfolging1.id
                }

                queries.gjennomforing.getAllGruppetiltakKompakt(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
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

                queries.gjennomforing.getAllGruppetiltakKompakt(avtaleId = AvtaleFixtures.oppfolging.id).items
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                queries.gjennomforing.getByAvtale(AvtaleFixtures.oppfolging.id)
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)

                queries.gjennomforing.getAllGruppetiltakKompakt(avtaleId = AvtaleFixtures.AFT.id).items
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                queries.gjennomforing.getByAvtale(AvtaleFixtures.AFT.id)
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
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

                queries.gjennomforing.getAllGruppetiltakKompakt(sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate)
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

                queries.gjennomforing.getAllGruppetiltakKompakt(
                    administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                )
                    .totalCount shouldBe 2

                queries.gjennomforing.getAllGruppetiltakKompakt(
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

                queries.gjennomforing.getAllGruppetiltakKompakt(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 1
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id)
                    }

                queries.gjennomforing.getAllGruppetiltakKompakt(
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

                queries.gjennomforing.getAllGruppetiltakKompakt(navEnheter = listOf(Gjovik.enhetsnummer))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                    }

                queries.gjennomforing.getAllGruppetiltakKompakt(
                    navEnheter = listOf(
                        Lillehammer.enhetsnummer,
                        Sel.enhetsnummer,
                    ),
                )
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                    }

                queries.gjennomforing.getAllGruppetiltakKompakt(navEnheter = listOf(Innlandet.enhetsnummer))
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

            (1..10).forEach {
                queries.gjennomforing.upsertGruppetiltak(
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
                val (totalCount, items) = queries.gjennomforing.getAllGruppetiltakKompakt(pagination)

                items.size shouldBe expectedSize
                items.firstOrNull()?.navn shouldBe expectedFirst
                items.lastOrNull()?.navn shouldBe expectedLast

                totalCount shouldBe expectedTotalCount
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
            ansvarligFor = listOf(),
        )

        val kontaktperson2 = ArrangorKontaktperson(
            id = UUID.randomUUID(),
            arrangorId = ArrangorFixtures.underenhet1.id,
            navn = "Gibli Bobli",
            telefon = "",
            epost = "test@test.no",
            beskrivelse = "",
            ansvarligFor = listOf(),
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

                queries.gjennomforing.getGruppetiltak(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson1.id)
                }
                queries.gjennomforing.getGruppetiltak(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
                }

                queries.gjennomforing.frikobleKontaktpersonFraGjennomforing(kontaktperson1.id, Oppfolging1.id)

                queries.gjennomforing.getGruppetiltak(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.shouldBeEmpty()
                }
                queries.gjennomforing.getGruppetiltak(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                    it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
                }
            }
        }
    }
})

private fun toGjennomforingArrangorKontaktperson(kontaktperson: ArrangorKontaktperson) = GjennomforingGruppetiltak.ArrangorKontaktperson(
    id = kontaktperson.id,
    navn = kontaktperson.navn,
    beskrivelse = kontaktperson.beskrivelse,
    telefon = kontaktperson.telefon,
    epost = kontaktperson.epost,
)

private infix fun Collection<GjennomforingGruppetiltakKompakt>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
