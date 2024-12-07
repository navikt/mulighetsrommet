package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.toTable
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.GruppeAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.GruppeFagYrke1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.DatavarehusTiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.DatavarehusTiltakAmoDto
import no.nav.mulighetsrommet.api.gjennomforing.model.DatavarehusTiltakDto
import no.nav.mulighetsrommet.api.gjennomforing.model.DatavarehusTiltakYrkesfagDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.utdanning.db.UtdanningRepository
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utdanning.model.NusKodeverk
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramType
import java.util.*

class DatavarehusTiltakQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    test("henter relevante data om tiltakstype, avtale, og gjennomføring") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )
        domain.initialize(database.db)

        val tiltak = database.db.useSession {
            DatavarehusTiltakQueries.getDatavarehusTiltak(it, AFT1.id)
        }

        tiltak.shouldBeTypeOf<DatavarehusTiltakDto>().should {
            it.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
            it.gjennomforing.id shouldBe AFT1.id
            it.gjennomforing.navn shouldBe AFT1.navn
            it.gjennomforing.startDato shouldBe AFT1.startDato
            it.gjennomforing.sluttDato shouldBe AFT1.sluttDato
            it.gjennomforing.opprettetTidspunkt.shouldNotBeNull()
            it.gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
            it.gjennomforing.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES
            it.avtale shouldBe DatavarehusTiltak.Avtale(AvtaleFixtures.AFT.id, AvtaleFixtures.AFT.navn)
            it.arrangor shouldBe DatavarehusTiltak.Arrangor(
                organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            )
            it.arena.shouldBeNull()
        }
    }

    test("henter tiltaksnummer når det finnes i Arena") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )
        domain.initialize(database.db)

        TiltaksgjennomforingRepository(database.db).also { repository ->
            database.db.useSession {
                repository.updateArenaData(AFT1.id, tiltaksnummer = "2020#1234", arenaAnsvarligEnhet = null, it)
            }
        }

        val tiltak = database.db.useSession {
            DatavarehusTiltakQueries.getDatavarehusTiltak(it, AFT1.id)
        }

        tiltak.arena shouldBe DatavarehusTiltak.ArenaData(aar = 2020, lopenummer = 1234)
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
        domain.initialize(database.db)

        val table = domain.gjennomforinger.associate { it.id to it.amoKategorisering }.toTable()

        table.forAll { id, expectedAmoKategorisering ->
            val tiltak = database.db.useSession {
                DatavarehusTiltakQueries.getDatavarehusTiltak(it, id)
            }

            tiltak.shouldBeTypeOf<DatavarehusTiltakAmoDto>().amoKategorisering.shouldNotBeNull().shouldBe(
                expectedAmoKategorisering,
            )
        }
    }

    test("henter Gruppe Fag/Yrke med informasjon om utdanningsprogram") {
        val utdanningslop = database.db.transaction { tx ->
            val repository = UtdanningRepository(database.db)
            repository.upsertUtdanningsprogram(
                tx,
                Utdanningsprogram(
                    navn = "Sveiseprogram",
                    nusKoder = listOf("1234", "2345"),
                    programomradekode = "BABAN3----",
                    UtdanningsprogramType.YRKESFAGLIG,
                ),
            )

            repository.upsertUtdanning(
                tx,
                Utdanning(
                    programomradekode = "BABAN3----",
                    utdanningId = "u_sveisefag",
                    navn = "Sveisefag",
                    sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                    aktiv = true,
                    utdanningstatus = Utdanning.Status.GYLDIG,
                    utdanningslop = listOf("BABAN3----"),
                    nusKodeverk = listOf(NusKodeverk("Sveisefag", "12345")),
                ),
            )

            repository.upsertUtdanning(
                tx,
                Utdanning(
                    programomradekode = "BABAN3----",
                    utdanningId = "u_sveisefag_under_vann",
                    navn = "Sveisefag under vann",
                    sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
                    aktiv = true,
                    utdanningstatus = Utdanning.Status.GYLDIG,
                    utdanningslop = listOf("BABAN3----"),
                    nusKodeverk = listOf(NusKodeverk("Sveisefag under vann", "23456")),
                ),
            )

            UtdanningslopDbo(
                repository.getIdForUtdanningsprogram(tx, "BABAN3----"),
                listOf(
                    repository.getIdForUtdanning(tx, "u_sveisefag"),
                    repository.getIdForUtdanning(tx, "u_sveisefag_under_vann"),
                ),
            )
        }

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
            avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
            gjennomforinger = listOf(
                GruppeFagYrke1.copy(utdanningslop = utdanningslop),
            ),
        )
        domain.initialize(database.db)

        val gjennomforing = database.db.useSession {
            DatavarehusTiltakQueries.getDatavarehusTiltak(it, GruppeFagYrke1.id)
        }

        gjennomforing.shouldBeTypeOf<DatavarehusTiltakYrkesfagDto>().utdanningslop.shouldNotBeNull().shouldBe(
            DatavarehusTiltakYrkesfagDto.Utdanningslop(
                utdanningsprogram = DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanningsprogram(
                    navn = "Sveiseprogram",
                    nusKoder = listOf("1234", "2345"),
                ),
                utdanninger = setOf(
                    DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                        navn = "Sveisefag",
                        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                        nusKoder = listOf("12345"),
                    ),
                    DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                        navn = "Sveisefag under vann",
                        sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
                        nusKoder = listOf("23456"),
                    ),
                ),
            ),
        )
    }

    test("henter Gruppe Fag/Yrke uten informasjon om utdanningsprogram") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.GruppeFagOgYrkesopplaering),
            avtaler = listOf(AvtaleFixtures.gruppeFagYrke),
            gjennomforinger = listOf(GruppeFagYrke1),
        )
        domain.initialize(database.db)

        val gjennomforing = database.db.useSession {
            DatavarehusTiltakQueries.getDatavarehusTiltak(it, GruppeFagYrke1.id)
        }

        gjennomforing.shouldBeTypeOf<DatavarehusTiltakYrkesfagDto>().utdanningslop.shouldBeNull()
    }
})
