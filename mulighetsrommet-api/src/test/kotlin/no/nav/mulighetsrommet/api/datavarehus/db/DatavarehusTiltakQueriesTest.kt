package no.nav.mulighetsrommet.api.datavarehus.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.amo.toDbo
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1AmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1Dto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1YrkesfagDto
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.ForerkortFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.GruppeAmo1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.GruppeFagYrke1
import no.nav.mulighetsrommet.api.fixtures.InnholdElementFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramType
import java.util.UUID

class DatavarehusTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("gruppetiltak") {
        test("henter relevante data om tiltakstype, avtale, og gjennomføring") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            )

            val tiltak = database.runAndRollback { session ->
                domain.setup(session)

                queries.dvh.getDatavarehusTiltak(AFT1.id)
            }

            tiltak.shouldBeTypeOf<DatavarehusTiltakV1Dto>().should {
                it.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
                it.gjennomforing.id shouldBe AFT1.id
                it.gjennomforing.navn shouldBe AFT1.navn
                it.gjennomforing.startDato shouldBe AFT1.startDato
                it.gjennomforing.sluttDato shouldBe AFT1.sluttDato
                it.gjennomforing.opprettetTidspunkt.shouldNotBeNull()
                it.gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
                it.gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
                it.gjennomforing.arena.shouldBeNull()
                it.gjennomforing.oppstartstype shouldBe GjennomforingOppstartstype.LOPENDE
                it.gjennomforing.pameldingstype shouldBe GjennomforingPameldingType.DIREKTE_VEDTAK
                it.gjennomforing.arrangor shouldBe DatavarehusTiltakV1.Arrangor(
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                )
                it.avtale.shouldNotBeNull().should { avtale ->
                    avtale.id shouldBe AvtaleFixtures.AFT.id
                    avtale.navn shouldBe AvtaleFixtures.AFT.detaljerDbo.navn
                    avtale.opprettetTidspunkt.shouldNotBeNull()
                    avtale.oppdatertTidspunkt.shouldNotBeNull()
                }
            }
        }

        test("henter tiltaksnummer når det finnes i Arena") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            )

            val tiltak = database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(AFT1.id, tiltaksnummer = Tiltaksnummer("2020#1234")),
                )

                queries.dvh.getDatavarehusTiltak(AFT1.id)
            }

            tiltak.gjennomforing.arena shouldBe DatavarehusTiltakV1.ArenaData(aar = 2020, lopenummer = 1234)
        }

        context("henter Gruppe AMO med amo-kategorisering") {
            val studiespesialisering = OpplaringKategorisering(kurstype = KurstypeFixtures.studiespesialisering)
            val fov = OpplaringKategorisering(
                kurstype = KurstypeFixtures.fov,
                innholdElementer = setOf(
                    InnholdElementFixtures.bransjerettetOpplaring,
                ),
            )
            val grunnleggende =
                OpplaringKategorisering(
                    kurstype = KurstypeFixtures.grunnleggendeFerdigheter,
                    innholdElementer = setOf(
                        InnholdElementFixtures.grunnleggendeFerdigheter,
                    ),
                )
            val norskopplaering =
                OpplaringKategorisering(
                    kurstype = KurstypeFixtures.norskopplaering,
                    innholdElementer = setOf(
                        InnholdElementFixtures.norskopplaring,
                    ),
                )
            val bransje =
                OpplaringKategorisering(
                    kurstype = KurstypeFixtures.bransjeOgYrkesrettet,
                    bransje = BransjeFixtures.kontorarbeid,
                    forerkort = setOf(ForerkortFixtures.A),
                    innholdElementer = setOf(InnholdElementFixtures.praksis),
                    sertifiseringer = setOf(
                        Sertifisering(konseptId = 1, label = "Jobb"),
                    ),
                )
            val amoGjennomforing = GruppeAmo1.copy(id = UUID.randomUUID())
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeAmo),
                avtaler = listOf(AvtaleFixtures.gruppeAmo),
                gjennomforinger = listOf(
                    amoGjennomforing,
                ),
            )

            beforeSpec { database.truncateAll() }

            test("studiepserialisering") {
                database.runAndRollback {
                    domain.setup(it)
                    context(this.session) { OpplaringKategoriseringQueries.upsert(amoGjennomforing.id, studiespesialisering.toDbo()) }
                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull().shouldBe(AmoKategorisering.Studiespesialisering)
                }
            }
            test("fov") {
                database.runAndRollback { session ->
                    domain.setup(session)
                    context(this.session) { OpplaringKategoriseringQueries.upsert(amoGjennomforing.id, fov.toDbo()) }
                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull().shouldBe(
                            AmoKategorisering.ForberedendeOpplaeringForVoksne(
                                innholdElementer = listOf(AmoKategorisering.InnholdElement.BRANSJERETTET_OPPLARING),
                            ),
                        )
                }
            }
            test("grunnleggende ferdigheter") {
                database.runAndRollback { session ->
                    domain.setup(session)
                    context(this.session) { OpplaringKategoriseringQueries.upsert(amoGjennomforing.id, grunnleggende.toDbo()) }
                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull()
                        .shouldBe(AmoKategorisering.GrunnleggendeFerdigheter(innholdElementer = listOf(AmoKategorisering.InnholdElement.GRUNNLEGGENDE_FERDIGHETER)))
                }
            }
            test("norskopplaering") {
                database.runAndRollback { session ->
                    domain.setup(session)
                    context(this.session) { OpplaringKategoriseringQueries.upsert(amoGjennomforing.id, norskopplaering.toDbo()) }
                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull().shouldBe(
                            AmoKategorisering.Norskopplaering(
                                norskprove = false,
                                innholdElementer = listOf(AmoKategorisering.InnholdElement.NORSKOPPLAERING),
                            ),
                        )
                }
            }
            test("Bransje og yrke") {
                database.runAndRollback { session ->
                    domain.setup(session)
                    val dbo = bransje.toDbo()
                    context(this.session) { OpplaringKategoriseringQueries.upsert(amoGjennomforing.id, dbo) }
                    val bransjeOgYrkesrettet = queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull()

                    bransjeOgYrkesrettet.shouldBe(
                        AmoKategorisering.BransjeOgYrkesrettet(
                            bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.KONTORARBEID,
                            innholdElementer = listOf(AmoKategorisering.InnholdElement.PRAKSIS),
                            forerkort = listOf(AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.A),
                            sertifiseringer = listOf(
                                Sertifisering(konseptId = 1, label = "Jobb"),
                            ),
                        ),
                    )
                }
            }
        }

        test("henter Gruppe Fag/Yrke med informasjon om utdanningsprogram") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
                avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
                gjennomforinger = listOf(GruppeFagYrke1),
            ) {
                queries.utdanning.upsertUtdanningsprogram(
                    Utdanningsprogram(
                        navn = "Sveiseprogram",
                        nusKoder = listOf("1234", "2345"),
                        programomradekode = "BABAN3----",
                        UtdanningsprogramType.YRKESFAGLIG,
                    ),
                )

                queries.utdanning.upsertUtdanning(
                    Utdanning(
                        programomradekode = "BABAN3----",
                        utdanningId = "u_sveisefag",
                        navn = "Sveisefag",
                        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                        aktiv = true,
                        utdanningstatus = Utdanning.Status.GYLDIG,
                        utdanningslop = listOf("BABAN3----"),
                        nusKoder = listOf("12345"),
                    ),
                )

                queries.utdanning.upsertUtdanning(
                    Utdanning(
                        programomradekode = "BABAN3----",
                        utdanningId = "u_sveisefag_under_vann",
                        navn = "Sveisefag under vann",
                        sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
                        aktiv = true,
                        utdanningstatus = Utdanning.Status.GYLDIG,
                        utdanningslop = listOf("BABAN3----"),
                        nusKoder = listOf("23456"),
                    ),
                )

                val utdanningslop = UtdanningslopDbo(
                    queries.utdanning.getIdForUtdanningsprogram("BABAN3----"),
                    setOf(
                        queries.utdanning.getIdForUtdanning("u_sveisefag"),
                        queries.utdanning.getIdForUtdanning("u_sveisefag_under_vann"),
                    ),
                )

                queries.gjennomforing.setUtdanningslop(GruppeFagYrke1.id, utdanningslop)
            }

            database.runAndRollback { session ->
                domain.setup(session)
                val idForUtdanningsprogram = queries.utdanning.getIdForUtdanningsprogram("BABAN3----")
                val idForSveisefag = queries.utdanning.getIdForUtdanning("u_sveisefag")
                val idForSveisefagUnderVann = queries.utdanning.getIdForUtdanning("u_sveisefag_under_vann")

                val gjennomforing = queries.dvh.getDatavarehusTiltak(GruppeFagYrke1.id)

                gjennomforing.shouldBeTypeOf<DatavarehusTiltakV1YrkesfagDto>().utdanningslop.shouldNotBeNull().should {
                    it.utdanningsprogram shouldBe idForUtdanningsprogram
                    it.utdanninger shouldBe setOf(idForSveisefag, idForSveisefagUnderVann)
                }
            }
        }

        test("henter Gruppe Fag/Yrke uten informasjon om utdanningsprogram") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
                avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
                gjennomforinger = listOf(GruppeFagYrke1),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val gjennomforing = queries.dvh.getDatavarehusTiltak(GruppeFagYrke1.id)

                gjennomforing.shouldBeTypeOf<DatavarehusTiltakV1YrkesfagDto>().utdanningslop.shouldBeNull()
            }
        }
    }

    context("enkeltplasser") {
        test("henter relevant data om tiltakstype og gjennomføring") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            )

            val tiltak = database.runAndRollback { session ->
                domain.setup(session)

                DatavarehusTiltakQueries(session).getDatavarehusTiltak(GjennomforingFixtures.EnkelAmo.id)
            }

            tiltak.shouldBeTypeOf<DatavarehusTiltakV1Dto>().should {
                it.tiltakskode shouldBe Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
                it.gjennomforing.id shouldBe GjennomforingFixtures.EnkelAmo.id
                it.gjennomforing.opprettetTidspunkt.shouldNotBeNull()
                it.gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
                it.gjennomforing.arrangor shouldBe DatavarehusTiltakV1.Arrangor(
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                )
                it.gjennomforing.arena.shouldBeNull()
                it.gjennomforing.navn.shouldBeNull()
                it.gjennomforing.oppstartstype shouldBe GjennomforingOppstartstype.ENKELTPLASS
                it.gjennomforing.pameldingstype shouldBe GjennomforingPameldingType.TRENGER_GODKJENNING
                it.gjennomforing.startDato.shouldBeNull()
                it.gjennomforing.sluttDato.shouldBeNull()
                it.gjennomforing.status.shouldBeNull()
                it.avtale.shouldBeNull()
            }
        }

        test("henter bare tiltaksnummer fra Arena-data") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            )

            val tiltak = database.runAndRollback { session ->
                domain.setup(session)
                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = GjennomforingFixtures.EnkelAmo.id,
                        tiltaksnummer = Tiltaksnummer("2024#456"),
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                DatavarehusTiltakQueries(session).getDatavarehusTiltak(GjennomforingFixtures.EnkelAmo.id)
            }

            tiltak.shouldBeTypeOf<DatavarehusTiltakV1Dto>().should {
                it.gjennomforing.id shouldBe GjennomforingFixtures.EnkelAmo.id
                it.gjennomforing.opprettetTidspunkt.shouldNotBeNull()
                it.gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
                it.gjennomforing.arena shouldBe DatavarehusTiltakV1.ArenaData(
                    aar = 2024,
                    lopenummer = 456,
                )
                it.gjennomforing.navn.shouldBeNull()
                it.gjennomforing.startDato.shouldBeNull()
                it.gjennomforing.sluttDato.shouldBeNull()
                it.gjennomforing.status.shouldBeNull()
            }
        }
    }
})
