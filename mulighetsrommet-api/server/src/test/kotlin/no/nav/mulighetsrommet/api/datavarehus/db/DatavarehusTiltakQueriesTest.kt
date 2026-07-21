package no.nav.mulighetsrommet.api.datavarehus.db

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1AmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1Dto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1YrkesfagDto
import no.nav.mulighetsrommet.api.datavarehus.model.DvhAmoKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType
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
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.util.UUID

class DatavarehusTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    context("gruppetiltak") {
        test("henter relevante data om tiltakstype, avtale, og gjennomføring") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
            )

            val tiltak = database.runAndRollback {
                domain.initialize()

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

            val tiltak = database.runAndRollback {
                domain.initialize()

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(AFT1.id, tiltaksnummer = Tiltaksnummer("2020#1234")),
                )

                queries.dvh.getDatavarehusTiltak(AFT1.id)
            }

            tiltak.gjennomforing.arena shouldBe DatavarehusTiltakV1.ArenaData(aar = 2020, lopenummer = 1234)
        }

        context("henter Gruppe AMO med amo-kategorisering") {
            val studiespesialisering = OpplaringKategorisering(kurstype = KurstypeFixtures.studiespesialisering.id)
            val fov = OpplaringKategorisering(
                kurstype = KurstypeFixtures.fov.id,
                innholdElementer = setOf(
                    InnholdElementFixtures.bransjerettetOpplaring.id,
                ),
            )
            val grunnleggende = OpplaringKategorisering(
                kurstype = KurstypeFixtures.grunnleggendeFerdigheter.id,
                innholdElementer = setOf(
                    InnholdElementFixtures.grunnleggendeFerdigheter.id,
                ),
            )
            val norskopplaering = OpplaringKategorisering(
                kurstype = KurstypeFixtures.norskopplaering.id,
                innholdElementer = setOf(
                    InnholdElementFixtures.norskopplaring.id,
                ),
            )
            val bransje = OpplaringKategorisering(
                kurstype = KurstypeFixtures.bransjeOgYrkesrettet.id,
                bransje = BransjeFixtures.kontorarbeid.id,
                forerkort = setOf(ForerkortFixtures.A.id),
                innholdElementer = setOf(InnholdElementFixtures.praksis.id),
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

            test("studiepserialisering") {
                database.runAndRollback {
                    domain.initialize()

                    queries.opplaering.upsert(amoGjennomforing.id, studiespesialisering)

                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull()
                        .shouldBe(DvhAmoKategorisering(kurstype = Kurstype.Kode.STUDIESPESIALISERING))
                }
            }
            test("fov") {
                database.runAndRollback {
                    domain.initialize()

                    queries.opplaering.upsert(amoGjennomforing.id, fov)

                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull().shouldBe(
                            DvhAmoKategorisering(
                                kurstype = Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                                innholdElementer = listOf(InnholdElement.Kode.BRANSJERETTET_OPPLARING),
                            ),
                        )
                }
            }
            test("grunnleggende ferdigheter") {
                database.runAndRollback {
                    domain.initialize()

                    queries.opplaering.upsert(amoGjennomforing.id, grunnleggende)

                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull()
                        .shouldBe(
                            DvhAmoKategorisering(
                                kurstype = Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER,
                                innholdElementer = listOf(InnholdElement.Kode.GRUNNLEGGENDE_FERDIGHETER),
                            ),
                        )
                }
            }
            test("norskopplaering") {
                database.runAndRollback {
                    domain.initialize()

                    queries.opplaering.upsert(amoGjennomforing.id, norskopplaering)

                    queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull().shouldBe(
                            DvhAmoKategorisering(
                                kurstype = Kurstype.Kode.NORSKOPPLAERING,
                                norskprove = false,
                                innholdElementer = listOf(InnholdElement.Kode.NORSKOPPLAERING),
                            ),
                        )
                }
            }
            test("Bransje og yrke") {
                database.runAndRollback {
                    domain.initialize()

                    queries.opplaering.upsert(amoGjennomforing.id, bransje)

                    val bransjeOgYrkesrettet = queries.dvh.getDatavarehusTiltak(amoGjennomforing.id)
                        .shouldBeTypeOf<DatavarehusTiltakV1AmoDto>()
                        .amoKategorisering.shouldNotBeNull()

                    bransjeOgYrkesrettet.shouldBe(
                        DvhAmoKategorisering(
                            kurstype = Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
                            bransje = Bransje.Kode.KONTORARBEID,
                            innholdElementer = listOf(InnholdElement.Kode.PRAKSIS),
                            forerkort = listOf(ForerkortKlasse.Kode.A),
                            sertifiseringer = listOf(
                                Sertifisering(konseptId = 1, label = "Jobb"),
                            ),
                        ),
                    )
                }
            }
        }

        test("henter Gruppe Fag/Yrke med informasjon om utdanningsprogram") {
            val utdanninger = listOf(
                Utdanning(
                    programomradekode = "BABAT1----",
                    utdanningId = "u_sveisefag",
                    navn = "Sveisefag",
                    sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                    aktiv = true,
                    utdanningstatus = Utdanning.Status.GYLDIG,
                    utdanningslop = listOf("BABAT1----"),
                    nusKoder = listOf("12345"),
                ),
                Utdanning(
                    programomradekode = "BABAT1----",
                    utdanningId = "u_sveisefag_under_vann",
                    navn = "Sveisefag under vann",
                    sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
                    aktiv = true,
                    utdanningstatus = Utdanning.Status.GYLDIG,
                    utdanningslop = listOf("BABAT1----"),
                    nusKoder = listOf("23456"),
                ),
            )
            val utdanningsprogram = Utdanningsprogram.opprett(
                programomradekode = "BABAT1----",
                navn = "Sveiseprogram",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = utdanninger,
            ).shouldBeRight()

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
                avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
                gjennomforinger = listOf(GruppeFagYrke1),
                utdanningsprogram = listOf(utdanningsprogram),
            ) {
                val utdanningslop = Utdanningslop(
                    queries.utdanning.getIdForUtdanningsprogram("BABAT1----"),
                    setOf(
                        queries.utdanning.getIdForUtdanning("u_sveisefag"),
                        queries.utdanning.getIdForUtdanning("u_sveisefag_under_vann"),
                    ),
                )
                queries.opplaering.upsert(
                    GruppeFagYrke1.id,
                    OpplaringKategorisering(utdanningslop = utdanningslop),
                )
            }

            database.runAndRollback {
                domain.initialize()

                val idForUtdanningsprogram = queries.utdanning.getIdForUtdanningsprogram("BABAT1----")
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

            database.runAndRollback {
                domain.initialize()

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

            database.runAndRollback {
                domain.initialize()

                val tiltak = queries.dvh.getDatavarehusTiltak(GjennomforingFixtures.EnkelAmo.id)

                tiltak.shouldBeTypeOf<DatavarehusTiltakV1AmoDto>().should {
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
        }

        test("henter bare tiltaksnummer fra Arena-data") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.EnkelFagOgYrke),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelFagOgYrke),
            )

            database.runAndRollback {
                domain.initialize()

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = GjennomforingFixtures.EnkelFagOgYrke.id,
                        tiltaksnummer = Tiltaksnummer("2024#456"),
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                val tiltak = queries.dvh.getDatavarehusTiltak(GjennomforingFixtures.EnkelFagOgYrke.id)

                tiltak.shouldBeTypeOf<DatavarehusTiltakV1YrkesfagDto>().should {
                    it.gjennomforing.id shouldBe GjennomforingFixtures.EnkelFagOgYrke.id
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
    }
})
