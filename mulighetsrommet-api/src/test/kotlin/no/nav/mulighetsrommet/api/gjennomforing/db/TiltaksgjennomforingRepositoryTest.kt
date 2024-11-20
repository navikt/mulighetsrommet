package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.*
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotliquery.Query
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingKontaktperson
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetFixtures.IT,
            Innlandet,
            Gjovik,
            Lillehammer,
            Sel,
            Oslo,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.VTA,
            TiltakstypeFixtures.ArbeidsrettetRehabilitering,
            TiltakstypeFixtures.GruppeAmo,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.Jobbklubb,
            TiltakstypeFixtures.DigitalOppfolging,
            TiltakstypeFixtures.Avklaring,
            TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
            TiltakstypeFixtures.EnkelAmo,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    context("CRUD") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("CRUD adminflate-tiltaksgjennomføringer") {
            tiltaksgjennomforinger.upsert(Oppfolging1)

            tiltaksgjennomforinger.get(Oppfolging1.id) should {
                it.shouldNotBeNull()
                it.id shouldBe Oppfolging1.id
                it.tiltakstype shouldBe TiltaksgjennomforingDto.Tiltakstype(
                    id = TiltakstypeFixtures.Oppfolging.id,
                    navn = TiltakstypeFixtures.Oppfolging.navn,
                    tiltakskode = Tiltakskode.OPPFOLGING,
                )
                it.navn shouldBe Oppfolging1.navn
                it.tiltaksnummer shouldBe null
                it.arrangor shouldBe TiltaksgjennomforingDto.ArrangorUnderenhet(
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    navn = ArrangorFixtures.underenhet1.navn,
                    slettet = false,
                    kontaktpersoner = emptyList(),
                )
                it.startDato shouldBe Oppfolging1.startDato
                it.sluttDato shouldBe Oppfolging1.sluttDato
                it.arenaAnsvarligEnhet shouldBe null
                it.status.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET
                it.apentForPamelding shouldBe true
                it.antallPlasser shouldBe 12
                it.avtaleId shouldBe Oppfolging1.avtaleId
                it.administratorer shouldBe listOf(
                    TiltaksgjennomforingDto.Administrator(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                    ),
                )
                it.navEnheter shouldBe listOf(Gjovik)
                it.oppstart shouldBe TiltaksgjennomforingOppstartstype.LOPENDE
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                it.kontaktpersoner shouldBe listOf()
                it.stedForGjennomforing shouldBe "Oslo"
                it.navRegion shouldBe Innlandet
                it.faneinnhold shouldBe null
                it.beskrivelse shouldBe null
                it.createdAt shouldNotBe null
            }

            tiltaksgjennomforinger.delete(Oppfolging1.id)

            tiltaksgjennomforinger.get(Oppfolging1.id) shouldBe null
        }

        test("Administratorer crud") {
            val gjennomforing = Oppfolging1.copy(
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id)?.administratorer.shouldContainExactlyInAnyOrder(
                TiltaksgjennomforingDto.Administrator(
                    navIdent = NavAnsattFixture.ansatt1.navIdent,
                    navn = "Donald Duck",
                ),
            )
        }

        test("navEnheter crud") {
            val gjennomforing = Oppfolging1.copy(
                id = UUID.randomUUID(),
                navEnheter = listOf("1", "2"),
            )

            val testDomain = MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetDbo(
                        navn = "Navn1",
                        enhetsnummer = "1",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                    ),
                    NavEnhetDbo(
                        navn = "Navn2",
                        enhetsnummer = "2",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                    ),
                    NavEnhetDbo(
                        navn = "Navn3",
                        enhetsnummer = "3",
                        status = NavEnhetStatus.AKTIV,
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                    ),
                ),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing),
            )
            testDomain.initialize(database.db)

            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().shouldNotBeNull().should {
                it.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhetDbo(
                        enhetsnummer = "1",
                        navn = "Navn1",
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                        status = NavEnhetStatus.AKTIV,
                    ),
                    NavEnhetDbo(
                        enhetsnummer = "2",
                        navn = "Navn2",
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                        status = NavEnhetStatus.AKTIV,
                    ),
                )
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(navEnheter = listOf("3", "1")))
            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhetDbo(
                        enhetsnummer = "1",
                        navn = "Navn1",
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                        status = NavEnhetStatus.AKTIV,
                    ),
                    NavEnhetDbo(
                        enhetsnummer = "3",
                        navn = "Navn3",
                        type = Norg2Type.LOKAL,
                        overordnetEnhet = null,
                        status = NavEnhetStatus.AKTIV,
                    ),
                )
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(navEnheter = listOf()))
            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.navEnheter.shouldBeEmpty()
            }
        }

        test("kontaktpersoner på tiltaksgjennomføring CRUD") {
            val gjennomforing = Oppfolging1.copy(
                kontaktpersoner = listOf(
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt1.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                        beskrivelse = "hei hei kontaktperson",
                    ),
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt2.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt2.hovedenhet),
                        beskrivelse = null,
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforing)

            val result = tiltaksgjennomforinger.get(gjennomforing.id)
            result?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                TiltaksgjennomforingKontaktperson(
                    navIdent = NavIdent("DD1"),
                    navn = "Donald Duck",
                    mobilnummer = "12345678",
                    epost = "donald.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                    beskrivelse = "hei hei kontaktperson",
                ),
                TiltaksgjennomforingKontaktperson(
                    navIdent = NavIdent("DD2"),
                    navn = "Dolly Duck",
                    mobilnummer = "48243214",
                    epost = "dolly.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                    beskrivelse = null,
                ),
            )
            val gjennomforingFjernetKontaktperson = gjennomforing.copy(
                kontaktpersoner = listOf(
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt1.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                        beskrivelse = null,
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforingFjernetKontaktperson)

            val oppdatertResult = tiltaksgjennomforinger.get(gjennomforingFjernetKontaktperson.id)
            oppdatertResult?.kontaktpersoner shouldBe listOf(
                TiltaksgjennomforingKontaktperson(
                    navIdent = NavIdent("DD1"),
                    navn = "Donald Duck",
                    mobilnummer = "12345678",
                    epost = "donald.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                    beskrivelse = null,
                ),
            )
        }

        test("arrangør kontaktperson") {
            val arrangorRepository = ArrangorRepository(database.db)

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
            arrangorRepository.upsertKontaktperson(thomas)
            arrangorRepository.upsertKontaktperson(jens)

            val gjennomforing = Oppfolging1.copy(arrangorKontaktpersoner = listOf(thomas.id))

            tiltaksgjennomforinger.upsert(gjennomforing)
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.arrangor.kontaktpersoner shouldContainExactly listOf(thomas)
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(arrangorKontaktpersoner = emptyList()))
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.arrangor.kontaktpersoner shouldHaveSize 0
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(arrangorKontaktpersoner = listOf(thomas.id, jens.id)))
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.arrangor.kontaktpersoner shouldContainExactlyInAnyOrder listOf(thomas, jens)
            }
        }

        test("getUpdatedAt") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            val firstUpdated = tiltaksgjennomforinger.getUpdatedAt(Oppfolging1.id).shouldNotBeNull()

            tiltaksgjennomforinger.upsert(Oppfolging1)
            val secondUpdated = tiltaksgjennomforinger.getUpdatedAt(Oppfolging1.id).shouldNotBeNull()

            secondUpdated.shouldBeAfter(firstUpdated)
        }

        test("Publisert for alle må settes eksplisitt") {
            val gjennomforing = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe false

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe true
        }

        test("skal vises til veileder basert til publisert og status") {
            val gjennomforing = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(gjennomforing)
            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe true

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, false)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe false

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.avbryt(gjennomforing.id, LocalDateTime.now(), AvbruttAarsak.Feilregistrering)

            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe false
        }

        test("faneinnhold") {
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

            val gjennomforing = Oppfolging1.copy(
                id = UUID.randomUUID(),
                faneinnhold = faneinnhold,
            )

            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.faneinnhold!!.forHvem!![0] shouldBe faneinnhold.forHvem!![0]
            }
        }

        test("amoKategoriserng") {
            val amo = AmoKategorisering.Norskopplaering(
                norskprove = true,
                innholdElementer = listOf(
                    AmoKategorisering.InnholdElement.ARBEIDSMARKEDSKUNNSKAP,
                    AmoKategorisering.InnholdElement.PRAKSIS,
                ),
            )
            val gjennomforing = Oppfolging1.copy(
                id = UUID.randomUUID(),
                amoKategorisering = amo,
            )

            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.amoKategorisering shouldBe amo
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(amoKategorisering = null))
            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.amoKategorisering shouldBe null
            }
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("filtrering på arrangør") {
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
            )
            tiltaksgjennomforinger.upsert(
                Oppfolging2.copy(arrangorId = ArrangorFixtures.underenhet2.id),
            )

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
            ).should {
                it.items.size shouldBe 1
                it.items[0].id shouldBe Oppfolging1.id
            }

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = listOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
            ).should {
                it.items.size shouldBe 1
                it.items[0].id shouldBe Oppfolging2.id
            }
        }

        test("søk på tiltaksarrangørs navn") {
            val arrangorer = ArrangorRepository(database.db)

            arrangorer.upsert(ArrangorFixtures.hovedenhet)
            arrangorer.upsert(ArrangorFixtures.underenhet1.copy(navn = "Underenhet Bergen"))
            arrangorer.upsert(ArrangorFixtures.underenhet2.copy(navn = "Underenhet Ålesun"))

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(arrangorId = ArrangorFixtures.underenhet1.id),
            )
            tiltaksgjennomforinger.upsert(
                Oppfolging2.copy(arrangorId = ArrangorFixtures.underenhet2.id),
            )

            tiltaksgjennomforinger.getAll(search = "bergen").should {
                it.items.size shouldBe 1
                it.items[0].arrangor.navn shouldBe "Underenhet Bergen"
            }

            tiltaksgjennomforinger.getAll(search = "under").should {
                it.items.size shouldBe 2
            }
        }

        test("skal migreres henter kun der tiltakstypen har egen tiltakskode") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.upsert(EnkelAmo1)

            tiltaksgjennomforinger.getAll().should {
                it.totalCount shouldBe 1
                it.items[0].id shouldBe Oppfolging1.id
            }
        }

        test("filtrering på opphav") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.setOpphav(Oppfolging1.id, ArenaMigrering.Opphav.ARENA)
            tiltaksgjennomforinger.upsert(Oppfolging2)

            tiltaksgjennomforinger.getAll(opphav = null).should {
                it.totalCount shouldBe 2
            }

            tiltaksgjennomforinger.getAll(opphav = ArenaMigrering.Opphav.ARENA).should {
                it.totalCount shouldBe 1
                it.items[0].id shouldBe Oppfolging1.id
            }

            tiltaksgjennomforinger.getAll(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE).should {
                it.totalCount shouldBe 1
                it.items[0].id shouldBe Oppfolging2.id
            }
        }

        test("filtrering på avtale") {
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                ),
            )

            tiltaksgjennomforinger.upsert(
                Oppfolging2.copy(
                    avtaleId = AvtaleFixtures.AFT.id,
                ),
            )

            val result = tiltaksgjennomforinger.getAll(
                avtaleId = AvtaleFixtures.oppfolging.id,
            ).items
            result shouldHaveSize 1
            result.first().id shouldBe Oppfolging1.id
        }

        test("filtrer vekk gjennomføringer basert på sluttdato") {
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    sluttDato = LocalDate.of(2023, 12, 31),
                ),
            )

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.of(2023, 6, 29),
                ),
            )

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.of(2022, 12, 31),
                ),
            )

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = null,
                ),
            )

            tiltaksgjennomforinger.getAll(sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate)
                .should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 3
                    gjennomforinger.map { it.sluttDato } shouldContainExactlyInAnyOrder listOf(
                        LocalDate.of(2023, 12, 31),
                        LocalDate.of(2023, 6, 29),
                        null,
                    )
                }
        }

        test("filtrer på nav_enhet") {
            val tg1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf(Lillehammer.enhetsnummer))
            tiltaksgjennomforinger.upsert(tg1)

            val tg2 = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(tg2)
            Query("update tiltaksgjennomforing set arena_ansvarlig_enhet = '${Lillehammer.enhetsnummer}' where id = '${tg2.id}'")
                .asUpdate
                .let { database.db.run(it) }

            val tg3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf(Gjovik.enhetsnummer))
            tiltaksgjennomforinger.upsert(tg3)

            val tg4 = Oppfolging1.copy(
                id = UUID.randomUUID(),
                navEnheter = listOf(Lillehammer.enhetsnummer, Gjovik.enhetsnummer),
            )
            tiltaksgjennomforinger.upsert(tg4)

            tiltaksgjennomforinger.getAll(navEnheter = listOf(Lillehammer.enhetsnummer)).should {
                it.totalCount shouldBe 3
                it.items.map { tg -> tg.id } shouldContainAll listOf(tg1.id, tg2.id, tg4.id)
            }
        }

        test("administrator") {
            val tg1 = Oppfolging1.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            val tg2 = Oppfolging1.copy(
                id = UUID.randomUUID(),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
            )

            tiltaksgjennomforinger.upsert(tg1)
            tiltaksgjennomforinger.upsert(tg2)

            tiltaksgjennomforinger.getAll(administratorNavIdent = NavAnsattFixture.ansatt1.navIdent).should {
                it.totalCount shouldBe 2
                it.items.map { tg -> tg.id } shouldContainAll listOf(tg1.id, tg2.id)
            }

            tiltaksgjennomforinger.getAll(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent).should {
                it.totalCount shouldBe 1
                it.items.map { tg -> tg.id } shouldContainAll listOf(tg2.id)
            }
        }

        test("filtrering på tiltakstype") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
                gjennomforinger = listOf(Oppfolging1, VTA1, AFT1),
            ).initialize(database.db)

            tiltaksgjennomforinger.getAll(tiltakstypeIder = listOf(TiltakstypeFixtures.Oppfolging.id))
                .should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 1
                    gjennomforinger[0].navn shouldBe Oppfolging1.navn
                }

            tiltaksgjennomforinger.getAll(
                tiltakstypeIder = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.VTA.id),
            ).should { (totalCount, gjennomforinger) ->
                totalCount shouldBe 2
                gjennomforinger.map { it.navn } shouldContainExactlyInAnyOrder listOf(VTA1.navn, AFT1.navn)
            }
        }

        test("filtrering på Nav-enhet") {
            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    Innlandet,
                    Gjovik,
                    Lillehammer,
                    Sel,
                ),
                avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
                gjennomforinger = listOf(
                    Oppfolging1.copy(navEnheter = listOf(Gjovik.enhetsnummer)),
                    VTA1.copy(navEnheter = listOf(Lillehammer.enhetsnummer)),
                    AFT1.copy(navEnheter = listOf(Sel.enhetsnummer, Gjovik.enhetsnummer)),
                ),
            ).initialize(database.db)

            tiltaksgjennomforinger.getAll(navEnheter = listOf(Gjovik.enhetsnummer))
                .should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 2
                    gjennomforinger.map { it.navn } shouldContainExactlyInAnyOrder listOf(Oppfolging1.navn, AFT1.navn)
                }

            tiltaksgjennomforinger.getAll(navEnheter = listOf(Lillehammer.enhetsnummer, Sel.enhetsnummer))
                .should { (totalCount, gjennomforinger) ->
                    totalCount shouldBe 2
                    gjennomforinger.map { it.navn } shouldContainExactlyInAnyOrder listOf(VTA1.navn, AFT1.navn)
                }

            tiltaksgjennomforinger.getAll(navEnheter = listOf(Innlandet.enhetsnummer))
                .should { (totalCount) ->
                    totalCount shouldBe 3
                }
        }
    }

    test("pagination") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        (1..10).forEach {
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    navn = "$it".padStart(2, '0'),
                ),
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
            val (totalCount, items) = tiltaksgjennomforinger.getAll(pagination)

            items.size shouldBe expectedSize
            items.firstOrNull()?.navn shouldBe expectedFirst
            items.lastOrNull()?.navn shouldBe expectedLast

            totalCount shouldBe expectedTotalCount
        }
    }

    context("åpent for påmelding") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("skal sette åpent for påmelding") {
            val gjennomforing = TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                id = UUID.randomUUID(),
            )
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().apentForPamelding shouldBe true

            tiltaksgjennomforinger.setApentForPamelding(gjennomforing.id, false)

            tiltaksgjennomforinger.get(gjennomforing.id).shouldNotBeNull().apentForPamelding shouldBe false
        }
    }

    context("tiltaksgjennomforingstatus") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val dagensDato = LocalDate.now()
        val enManedFrem = dagensDato.plusMonths(1)
        val enManedTilbake = dagensDato.minusMonths(1)
        val toManederFrem = dagensDato.plusMonths(2)
        val toManederTilbake = dagensDato.minusMonths(2)

        test("status AVLYST og AVBRUTT utledes fra avbrutt-tidspunkt") {
            forAll(
                row(enManedTilbake, enManedFrem, enManedTilbake.minusDays(1), TiltaksgjennomforingStatus.AVLYST),
                row(enManedFrem, toManederFrem, dagensDato, TiltaksgjennomforingStatus.AVLYST),
                row(dagensDato, toManederFrem, dagensDato, TiltaksgjennomforingStatus.AVBRUTT),
                row(enManedTilbake, enManedFrem, enManedTilbake.plusDays(3), TiltaksgjennomforingStatus.AVBRUTT),
                row(enManedFrem, toManederFrem, enManedFrem.plusMonths(2), TiltaksgjennomforingStatus.AVBRUTT),
            ) { startDato, sluttDato, avbruttDato, expectedStatus ->
                tiltaksgjennomforinger.upsert(AFT1.copy(startDato = startDato, sluttDato = sluttDato))

                tiltaksgjennomforinger.avbryt(
                    AFT1.id,
                    avbruttDato.atStartOfDay(),
                    AvbruttAarsak.Feilregistrering,
                )

                tiltaksgjennomforinger.get(AFT1.id).shouldNotBeNull().status.status shouldBe expectedStatus
            }
        }

        test("hvis ikke avbrutt så blir status utledet basert på dagens dato") {
            forAll(
                row(toManederTilbake, enManedTilbake, TiltaksgjennomforingStatus.AVSLUTTET),
                row(enManedTilbake, enManedFrem, TiltaksgjennomforingStatus.GJENNOMFORES),
                row(enManedTilbake, null, TiltaksgjennomforingStatus.GJENNOMFORES),
                row(dagensDato, dagensDato, TiltaksgjennomforingStatus.GJENNOMFORES),
                row(enManedFrem, toManederFrem, TiltaksgjennomforingStatus.PLANLAGT),
                row(enManedFrem, null, TiltaksgjennomforingStatus.PLANLAGT),
            ) { startDato, sluttDato, status ->
                tiltaksgjennomforinger.upsert(AFT1.copy(startDato = startDato, sluttDato = sluttDato))

                tiltaksgjennomforinger.get(AFT1.id).shouldNotBeNull().status.status shouldBe status
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

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("Skal fjerne kontaktperson fra koblingstabell") {
            testDomain.initialize(database.db)

            tiltaksgjennomforinger.get(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson1.id)
            }
            tiltaksgjennomforinger.get(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
            }

            database.db.transaction { tx ->
                tiltaksgjennomforinger.frikobleKontaktpersonFraGjennomforing(
                    kontaktperson1.id,
                    Oppfolging1.id,
                    tx,
                )
            }

            tiltaksgjennomforinger.get(testDomain.gjennomforinger[0].id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner.shouldBeEmpty()
            }
            tiltaksgjennomforinger.get(testDomain.gjennomforinger[1].id).shouldNotBeNull().should {
                it.arrangor.kontaktpersoner.first().id.shouldBe(kontaktperson2.id)
            }
        }
    }
})
