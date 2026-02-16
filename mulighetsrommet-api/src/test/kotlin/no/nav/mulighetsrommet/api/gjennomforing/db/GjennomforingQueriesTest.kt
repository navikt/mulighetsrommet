package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
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
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.ArenaEnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
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
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
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

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    context("avtale") {
        test("lagre gjennomføring") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)

                queries.gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id) should {
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
                    )
                    it.startDato shouldBe Oppfolging1.startDato
                    it.sluttDato shouldBe Oppfolging1.sluttDato
                    it.status.type shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.antallPlasser shouldBe 12
                    it.avtaleId shouldBe Oppfolging1.avtaleId
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                    it.arena?.tiltaksnummer shouldBe null
                    it.arena?.ansvarligNavEnhet shouldBe null
                    it.apentForPamelding shouldBe true
                    it.kontorstruktur.shouldNotBeNull()
                }

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldBe emptyList()
                    it.administratorer shouldBe listOf()
                    it.kontorstruktur.shouldNotBeNull()
                    it.kontaktpersoner shouldBe listOf()
                    it.oppmoteSted shouldBe "Munch museet"
                    it.faneinnhold shouldBe null
                    it.beskrivelse shouldBe null
                }

                queries.gjennomforing.delete(Oppfolging1.id)

                queries.gjennomforing.getGjennomforing(Oppfolging1.id) shouldBe null
            }
        }

        test("Administratorer crud") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setAdministratorer(Oppfolging1.id, setOf(NavAnsattFixture.DonaldDuck.navIdent))

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).administratorer.shouldContainExactlyInAnyOrder(
                    GjennomforingAvtaleDetaljer.Administrator(
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
                ).setup(session)

                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer),
                )
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
                    .kontorstruktur
                    .shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        it.kontorer.should { (first, second) ->
                            first.enhetsnummer shouldBe Gjovik.enhetsnummer
                            second.enhetsnummer shouldBe Sel.enhetsnummer
                        }
                    }

                queries.gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                )
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
                    .kontorstruktur
                    .shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        it.kontorer.shouldHaveSize(1).first().enhetsnummer shouldBe Lillehammer.enhetsnummer
                    }

                queries.gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Oslo.enhetsnummer),
                )
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
                    .kontorstruktur
                    .shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Oslo.enhetsnummer
                        it.kontorer.shouldBeEmpty()
                    }

                queries.gjennomforing.setNavEnheter(Oppfolging1.id, setOf())
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontorstruktur.shouldBeEmpty()
            }
        }

        test("tilgjengelig for arrangør") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setTilgjengeligForArrangorDato(Oppfolging1.id, LocalDate.now())
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.tilgjengeligForArrangorDato shouldBe LocalDate.now()
                }
            }
        }

        test("Nav kontaktperson") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setKontaktpersoner(
                    Oppfolging1.id,
                    setOf(
                        GjennomforingKontaktpersonDbo(
                            navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                            beskrivelse = "hei hei kontaktperson",
                        ),
                        GjennomforingKontaktpersonDbo(
                            navIdent = NavAnsattFixture.MikkeMus.navIdent,
                            beskrivelse = null,
                        ),
                    ),
                )

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    GjennomforingAvtaleDetaljer.GjennomforingKontaktperson(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                        mobilnummer = "12345678",
                        epost = "donald.duck@nav.no",
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = "hei hei kontaktperson",
                    ),
                    GjennomforingAvtaleDetaljer.GjennomforingKontaktperson(
                        navIdent = NavIdent("DD2"),
                        navn = "Mikke Mus",
                        mobilnummer = "48243214",
                        epost = "mikke.mus@nav.no",
                        hovedenhet = NavEnhetNummer("0400"),
                        beskrivelse = null,
                    ),
                )

                queries.gjennomforing.setKontaktpersoner(
                    Oppfolging1.id,
                    setOf(
                        GjennomforingKontaktpersonDbo(
                            navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                            beskrivelse = null,
                        ),
                    ),
                )

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontaktpersoner shouldBe listOf(
                    GjennomforingAvtaleDetaljer.GjennomforingKontaktperson(
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
                ).setup(session)

                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf(thomas.id))
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactly listOf(toGjennomforingArrangorKontaktperson(thomas))
                }

                queries.gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf())
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldHaveSize 0
                }

                queries.gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf(thomas.id, jens.id))
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toGjennomforingArrangorKontaktperson(thomas),
                        toGjennomforingArrangorKontaktperson(jens),
                    )
                }

                queries.gjennomforing.frikobleKontaktpersonFraGjennomforing(thomas.id, Oppfolging1.id)

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toGjennomforingArrangorKontaktperson(jens),
                    )
                }
            }
        }

        test("Publisert må settes eksplisitt") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).publisert shouldBe false

                queries.gjennomforing.setPublisert(Oppfolging1.id, true)
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).publisert shouldBe true
            }
        }

        test("skal sette åpent for påmelding") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).apentForPamelding shouldBe true

                queries.gjennomforing.setApentForPamelding(Oppfolging1.id, false)
                queries.gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).apentForPamelding shouldBe false
            }
        }

        test("oppdater status") {
            database.runAndRollback {
                val id = Oppfolging1.id
                queries.gjennomforing.upsert(Oppfolging1)

                val tidspunkt = LocalDate.now()
                queries.gjennomforing.setStatus(
                    id,
                    GjennomforingStatusType.AVBRUTT,
                    tidspunkt,
                    listOf(AvbrytGjennomforingAarsak.ANNET),
                    ":)",
                )
                queries.gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingStatus.Avbrutt(
                    aarsaker = listOf(AvbrytGjennomforingAarsak.ANNET),
                    forklaring = ":)",
                )

                queries.gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.AVLYST,
                    sluttDato = tidspunkt,
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
                queries.gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingStatus.Avlyst(
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                queries.gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.GJENNOMFORES,
                    sluttDato = tidspunkt,
                    aarsaker = null,
                    forklaring = null,
                )
                queries.gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingStatus.Gjennomfores
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

            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1.copy(faneinnhold = faneinnhold))

                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
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

            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
                queries.gjennomforing.setAmoKategorisering(Oppfolging1.id, amo)
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.amoKategorisering shouldBe amo
                }

                queries.gjennomforing.setAmoKategorisering(Oppfolging1.id, null)
                queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.amoKategorisering shouldBe null
                }
            }
        }

        test("stengt hos arrangør lagres og hentes i periodens rekkefølge") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)
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

                queries.gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).stengt shouldContainExactly listOf(
                    GjennomforingAvtale.StengtPeriode(
                        3,
                        LocalDate.of(2024, 12, 1),
                        LocalDate.of(2024, 12, 31),
                        "Forrige juleferie",
                    ),
                    GjennomforingAvtale.StengtPeriode(
                        1,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31),
                        "Januarferie",
                    ),
                    GjennomforingAvtale.StengtPeriode(
                        2,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 7, 31),
                        "Sommerferie",
                    ),
                )
            }
        }

        test("tillater ikke lagring av overlappende perioder med stengt hos arrangør") {
            database.runAndRollback {
                queries.gjennomforing.upsert(Oppfolging1)

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

    context("arena") {
        val arenaEnkelAmo1 = ArenaEnkelAmo.copy(
            navn = "Arenanavn",
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = LocalDate.of(2025, 2, 1),
            status = GjennomforingStatusType.GJENNOMFORES,
            deltidsprosent = 100.0,
            antallPlasser = 10,
            arenaTiltaksnummer = Tiltaksnummer("2021#1234"),
            arenaAnsvarligEnhet = "1234",
        )

        test("lagre arenatiltak") {
            database.runAndRollback {
                queries.gjennomforing.upsert(arenaEnkelAmo1)

                queries.gjennomforing.getGjennomforingArenaOrError(arenaEnkelAmo1.id).should {
                    it.id shouldBe arenaEnkelAmo1.id
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
                    )
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2021#1234")
                    it.arena?.ansvarligNavEnhet shouldBe "1234"
                    it.navn shouldBe "Arenanavn"
                    it.startDato shouldBe LocalDate.of(2025, 1, 1)
                    it.sluttDato shouldBe LocalDate.of(2025, 2, 1)
                    it.status.type shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.deltidsprosent shouldBe 100.0
                    it.antallPlasser shouldBe 10
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.pameldingType shouldBe GjennomforingPameldingType.DIREKTE_VEDTAK
                }

                queries.gjennomforing.delete(arenaEnkelAmo1.id)

                queries.gjennomforing.getGjennomforing(arenaEnkelAmo1.id) shouldBe null
            }
        }
    }

    context("enkeltplass") {
        val enkelAmo1 = EnkelAmo.copy(
            navn = "Arena-navn",
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
        )

        test("lagre enkeltplass") {
            database.runAndRollback {
                queries.gjennomforing.upsert(enkelAmo1)

                queries.gjennomforing.getGjennomforingEnkeltplassOrError(enkelAmo1.id).should {
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
                    )
                    it.arena?.tiltaksnummer.shouldBeNull()
                    it.arena?.ansvarligNavEnhet.shouldBeNull()
                    it.navn shouldBe "Arena-navn"
                    it.startDato shouldBe LocalDate.of(2025, 1, 1)
                    it.sluttDato.shouldBeNull()
                    it.status.type shouldBe GjennomforingStatusType.GJENNOMFORES
                }

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = enkelAmo1.id,
                        tiltaksnummer = Tiltaksnummer("2025#1"),
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                queries.gjennomforing.getGjennomforingEnkeltplassOrError(enkelAmo1.id).should {
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2025#1")
                    it.arena?.ansvarligNavEnhet shouldBe "0400"
                }

                queries.gjennomforing.delete(enkelAmo1.id)

                queries.gjennomforing.getGjennomforing(enkelAmo1.id) shouldBe null
            }
        }
    }

    context("free text search") {
        test("løpenummer og tiltaksnummer blir automatisk med i fritekstsøket") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                    ),
                ).setup(session)

                val lopenummer = queries.gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).lopenummer

                queries.gjennomforing.getAll(search = lopenummer.value).items.shouldHaveSize(0)

                queries.gjennomforing.setFreeTextSearch(Oppfolging1.id, listOf("foo"))

                queries.gjennomforing.getAll(search = lopenummer.value)
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAll(search = lopenummer.aar.toString())
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAll(search = lopenummer.lopenummer.toString())
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAll(search = "foo")
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                queries.gjennomforing.getAll(search = "bar").items.shouldHaveSize(0)
            }
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        test("filtrering på arrangør") {
            database.runAndRollback { session ->
                val domain = MulighetsrommetTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                        Oppfolging1.copy(id = UUID.randomUUID(), arrangorId = ArrangorFixtures.underenhet2.id),
                    ),
                ).setup(session)

                queries.gjennomforing.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe domain.gjennomforinger[0].id
                }

                queries.gjennomforing.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe domain.gjennomforinger[1].id
                }
            }
        }

        test("filtrering på avtale") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    gjennomforinger = listOf(Oppfolging1, AFT1),
                ).setup(session)

                queries.gjennomforing.getAll(avtaleId = AvtaleFixtures.oppfolging.id).items
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                queries.gjennomforing.getByAvtale(AvtaleFixtures.oppfolging.id)
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)

                queries.gjennomforing.getAll(avtaleId = AvtaleFixtures.AFT.id).items
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                queries.gjennomforing.getByAvtale(AvtaleFixtures.AFT.id)
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }
        }

        test("filtrer vekk gjennomføringer basert på sluttdato") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(sluttDato = LocalDate.of(2023, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 6, 29)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2022, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = null),
                    ),
                ).setup(session)

                queries.gjennomforing.getAll(sluttDatoGreaterThanOrEqualTo = LocalDate.of(2023, 1, 1))
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

        test("kan filtrere på enten administrator eller koordinator") {
            database.runAndRollback { session ->
                val id0 = UUID.randomUUID()
                val id1 = UUID.randomUUID()
                val domain = MulighetsrommetTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(id = id0),
                        Oppfolging1.copy(id = id1),
                    ),
                ) {
                    queries.gjennomforing.setAdministratorer(id0, setOf(NavAnsattFixture.DonaldDuck.navIdent))
                    queries.gjennomforing.setAdministratorer(
                        id1,
                        setOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
                    )
                }.setup(session)

                queries.gjennomforing.getAll(
                    administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).totalCount shouldBe 2

                queries.gjennomforing.getAll(
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).totalCount shouldBe 0

                queries.gjennomforing.insertKoordinatorForGjennomforing(
                    UUID.randomUUID(),
                    NavAnsattFixture.DonaldDuck.navIdent,
                    domain.gjennomforinger[0].id,
                )

                queries.gjennomforing.getAll(
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).items shouldContainExactlyIds listOf(domain.gjennomforinger[0].id)

                queries.gjennomforing.getAll(
                    administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                    koordinatorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                ).items shouldContainExactlyIds listOf(domain.gjennomforinger[1].id)
            }
        }

        test("filtrering på tiltakstype") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
                ).setup(session)

                queries.gjennomforing.getAll(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 1
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id)
                    }

                queries.gjennomforing.getAll(
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
                    gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
                ) {
                    queries.gjennomforing.setNavEnheter(
                        Oppfolging1.id,
                        setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                    )
                    queries.gjennomforing.setNavEnheter(
                        VTA1.id,
                        setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                    )
                    queries.gjennomforing.setNavEnheter(
                        AFT1.id,
                        setOf(Innlandet.enhetsnummer, Sel.enhetsnummer, Gjovik.enhetsnummer),
                    )
                }.setup(session)

                queries.gjennomforing.getAll(navEnheter = listOf(Gjovik.enhetsnummer))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                    }

                queries.gjennomforing.getAll(
                    navEnheter = listOf(
                        Lillehammer.enhetsnummer,
                        Sel.enhetsnummer,
                    ),
                )
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                    }

                queries.gjennomforing.getAll(navEnheter = listOf(Innlandet.enhetsnummer))
                    .should { (totalCount) ->
                        totalCount shouldBe 3
                    }
            }
        }

        test("filtrering på type gjennomføring") {
            database.runAndRollback { session ->
                MulighetsrommetTestDomain(
                    gjennomforinger = listOf(Oppfolging1, AFT1, EnkelAmo, ArenaEnkelAmo),
                ).setup(session)

                queries.gjennomforing.getAll().should {
                    it.totalCount shouldBe 4
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id, EnkelAmo.id, ArenaEnkelAmo.id)
                }

                queries.gjennomforing.getAll(
                    typer = listOf(GjennomforingType.ENKELTPLASS, GjennomforingType.AVTALE, GjennomforingType.ARENA),
                ).should {
                    it.totalCount shouldBe 4
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id, EnkelAmo.id, ArenaEnkelAmo.id)
                }

                queries.gjennomforing.getAll(typer = listOf(GjennomforingType.AVTALE)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                }

                queries.gjennomforing.getAll(typer = listOf(GjennomforingType.ENKELTPLASS)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(EnkelAmo.id)
                }

                queries.gjennomforing.getAll(typer = listOf(GjennomforingType.ARENA)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(ArenaEnkelAmo.id)
                }
            }
        }
    }

    test("pagination") {
        database.runAndRollback { _ ->
            (1..10).forEach {
                queries.gjennomforing.upsert(
                    Oppfolging1.copy(id = UUID.randomUUID(), navn = "$it".padStart(2, '0')),
                )
            }

            queries.gjennomforing.getAll(Pagination.all()).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            queries.gjennomforing.getAll(Pagination.of(page = 1, size = 20)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            queries.gjennomforing.getAll(Pagination.of(page = 1, size = 2)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "02"
            }

            queries.gjennomforing.getAll(Pagination.of(page = 3, size = 2)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "05"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "06"
            }

            queries.gjennomforing.getAll(Pagination.of(page = 3, size = 4)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "09"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            queries.gjennomforing.getAll(Pagination.of(page = 2, size = 20)).should {
                it.totalCount shouldBe 0
                it.items.shouldBeEmpty()
            }
        }
    }
})

private fun toGjennomforingArrangorKontaktperson(kontaktperson: ArrangorKontaktperson) = GjennomforingAvtaleDetaljer.ArrangorKontaktperson(
    id = kontaktperson.id,
    navn = kontaktperson.navn,
    beskrivelse = kontaktperson.beskrivelse,
    telefon = kontaktperson.telefon,
    epost = kontaktperson.epost,
)

private infix fun Collection<GjennomforingKompakt>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
