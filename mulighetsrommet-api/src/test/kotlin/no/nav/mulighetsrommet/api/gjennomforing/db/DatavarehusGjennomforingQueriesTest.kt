package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.toTable
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.GruppeAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.GruppeFagYrke1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.DatavarehusGjennomforingDto
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

class DatavarehusGjennomforingQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    test("henter relevante data om tiltakstype, avtale, og gjennomføring") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )
        domain.initialize(database.db)

        val gjennomforing = database.db.useSession {
            DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, AFT1.id)
        }

        gjennomforing.id shouldBe AFT1.id
        gjennomforing.navn shouldBe AFT1.navn
        gjennomforing.startDato shouldBe AFT1.startDato
        gjennomforing.sluttDato shouldBe AFT1.sluttDato
        gjennomforing.opprettetTidspunkt.shouldNotBeNull()
        gjennomforing.oppdatertTidspunkt.shouldNotBeNull()
        gjennomforing.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES
        gjennomforing.tiltakstype shouldBe DatavarehusGjennomforingDto.Tiltakstype(
            id = TiltakstypeFixtures.AFT.id,
            navn = TiltakstypeFixtures.AFT.navn,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        )
        gjennomforing.avtale.id shouldBe AvtaleFixtures.AFT.id
        gjennomforing.avtale.navn shouldBe AvtaleFixtures.AFT.navn
        gjennomforing.arrangor shouldBe DatavarehusGjennomforingDto.Arrangor(
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
        )
        gjennomforing.amoKategorisering.shouldBeNull()
        gjennomforing.utdanningslop.shouldBeNull()
        gjennomforing.arena.shouldBeNull()
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

        val gjennomforing = database.db.useSession {
            DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, AFT1.id)
        }

        gjennomforing.arena shouldBe DatavarehusGjennomforingDto.ArenaData(aar = 2020, lopenummer = 1234)
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
            val gjennomforing = database.db.useSession {
                DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, id)
            }

            gjennomforing.utdanningslop.shouldBeNull()
            gjennomforing.amoKategorisering.shouldNotBeNull().shouldBe(expectedAmoKategorisering)
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
            DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, GruppeFagYrke1.id)
        }

        gjennomforing.id shouldBe GruppeFagYrke1.id
        gjennomforing.amoKategorisering.shouldBeNull()
        gjennomforing.utdanningslop.shouldNotBeNull().shouldBe(
            DatavarehusGjennomforingDto.Utdanningslop(
                utdanningsprogram = DatavarehusGjennomforingDto.Utdanningslop.Utdanningsprogram(
                    navn = "Sveiseprogram",
                    nusKoder = listOf("1234", "2345"),
                ),
                utdanninger = setOf(
                    DatavarehusGjennomforingDto.Utdanningslop.Utdanning(
                        navn = "Sveisefag",
                        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
                        nusKoder = listOf("12345"),
                    ),
                    DatavarehusGjennomforingDto.Utdanningslop.Utdanning(
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
            DatavarehusGjennomforingQueries.getDatavarehusGjennomforing(it, GruppeFagYrke1.id)
        }

        gjennomforing.utdanningslop.shouldBeNull()
    }
})
