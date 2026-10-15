package no.nav.mulighetsrommet.api.persistence.gjennomforing

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
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.gjennomforing.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.InnholdElementFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.KurstypeFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.persistence.AdminTestDomain
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.AFT1
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.ArenaEnkelAmo
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.GruppeAmo1
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.VTA1
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.DeltakerStatusType
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
    val database = extension(SqlAdminDatabaseTestListener())

    val domain = AdminTestDomain(
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT, AvtaleFixtures.gruppeAmo),
    )

    beforeSpec {
        database.run { domain.initialize() }
    }

    context("avtale") {
        test("lagre gjennomføring") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)

                gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id) should {
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
                    it.status shouldBe GjennomforingAvtaleStatus.Gjennomfores
                    it.antallPlasser shouldBe 12
                    it.avtaleId shouldBe Oppfolging1.avtaleId
                    it.oppstart shouldBe GjennomforingOppstartstype.LOPENDE
                    it.arena?.tiltaksnummer shouldBe null
                    it.arena?.ansvarligNavEnhet shouldBe null
                    it.apentForPamelding shouldBe true
                    it.kontorstruktur.shouldNotBeNull()
                }

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldBe emptyList()
                    it.administratorer shouldBe listOf()
                    it.kontorstruktur.shouldNotBeNull()
                    it.kontaktpersoner shouldBe listOf()
                    it.oppmoteSted.shouldBeNull()
                    it.faneinnhold shouldBe null
                    it.beskrivelse shouldBe null
                }

                gjennomforing.delete(Oppfolging1.id)

                gjennomforing.getGjennomforing(Oppfolging1.id) shouldBe null
            }
        }

        test("Administratorer crud") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setAdministratorer(Oppfolging1.id, setOf(NavAnsattFixture.DonaldDuck.navIdent))

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).administratorer.shouldContainExactlyInAnyOrder(
                    GjennomforingAvtaleDetaljer.Administrator(
                        navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                        navn = "Donald Duck",
                    ),
                )
            }
        }

        test("navEnheter crud") {
            database.runAndRollback {
                AdminTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Lillehammer, Sel, Oslo),
                ).initialize()

                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer, Sel.enhetsnummer),
                )
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
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

                gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                )
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
                    .kontorstruktur
                    .shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Innlandet.enhetsnummer
                        it.kontorer.shouldHaveSize(1).first().enhetsnummer shouldBe Lillehammer.enhetsnummer
                    }

                gjennomforing.setNavEnheter(
                    Oppfolging1.id,
                    setOf(Oslo.enhetsnummer),
                )
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id)
                    .kontorstruktur
                    .shouldHaveSize(1)
                    .first()
                    .should {
                        it.region.enhetsnummer shouldBe Oslo.enhetsnummer
                        it.kontorer.shouldBeEmpty()
                    }

                gjennomforing.setNavEnheter(Oppfolging1.id, setOf())
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontorstruktur.shouldBeEmpty()
            }
        }

        test("tilgjengelig for arrangør") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setTilgjengeligForArrangorDato(Oppfolging1.id, LocalDate.now())
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.tilgjengeligForArrangorDato shouldBe LocalDate.now()
                }
            }
        }

        test("Nav kontaktperson") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setKontaktpersoner(
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

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontaktpersoner shouldContainExactlyInAnyOrder listOf(
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

                gjennomforing.setKontaktpersoner(
                    Oppfolging1.id,
                    setOf(
                        GjennomforingKontaktpersonDbo(
                            navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                            beskrivelse = null,
                        ),
                    ),
                )

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).kontaktpersoner shouldBe listOf(
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

            database.runAndRollback {
                AdminTestDomain(
                    arrangorer = listOf(ArrangorFixtures.hovedenhet.registrerKontaktpersoner(listOf(thomas, jens))),
                ).initialize()

                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf(thomas.id))
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactly listOf(toGjennomforingArrangorKontaktperson(thomas))
                }

                gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf())
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldHaveSize 0
                }

                gjennomforing.setArrangorKontaktpersoner(Oppfolging1.id, setOf(thomas.id, jens.id))
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toGjennomforingArrangorKontaktperson(thomas),
                        toGjennomforingArrangorKontaktperson(jens),
                    )
                }

                gjennomforing.frikobleKontaktpersonFraGjennomforing(thomas.id, Oppfolging1.id)

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.arrangorKontaktpersoner shouldContainExactlyInAnyOrder listOf(
                        toGjennomforingArrangorKontaktperson(jens),
                    )
                }
            }
        }

        test("Publisert må settes eksplisitt") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).publisert shouldBe false

                gjennomforing.setPublisert(Oppfolging1.id, true)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).publisert shouldBe true
            }
        }

        test("skal sette åpent for påmelding") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).apentForPamelding shouldBe true

                gjennomforing.setApentForPamelding(Oppfolging1.id, false)
                gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).apentForPamelding shouldBe false
            }
        }

        test("skal sette estimert ventetid") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).estimertVentetid.shouldBeNull()

                gjennomforing.setEstimertVentetid(Oppfolging1.id, 3, "maned")
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).estimertVentetid should {
                    it.shouldNotBeNull()
                    it.verdi shouldBe 3
                    it.enhet shouldBe "maned"
                }

                gjennomforing.setEstimertVentetid(Oppfolging1.id, null, null)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).estimertVentetid.shouldBeNull()
            }
        }

        test("oppdater status") {
            database.runAndRollback {
                val id = Oppfolging1.id
                gjennomforing.upsert(Oppfolging1)

                val tidspunkt = LocalDate.now()
                gjennomforing.setStatus(
                    id,
                    GjennomforingStatusType.AVBRUTT,
                    tidspunkt,
                    listOf(AvbrytGjennomforingAarsak.ANNET),
                    ":)",
                )
                gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingAvtaleStatus.Avbrutt(
                    aarsaker = listOf(AvbrytGjennomforingAarsak.ANNET),
                    forklaring = ":)",
                )

                gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.AVLYST,
                    sluttDato = tidspunkt,
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )
                gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingAvtaleStatus.Avlyst(
                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    forklaring = null,
                )

                gjennomforing.setStatus(
                    id = id,
                    status = GjennomforingStatusType.GJENNOMFORES,
                    sluttDato = tidspunkt,
                    aarsaker = null,
                    forklaring = null,
                )
                gjennomforing.getGjennomforingAvtaleOrError(id).status shouldBe GjennomforingAvtaleStatus.Gjennomfores
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
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setRedaksjoneltInnhold(Oppfolging1.id, null, faneinnhold)

                gjennomforing.getGjennomforingAvtaleDetaljerOrError(Oppfolging1.id).should {
                    it.faneinnhold.shouldNotBeNull().forHvem shouldBe faneinnhold.forHvem
                }
            }
        }

        test("lagre amoKategoriserng") {
            val kategorisering = OpplaringKategorisering(
                kurstype = KurstypeFixtures.norskopplaering.id,
                norskprove = true,
                innholdElementer = setOf(
                    InnholdElementFixtures.arbeidsmarkedskunnskap.id,
                    InnholdElementFixtures.praksis.id,
                ),
            )

            database.runAndRollback {
                gjennomforing.upsert(GruppeAmo1)
                opplaering.upsert(GruppeAmo1.id, kategorisering)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(GruppeAmo1.id).should {
                    it.opplaringKategorisering shouldBe OpplaringKategoriseringDetaljer(
                        kurstype = KurstypeFixtures.norskopplaering,
                        norskprove = true,
                        innholdElementer = setOf(
                            InnholdElementFixtures.arbeidsmarkedskunnskap,
                            InnholdElementFixtures.praksis,
                        ),
                    )
                }

                opplaering.upsert(GruppeAmo1.id, null)
                gjennomforing.getGjennomforingAvtaleDetaljerOrError(GruppeAmo1.id).should {
                    it.opplaringKategorisering shouldBe null
                }
            }
        }

        test("stengt hos arrangør lagres og hentes i periodens rekkefølge") {
            database.runAndRollback {
                gjennomforing.upsert(Oppfolging1)
                gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )
                gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 7, 1)),
                    "Sommerferie",
                )
                gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2024, 12, 1)),
                    "Forrige juleferie",
                )

                gjennomforing.getGjennomforingAvtaleOrError(Oppfolging1.id).stengt shouldContainExactly listOf(
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
                gjennomforing.upsert(Oppfolging1)

                gjennomforing.setStengtHosArrangor(
                    Oppfolging1.id,
                    Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    "Januarferie",
                )

                query {
                    gjennomforing.setStengtHosArrangor(
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
                gjennomforing.upsert(arenaEnkelAmo1)

                gjennomforing.getGjennomforingArenaOrError(arenaEnkelAmo1.id).should {
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
                    it.status shouldBe GjennomforingAvtaleStatus.Gjennomfores
                    it.deltidsprosent shouldBe 100.0
                    it.antallPlasser shouldBe 10
                    it.oppstart shouldBe GjennomforingOppstartstype.ENKELTPLASS
                    it.pameldingType shouldBe GjennomforingPameldingType.TRENGER_GODKJENNING
                }

                gjennomforing.delete(arenaEnkelAmo1.id)

                gjennomforing.getGjennomforing(arenaEnkelAmo1.id) shouldBe null
            }
        }
    }

    context("enkeltplass") {
        val enkelAmo1 = EnkelAmo.copy(
            navn = "Arena-navn",
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = null,
            status = GjennomforingStatusType.ENKELTPLASS_DELTAR,
        )

        test("lagre enkeltplass") {
            database.runAndRollback {
                gjennomforing.upsert(enkelAmo1)

                gjennomforing.getGjennomforingEnkeltplassOrError(enkelAmo1.id).should {
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
                    it.status shouldBe GjennomforingEnkeltplassStatus.Deltar
                    it.ansvarligEnhet shouldBe GjennomforingEnkeltplass.AnsvarligEnhet(
                        enhetsnummer = NavEnhetNummer("0400"),
                        navn = "Nav Innlandet",
                    )
                }

                gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = enkelAmo1.id,
                        tiltaksnummer = Tiltaksnummer("2025#1"),
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                gjennomforing.getGjennomforingEnkeltplassOrError(enkelAmo1.id).should {
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2025#1")
                    it.arena?.ansvarligNavEnhet shouldBe "0400"
                }

                gjennomforing.delete(enkelAmo1.id)

                gjennomforing.getGjennomforing(enkelAmo1.id) shouldBe null
            }
        }
    }

    context("free text search") {
        test("kan søke med fritekst") {
            database.runAndRollback {
                AdminTestDomain(
                    gjennomforinger = listOf(Oppfolging1),
                ).initialize()

                gjennomforing.setFreeTextSearch(Oppfolging1.id, listOf("foo", "foobar"))

                gjennomforing.getAll(search = "foo")
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                gjennomforing.getAll(search = "foob")
                    .items.shouldHaveSize(1).first().id shouldBe Oppfolging1.id

                gjennomforing.getAll(search = "bar").items.shouldHaveSize(0)
            }
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        test("filtrering på arrangør") {
            database.runAndRollback {
                val domain = AdminTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
                        Oppfolging1.copy(id = UUID.randomUUID(), arrangorId = ArrangorFixtures.underenhet2.id),
                    ),
                ).initialize()

                gjennomforing.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe domain.gjennomforinger[0].id
                }

                gjennomforing.getAll(
                    arrangorOrgnr = listOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
                ).should {
                    it.items.size shouldBe 1
                    it.items[0].id shouldBe domain.gjennomforinger[1].id
                }
            }
        }

        test("filtrering på avtale") {
            database.runAndRollback {
                AdminTestDomain(
                    gjennomforinger = listOf(Oppfolging1, AFT1),
                ).initialize()

                gjennomforing.getAll(avtaleId = AvtaleFixtures.oppfolging.id).items
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                gjennomforing.getByAvtale(AvtaleFixtures.oppfolging.id)
                    .shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)

                gjennomforing.getAll(avtaleId = AvtaleFixtures.AFT.id).items
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                gjennomforing.getByAvtale(AvtaleFixtures.AFT.id)
                    .shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }
        }

        test("filtrer vekk gjennomføringer basert på sluttdato") {
            database.runAndRollback {
                AdminTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(sluttDato = LocalDate.of(2023, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 6, 29)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2022, 12, 31)),
                        Oppfolging1.copy(id = UUID.randomUUID(), sluttDato = null),
                    ),
                ).initialize()

                gjennomforing.getAll(sluttDatoGreaterThanOrEqualTo = LocalDate.of(2023, 1, 1))
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
            database.runAndRollback {
                val id0 = UUID.randomUUID()
                val id1 = UUID.randomUUID()
                val domain = AdminTestDomain(
                    gjennomforinger = listOf(
                        Oppfolging1.copy(id = id0),
                        Oppfolging1.copy(id = id1),
                    ),
                ) {
                    gjennomforing.setAdministratorer(id0, setOf(NavAnsattFixture.DonaldDuck.navIdent))
                    gjennomforing.setAdministratorer(
                        id1,
                        setOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
                    )
                }.initialize()

                gjennomforing.getAll(
                    administratorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).totalCount shouldBe 2

                gjennomforing.getAll(
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).totalCount shouldBe 0

                gjennomforing.insertKoordinatorForGjennomforing(
                    UUID.randomUUID(),
                    NavAnsattFixture.DonaldDuck.navIdent,
                    domain.gjennomforinger[0].id,
                )

                gjennomforing.getAll(
                    koordinatorNavIdent = NavAnsattFixture.DonaldDuck.navIdent,
                ).items shouldContainExactlyIds listOf(domain.gjennomforinger[0].id)

                gjennomforing.getAll(
                    administratorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                    koordinatorNavIdent = NavAnsattFixture.MikkeMus.navIdent,
                ).items shouldContainExactlyIds listOf(domain.gjennomforinger[1].id)
            }
        }

        test("filtrering på tiltakstype") {
            database.runAndRollback {
                AdminTestDomain(
                    gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
                ).initialize()

                gjennomforing.getAll(tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.id))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 1
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id)
                    }

                gjennomforing.getAll(
                    tiltakstyper = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.VTA.id),
                ).should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 2
                    gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                }
            }
        }

        test("filtrering på Nav-enhet") {
            database.runAndRollback {
                AdminTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Lillehammer, Sel),
                    gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
                ) {
                    gjennomforing.setNavEnheter(
                        Oppfolging1.id,
                        setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer),
                    )
                    gjennomforing.setNavEnheter(
                        VTA1.id,
                        setOf(Innlandet.enhetsnummer, Lillehammer.enhetsnummer),
                    )
                    gjennomforing.setNavEnheter(
                        AFT1.id,
                        setOf(Innlandet.enhetsnummer, Sel.enhetsnummer, Gjovik.enhetsnummer),
                    )
                }.initialize()

                gjennomforing.getAll(navEnheter = listOf(Gjovik.enhetsnummer))
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                    }

                gjennomforing.getAll(
                    navEnheter = listOf(
                        Lillehammer.enhetsnummer,
                        Sel.enhetsnummer,
                    ),
                )
                    .should { (totalCount, gjennomforinger) ->
                        totalCount shouldBe 2
                        gjennomforinger shouldContainExactlyIds listOf(VTA1.id, AFT1.id)
                    }

                gjennomforing.getAll(navEnheter = listOf(Innlandet.enhetsnummer))
                    .should { (totalCount) ->
                        totalCount shouldBe 3
                    }
            }
        }

        test("filtrering på Nav-enhet treffer enkeltplass på ansvarlig enhet og dens overordnede enhet") {
            database.runAndRollback {
                val enkeltplass = EnkelAmo.copy(ansvarligEnhet = Gjovik.enhetsnummer)
                AdminTestDomain(
                    navEnheter = listOf(Innlandet, Gjovik, Sel),
                    gjennomforinger = listOf(enkeltplass),
                ).initialize()

                queries.gjennomforing.getAll(navEnheter = listOf(Gjovik.enhetsnummer))
                    .items shouldContainExactlyIds listOf(enkeltplass.id)

                queries.gjennomforing.getAll(navEnheter = listOf(Innlandet.enhetsnummer))
                    .items shouldContainExactlyIds listOf(enkeltplass.id)

                queries.gjennomforing.getAll(navEnheter = listOf(Sel.enhetsnummer))
                    .totalCount shouldBe 0
            }
        }

        test("filtrering på type gjennomføring") {
            database.runAndRollback {
                AdminTestDomain(
                    gjennomforinger = listOf(Oppfolging1, AFT1, EnkelAmo, ArenaEnkelAmo),
                    deltakere = listOf(DeltakerFixtures.createDeltaker(gjennomforingId = EnkelAmo.id)),
                ).initialize()

                gjennomforing.getAll().should {
                    it.totalCount shouldBe 4
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id, EnkelAmo.id, ArenaEnkelAmo.id)
                }

                gjennomforing.getAll(
                    typer = listOf(GjennomforingType.ENKELTPLASS, GjennomforingType.AVTALE, GjennomforingType.ARENA),
                ).should {
                    it.totalCount shouldBe 4
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id, EnkelAmo.id, ArenaEnkelAmo.id)
                }

                gjennomforing.getAll(typer = listOf(GjennomforingType.AVTALE)).should {
                    it.totalCount shouldBe 2
                    it.items shouldContainExactlyIds listOf(Oppfolging1.id, AFT1.id)
                }

                gjennomforing.getAll(typer = listOf(GjennomforingType.ENKELTPLASS)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(EnkelAmo.id)
                }

                gjennomforing.getAll(typer = listOf(GjennomforingType.ARENA)).should {
                    it.totalCount shouldBe 1
                    it.items shouldContainExactlyIds listOf(ArenaEnkelAmo.id)
                }
            }
        }

        test("filtrering på status treffer både avtale, arena og enkeltplass i én samlet liste") {
            database.runAndRollback {
                val avsluttetAft = AFT1.copy(status = GjennomforingStatusType.AVSLUTTET)
                AdminTestDomain(
                    gjennomforinger = listOf(Oppfolging1, avsluttetAft, EnkelAmo),
                    deltakere = listOf(
                        DeltakerFixtures.createDeltaker(
                            gjennomforingId = EnkelAmo.id,
                            status = DeltakerStatusType.DELTAR,
                        ),
                    ),
                ).initialize()

                gjennomforing.getAll().should {
                    it.items.filterIsInstance<GjennomforingEnkeltplassKompakt>()
                        .shouldHaveSize(1).first().status shouldBe GjennomforingEnkeltplassStatus.Deltar
                }

                gjennomforing.getAll().items shouldContainExactlyIds
                    listOf(Oppfolging1.id, avsluttetAft.id, EnkelAmo.id)

                gjennomforing.getAll(statuser = listOf(GjennomforingStatusType.GJENNOMFORES)).items
                    .shouldContainExactlyIds(listOf(Oppfolging1.id))

                gjennomforing.getAll(statuser = listOf(GjennomforingStatusType.AVSLUTTET)).items
                    .shouldContainExactlyIds(listOf(avsluttetAft.id))

                gjennomforing.getAll(statuser = listOf(GjennomforingStatusType.ENKELTPLASS_DELTAR)).items
                    .shouldContainExactlyIds(listOf(EnkelAmo.id))

                gjennomforing.getAll(statuser = listOf(GjennomforingStatusType.ENKELTPLASS_FULLFORT)).items
                    .shouldHaveSize(0)

                gjennomforing.getAll(
                    statuser = listOf(
                        GjennomforingStatusType.GJENNOMFORES,
                        GjennomforingStatusType.ENKELTPLASS_DELTAR,
                    ),
                ).items.shouldContainExactlyIds(listOf(Oppfolging1.id, EnkelAmo.id))

                gjennomforing.getAll(
                    statuser = listOf(
                        GjennomforingStatusType.GJENNOMFORES,
                        GjennomforingStatusType.ENKELTPLASS_FULLFORT,
                    ),
                ).items.shouldContainExactlyIds(listOf(Oppfolging1.id))
            }
        }
    }

    test("pagination") {
        database.runAndRollback {
            (1..10).forEach {
                gjennomforing.upsert(
                    Oppfolging1.copy(id = UUID.randomUUID(), navn = "$it".padStart(2, '0')),
                )
            }

            gjennomforing.getAll(Pagination.all()).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            gjennomforing.getAll(Pagination.of(page = 1, size = 20)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            gjennomforing.getAll(Pagination.of(page = 1, size = 2)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "01"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "02"
            }

            gjennomforing.getAll(Pagination.of(page = 3, size = 2)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "05"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "06"
            }

            gjennomforing.getAll(Pagination.of(page = 3, size = 4)).should {
                it.totalCount shouldBe 10
                it.items.first().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "09"
                it.items.last().shouldBeTypeOf<GjennomforingAvtaleKompakt>().navn shouldBe "10"
            }

            gjennomforing.getAll(Pagination.of(page = 2, size = 20)).should {
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
