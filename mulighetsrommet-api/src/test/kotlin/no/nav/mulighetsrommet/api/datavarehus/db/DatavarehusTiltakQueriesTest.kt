package no.nav.mulighetsrommet.api.datavarehus.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.toTable
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltak
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakAmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakYrkesfagDto
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.GruppeAmo1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.GruppeFagYrke1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramType
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.memberProperties

class DatavarehusTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    test("henter relevante data om tiltakstype, avtale, og gjennomføring") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )

        val tiltak = database.runAndRollback { session ->
            domain.setup(session)

            val queries = DatavarehusTiltakQueries(session)

            queries.getTiltak(AFT1.id)
        }

        tiltak.shouldBeTypeOf<DatavarehusTiltakDto>().should {
            it.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            it.gjennomforing.id shouldBe AFT1.id
            it.gjennomforing.navn shouldBe AFT1.navn
            it.gjennomforing.startDato shouldBe AFT1.startDato
            it.gjennomforing.sluttDato shouldBe AFT1.sluttDato
            it.gjennomforing.opprettetTidspunkt.shouldNotBeNull()
            it.gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
            it.gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            it.gjennomforing.arena.shouldBeNull()
            it.gjennomforing.arrangor shouldBe DatavarehusTiltak.Arrangor(
                organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            )
            it.avtale.shouldNotBeNull().should { avtale ->
                avtale.id shouldBe AvtaleFixtures.AFT.id
                avtale.navn shouldBe AvtaleFixtures.AFT.navn
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
            queries.gjennomforing.updateArenaData(AFT1.id, tiltaksnummer = "2020#1234", arenaAnsvarligEnhet = null)

            val queries = DatavarehusTiltakQueries(session)

            queries.getTiltak(AFT1.id)
        }

        tiltak.gjennomforing.arena shouldBe DatavarehusTiltak.ArenaData(aar = 2020, lopenummer = 1234)
    }

    test("henter Gruppe AMO med amo-kategorisering") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeAmo),
            avtaler = listOf(AvtaleFixtures.gruppeAmo),
            gjennomforinger = listOf(
                GruppeAmo1.copy(
                    id = UUID.randomUUID(),
                    amoKategorisering = AmoKategorisering.Studiespesialisering,
                ),
                GruppeAmo1.copy(
                    id = UUID.randomUUID(),
                    amoKategorisering = AmoKategorisering.ForberedendeOpplaeringForVoksne,
                ),
                GruppeAmo1.copy(
                    id = UUID.randomUUID(),
                    amoKategorisering = AmoKategorisering.GrunnleggendeFerdigheter(
                        innholdElementer = listOf(
                            AmoKategorisering.InnholdElement.GRUNNLEGGENDE_FERDIGHETER,
                        ),
                    ),
                ),
                GruppeAmo1.copy(
                    id = UUID.randomUUID(),
                    amoKategorisering = AmoKategorisering.Norskopplaering(
                        norskprove = true,
                        innholdElementer = listOf(AmoKategorisering.InnholdElement.NORSKOPPLAERING),
                    ),
                ),
                GruppeAmo1.copy(
                    id = UUID.randomUUID(),
                    amoKategorisering = AmoKategorisering.BransjeOgYrkesrettet(
                        bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.KONTORARBEID,
                        innholdElementer = listOf(AmoKategorisering.InnholdElement.PRAKSIS),
                        forerkort = listOf(AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.A),
                        sertifiseringer = listOf(
                            AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(konseptId = 1, label = "Jobb"),
                        ),
                    ),
                ),
            ),
        )

        val table = domain.gjennomforinger.associate { it.id to it.amoKategorisering }.toTable()

        database.runAndRollback { session ->
            domain.setup(session)

            val queries = DatavarehusTiltakQueries(session)

            table.forAll { id, expectedAmoKategorisering ->
                val tiltak = queries.getTiltak(id)

                tiltak.shouldBeTypeOf<DatavarehusTiltakAmoDto>().amoKategorisering.shouldNotBeNull().shouldBe(
                    expectedAmoKategorisering,
                )
            }
        }
    }

    test("henter Gruppe Fag/Yrke med informasjon om utdanningsprogram") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
            avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
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
                listOf(
                    queries.utdanning.getIdForUtdanning("u_sveisefag"),
                    queries.utdanning.getIdForUtdanning("u_sveisefag_under_vann"),
                ),
            )

            queries.gjennomforing.upsert(GruppeFagYrke1.copy(utdanningslop = utdanningslop))
        }

        database.runAndRollback { session ->
            domain.setup(session)
            val idForUtdanningsprogram = queries.utdanning.getIdForUtdanningsprogram("BABAN3----")
            val idForSveisefag = queries.utdanning.getIdForUtdanning("u_sveisefag")
            val idForSveisefagUnderVann = queries.utdanning.getIdForUtdanning("u_sveisefag_under_vann")

            val queries = DatavarehusTiltakQueries(session)

            val gjennomforing = queries.getTiltak(GruppeFagYrke1.id)

            gjennomforing.shouldBeTypeOf<DatavarehusTiltakYrkesfagDto>().utdanningslop.shouldNotBeNull().should {
                it.utdanningsprogram.id shouldBe idForUtdanningsprogram
                it.utdanningsprogram.navn shouldBe "Sveiseprogram"
                it.utdanningsprogram.nusKoder shouldBe listOf("1234", "2345")
                it.utdanningsprogram.opprettetTidspunkt.shouldNotBeNull()
                it.utdanningsprogram.oppdatertTidspunkt.shouldNotBeNull()

                it.utdanninger.shouldHaveSingleElement { utdanning ->
                    utdanning.equalsIgnoring(
                        DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                            id = idForSveisefag,
                            navn = "Sveisefag",
                            sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                            nusKoder = listOf("12345"),
                            opprettetTidspunkt = LocalDateTime.now(),
                            oppdatertTidspunkt = LocalDateTime.now(),
                        ),
                        "opprettetTidspunkt",
                        "oppdatertTidspunkt",
                    )
                }
                it.utdanninger.shouldHaveSingleElement { utdanning ->
                    utdanning.equalsIgnoring(
                        DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                            id = idForSveisefagUnderVann,
                            navn = "Sveisefag under vann",
                            sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
                            nusKoder = listOf("23456"),
                            opprettetTidspunkt = LocalDateTime.now(),
                            oppdatertTidspunkt = LocalDateTime.now(),
                        ),
                        "opprettetTidspunkt",
                        "oppdatertTidspunkt",
                    )
                }
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

            val queries = DatavarehusTiltakQueries(session)

            val gjennomforing = queries.getTiltak(GruppeFagYrke1.id)

            gjennomforing.shouldBeTypeOf<DatavarehusTiltakYrkesfagDto>().utdanningslop.shouldBeNull()
        }
    }
})

// Extension function to compare data classes excluding specific properties
inline fun <reified T : Any> T.equalsIgnoring(
    other: T?,
    vararg propertiesToExclude: String,
): Boolean {
    if (this === other) return true
    if (other == null) return false

    val properties = T::class.memberProperties
        .filter { it.name !in propertiesToExclude }

    return properties.all { prop ->
        val thisValue = prop.get(this)
        val otherValue = prop.get(other)
        thisValue == otherValue
    }
}
