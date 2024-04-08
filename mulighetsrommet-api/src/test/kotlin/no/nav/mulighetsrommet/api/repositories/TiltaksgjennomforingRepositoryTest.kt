package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
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
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingKontaktperson
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.ArenaOppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

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
                it.tiltakstype shouldBe TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = TiltakstypeFixtures.Oppfolging.id,
                    navn = TiltakstypeFixtures.Oppfolging.navn,
                    arenaKode = TiltakstypeFixtures.Oppfolging.arenaKode,
                )
                it.navn shouldBe Oppfolging1.navn
                it.tiltaksnummer shouldBe null
                it.arrangor shouldBe TiltaksgjennomforingAdminDto.ArrangorUnderenhet(
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    navn = ArrangorFixtures.underenhet1.navn,
                    slettet = false,
                    kontaktpersoner = emptyList(),
                )
                it.startDato shouldBe Oppfolging1.startDato
                it.sluttDato shouldBe Oppfolging1.sluttDato
                it.arenaAnsvarligEnhet shouldBe null
                it.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET
                it.apentForInnsok shouldBe true
                it.antallPlasser shouldBe 12
                it.avtaleId shouldBe Oppfolging1.avtaleId
                it.administratorer shouldBe listOf(
                    TiltaksgjennomforingAdminDto.Administrator(
                        navIdent = NavIdent("DD1"),
                        navn = "Donald Duck",
                    ),
                )
                it.navEnheter shouldBe listOf(Gjovik)
                it.sanityId shouldBe null
                it.oppstart shouldBe TiltaksgjennomforingOppstartstype.LOPENDE
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                it.kontaktpersoner shouldBe listOf()
                it.stedForGjennomforing shouldBe "Oslo"
                it.navRegion shouldBe NavEnhetFixtures.Innlandet
                it.faneinnhold shouldBe null
                it.beskrivelse shouldBe null
                it.createdAt shouldNotBe null
            }

            tiltaksgjennomforinger.delete(Oppfolging1.id)

            tiltaksgjennomforinger.get(Oppfolging1.id) shouldBe null
        }

        test("CRUD ArenaTiltaksgjennomforing") {
            val navEnheter = NavEnhetRepository(database.db)
            navEnheter.upsert(NavEnhetFixtures.Innlandet).shouldBeRight()
            val gjennomforingId = UUID.randomUUID()
            val gjennomforingFraArena = ArenaTiltaksgjennomforingDbo(
                id = gjennomforingId,
                navn = "Tiltak for dovne giraffer",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "2023#1",
                arrangorOrganisasjonsnummer = "123456789",
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = NavEnhetFixtures.Innlandet.enhetsnummer,
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                apentForInnsok = false,
                antallPlasser = 10,
                avtaleId = null,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                deltidsprosent = 100.0,
            )

            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(gjennomforingFraArena)

            tiltaksgjennomforinger.get(gjennomforingFraArena.id) should {
                it.shouldNotBeNull()
                it.id shouldBe gjennomforingId
                it.navn shouldBe "Tiltak for dovne giraffer"
                it.tiltakstype shouldBe TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = TiltakstypeFixtures.Oppfolging.id,
                    navn = TiltakstypeFixtures.Oppfolging.navn,
                    arenaKode = TiltakstypeFixtures.Oppfolging.arenaKode,
                )
                it.tiltaksnummer shouldBe "2023#1"
                it.arrangor shouldBe TiltaksgjennomforingAdminDto.ArrangorUnderenhet(
                    id = ArrangorFixtures.hovedenhet.id,
                    organisasjonsnummer = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                    navn = ArrangorFixtures.hovedenhet.navn,
                    slettet = false,
                    kontaktpersoner = emptyList(),
                )
                it.startDato shouldBe LocalDate.of(2023, 1, 1)
                it.sluttDato shouldBe LocalDate.of(2023, 2, 2)
                it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = "NAV Innlandet", enhetsnummer = "0400")
                it.apentForInnsok shouldBe false
                it.antallPlasser shouldBe 10
                it.avtaleId shouldBe null
                it.oppstart shouldBe TiltaksgjennomforingOppstartstype.FELLES
                it.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET
                it.administratorer shouldBe emptyList()
                it.navEnheter shouldBe emptyList()
                it.navRegion shouldBe null
                it.sanityId shouldBe null
                it.opphav shouldBe ArenaMigrering.Opphav.ARENA
                it.kontaktpersoner shouldBe emptyList()
                it.stedForGjennomforing shouldBe null
                it.faneinnhold shouldBe null
                it.beskrivelse shouldBe null
                it.createdAt shouldNotBe null
            }
        }

        test("upsert endrer ikke opphav om det allerede er satt") {
            val id1 = UUID.randomUUID()
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(ArenaOppfolging1.copy(id = id1))
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(id = id1))
            tiltaksgjennomforinger.get(id1).shouldNotBeNull().should {
                it.opphav shouldBe ArenaMigrering.Opphav.ARENA
            }

            val id2 = UUID.randomUUID()
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(id = id2))
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(ArenaOppfolging1.copy(id = id2))
            tiltaksgjennomforinger.get(id2).shouldNotBeNull().should {
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
            }
        }

        test("arena overskriver ikke oppstart") {
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(ArenaOppfolging1)
            tiltaksgjennomforinger.get(ArenaOppfolging1.id) should {
                it!!.oppstart shouldBe TiltaksgjennomforingOppstartstype.FELLES
            }

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = ArenaOppfolging1.id,
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                ),
            )

            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(ArenaOppfolging1)
            tiltaksgjennomforinger.get(ArenaOppfolging1.id) should {
                it!!.oppstart shouldBe TiltaksgjennomforingOppstartstype.LOPENDE
            }
        }

        test("Administratorer crud") {
            val gjennomforing = Oppfolging1.copy(
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id)?.administratorer.shouldContainExactlyInAnyOrder(
                TiltaksgjennomforingAdminDto.Administrator(
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

        test("update sanity_id") {
            val id = UUID.randomUUID()

            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.updateSanityTiltaksgjennomforingId(Oppfolging1.id, id)
            tiltaksgjennomforinger.get(Oppfolging1.id).should {
                it!!.sanityId.shouldBe(id)
            }
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

        test("håndterer at arena-ansvarlig-enhet ikke er en kjent NAV-enhet") {
            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(ArenaOppfolging1.copy(arenaAnsvarligEnhet = "9999"))

            tiltaksgjennomforinger.get(ArenaOppfolging1.id).shouldNotBeNull().arenaAnsvarligEnhet.shouldBe(
                ArenaNavEnhet(navn = null, enhetsnummer = "9999"),
            )
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
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe true

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, false)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe false

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.avbryt(gjennomforing.id, LocalDateTime.now(), AvbruttAarsak.Feilregistrering)

            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe false
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
    }

    context("Filtrering på tiltaksgjennomforingstatus") {
        val tiltaksgjennomforingAktiv = AFT1
        val tiltaksgjennomforingAvsluttetDato = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            sluttDato = LocalDate.now().minusMonths(1),
        )
        val tiltaksgjennomforingAvbrutt = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVBRUTT,
        )
        val tiltaksgjennomforingAvlyst = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVLYST,
        )
        val tiltaksgjennomforingPlanlagt = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            startDato = LocalDate.now().plusDays(1),
            sluttDato = LocalDate.now().plusDays(10),
        )

        beforeAny {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAktiv)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvsluttetDato)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvbrutt)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingPlanlagt)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvlyst)
        }

        test("filtrer på avbrutt") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(TiltaksgjennomforingStatus.AVBRUTT),
            )

            result.totalCount shouldBe 1
            result.items[0].id shouldBe tiltaksgjennomforingAvbrutt.id
        }

        test("filtrer på avsluttet") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(TiltaksgjennomforingStatus.AVSLUTTET),
            )

            result.totalCount shouldBe 1
            result.items[0].id shouldBe tiltaksgjennomforingAvsluttetDato.id
        }

        test("filtrer på gjennomføres") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(TiltaksgjennomforingStatus.GJENNOMFORES),
            )

            result.totalCount shouldBe 1
            result.items[0].id shouldBe tiltaksgjennomforingAktiv.id
        }

        test("filtrer på avlyst") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(TiltaksgjennomforingStatus.AVLYST),
            )

            result.totalCount shouldBe 1
            result.items[0].id shouldBe tiltaksgjennomforingAvlyst.id
        }

        test("filtrer på PLANLAGT") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(TiltaksgjennomforingStatus.PLANLAGT),
            )

            result.totalCount shouldBe 1
            result.items[0].id shouldBe tiltaksgjennomforingPlanlagt.id
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val navEnheter = NavEnhetRepository(database.db)

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

            tiltaksgjennomforinger.getAll(
                search = "bergen",
            ).should {
                it.items.size shouldBe 1
                it.items[0].arrangor.navn shouldBe "Underenhet Bergen"
            }

            tiltaksgjennomforinger.getAll(
                search = "under",
            ).should {
                it.items.size shouldBe 2
            }
        }

        test("skal migreres henter kun der tiltakstypen har egen tiltakskode") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.upsert(EnkelAmo1)

            tiltaksgjennomforinger.getAll(skalMigreres = true).should {
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
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Navn1",
                    enhetsnummer = "1",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Navn2",
                    enhetsnummer = "2",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()

            val tg1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1"))
            tiltaksgjennomforinger.upsert(tg1)

            val tg2 = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(tg2)
            Query("update tiltaksgjennomforing set arena_ansvarlig_enhet = '1' where id = '${tg2.id}'")
                .asUpdate
                .let { database.db.run(it) }

            val tg3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("2"))
            tiltaksgjennomforinger.upsert(tg3)

            val tg4 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1", "2"))
            tiltaksgjennomforinger.upsert(tg4)

            tiltaksgjennomforinger.getAll(navEnheter = listOf("1")).should {
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

        test("filtrering på NAV-enhet") {
            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    NavEnhetFixtures.Innlandet,
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

            tiltaksgjennomforinger.getAll(navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer))
                .should { (totalCount) ->
                    totalCount shouldBe 3
                }
        }
    }

    context("Hente tiltaksgjennomføringer som nærmer seg sluttdato") {
        test("Skal hente gjennomføringer som er 14, 7 eller 1 dag til sluttdato") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val oppfolging14Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 30),
                administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
            )
            val oppfolging7Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 23),
                administratorer = emptyList(),
            )
            val oppfolging1Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 17),
                administratorer = emptyList(),
            )
            val oppfolging10Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 26),
            )
            tiltaksgjennomforinger.upsert(oppfolging14Dager)
            tiltaksgjennomforinger.upsert(oppfolging7Dager)
            tiltaksgjennomforinger.upsert(oppfolging1Dager)
            tiltaksgjennomforinger.upsert(oppfolging10Dager)

            val result = tiltaksgjennomforinger.getAllGjennomforingerSomNarmerSegSluttdato(
                currentDate = LocalDate.of(2023, 5, 16),
            )

            result.map { Pair(it.id, it.administratorer) } shouldContainExactlyInAnyOrder listOf(
                Pair(oppfolging14Dager.id, listOf(NavIdent("DD1"))),
                Pair(oppfolging7Dager.id, listOf()),
                Pair(oppfolging1Dager.id, listOf()),
            )
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

    context("getAllVeilederflateTiltaksgjennomforing") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val oppfolgingSanityId = UUID.randomUUID()
        val arbeidstreningSanityId = UUID.randomUUID()

        beforeEach {
            Query("update tiltakstype set sanity_id = '$oppfolgingSanityId' where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set innsatsgruppe = '${Innsatsgruppe.STANDARD_INNSATS}' where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set innsatsgruppe = '${Innsatsgruppe.STANDARD_INNSATS}' where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }

            tiltaksgjennomforinger.upsert(Oppfolging1.copy(sluttDato = null, navEnheter = listOf("2990")))
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1.copy(navEnheter = listOf("2990")))
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)
        }

        test("skal filtrere basert på om tiltaket er publisert") {
            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2

            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, false)

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 1

            tiltaksgjennomforinger.setPublisert(AFT1.id, false)

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 0
        }

        test("skal bare returnere tiltak markert med tiltakskode definert") {
            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2

            Query("update tiltakstype set tiltakskode = null where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 1

            Query("update tiltakstype set tiltakskode = null where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 0
        }

        test("skal filtrere basert på innsatsgruppe") {
            Query("update tiltakstype set innsatsgruppe = '${Innsatsgruppe.SPESIELT_TILPASSET_INNSATS}' where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set innsatsgruppe = '${Innsatsgruppe.STANDARD_INNSATS}' where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 0

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe AFT1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.SPESIELT_TILPASSET_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe Oppfolging1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SPESIELT_TILPASSET_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2
        }

        test("skal filtrere på brukers enheter") {
            val enheter = NavEnhetRepository(database.db)
            enheter.upsert(NavEnhetFixtures.Oslo)
            enheter.upsert(NavEnhetFixtures.Innlandet)

            tiltaksgjennomforinger.upsert(Oppfolging1.copy(sluttDato = null, navEnheter = listOf("2990", "0400")))
            tiltaksgjennomforinger.upsert(AFT1.copy(navEnheter = listOf("2990", "0300")))

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("0400"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe Oppfolging1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("0300"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe AFT1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("0400", "0300"),
            ) shouldHaveSize 2

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2
        }

        test("skal filtrere basert på tiltakstype sanity Id") {
            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                sanityTiltakstypeIds = null,
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                sanityTiltakstypeIds = listOf(oppfolgingSanityId),
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe Oppfolging1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                sanityTiltakstypeIds = listOf(arbeidstreningSanityId),
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe AFT1.navn
            }
        }

        test("skal filtrere basert fritekst i navn") {
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(sluttDato = null, navn = "erik"))
            tiltaksgjennomforinger.upsert(AFT1.copy(navn = "frank"))

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                search = "rik",
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("0502"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe "erik"
            }
        }

        test("skal filtrere basert på apent_for_innsok") {
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    sluttDato = null,
                    apentForInnsok = true,
                    navEnheter = listOf("2990"),
                ),
            )
            tiltaksgjennomforinger.upsert(AFT1.copy(apentForInnsok = false, navEnheter = listOf("2990")))

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                apentForInnsok = true,
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe Oppfolging1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                apentForInnsok = false,
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ).should {
                it shouldHaveSize 1
                it[0].navn shouldBe AFT1.navn
            }

            tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
                apentForInnsok = null,
                innsatsgrupper = listOf(Innsatsgruppe.STANDARD_INNSATS),
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2
        }
    }

    context("Update åpent for innsøk") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("Skal sette åpent for innsøk til false for tiltak med felles oppstartstype og startdato i dag") {
            val dagensDatoMock = LocalDate.of(2024, 3, 6)
            val jobbklubbStartDatoIFremtiden = TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                id = UUID.randomUUID(),
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                startDato = LocalDate.of(2024, 5, 1),
                apentForInnsok = true,
            )
            tiltaksgjennomforinger.upsert(jobbklubbStartDatoIFremtiden)

            val jobbklubbStartDatoIDag = TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                id = UUID.randomUUID(),
                navn = "Jobbklubb 2",
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                startDato = dagensDatoMock,
                apentForInnsok = true,
            )
            tiltaksgjennomforinger.upsert(jobbklubbStartDatoIDag)
            val jobbklubbStartDatoIDagFraArena = TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                id = UUID.randomUUID(),
                navn = "Jobbklubb 2 fra Arena",
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                startDato = dagensDatoMock,
                apentForInnsok = true,
            )
            tiltaksgjennomforinger.upsert(jobbklubbStartDatoIDagFraArena)
            tiltaksgjennomforinger.setOpphav(jobbklubbStartDatoIDagFraArena.id, ArenaMigrering.Opphav.ARENA)

            val jobbklubbStartDatoHarPassert = TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                id = UUID.randomUUID(),
                navn = "Jobbklubb 3",
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                startDato = LocalDate.of(2024, 1, 1),
                apentForInnsok = false,
            )
            tiltaksgjennomforinger.upsert(jobbklubbStartDatoHarPassert)

            forAll(
                row(jobbklubbStartDatoIFremtiden.id, true),
                row(jobbklubbStartDatoIDag.id, false),
                row(jobbklubbStartDatoIDagFraArena.id, true),
                row(jobbklubbStartDatoHarPassert.id, false),
            ) { id, apentForInnsok ->
                database.db.transaction { tx ->
                    tiltaksgjennomforinger.lukkApentForInnsokForTiltakMedStartdatoForDato(
                        dagensDato = dagensDatoMock,
                        tx = tx,
                    )
                }
                tiltaksgjennomforinger.get(id)?.apentForInnsok shouldBe apentForInnsok
            }
        }
    }

    context("tiltaksgjennomforingstatus") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val dagensDato = LocalDate.now()
        val enManedFrem = dagensDato.plusMonths(1)
        val enManedTilbake = dagensDato.minusMonths(1)
        val toManederFrem = dagensDato.plusMonths(2)
        val toManederTilbake = dagensDato.minusMonths(2)

        test("avbrutt før start er avlyst") {
            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedTilbake,
                    sluttDato = enManedFrem,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, enManedTilbake.atStartOfDay().minusDays(1), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVLYST

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = toManederTilbake,
                    sluttDato = enManedTilbake,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, toManederTilbake.atStartOfDay().minusYears(1), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVLYST

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedFrem,
                    sluttDato = toManederFrem,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, enManedFrem.atStartOfDay().minusMonths(1), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVLYST
        }

        test("avbrutt etter start er avbrutt") {
            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedTilbake,
                    sluttDato = enManedFrem,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, enManedTilbake.atStartOfDay().plusDays(3), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVBRUTT

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = toManederTilbake,
                    sluttDato = enManedTilbake,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, toManederTilbake.atStartOfDay().plusYears(2), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVBRUTT

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedFrem,
                    sluttDato = toManederFrem,
                ),
            )
            tiltaksgjennomforinger.avbryt(AFT1.id, enManedFrem.atStartOfDay().plusMonths(2), AvbruttAarsak.Feilregistrering)
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVBRUTT
        }

        test("hvis ikke avbrutt") {
            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedTilbake,
                    sluttDato = enManedFrem,
                ),
            )
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = toManederTilbake,
                    sluttDato = enManedTilbake,
                ),
            )
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedFrem,
                    sluttDato = toManederFrem,
                ),
            )
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.PLANLAGT
        }

        test("hvis sluttdato mangler så regnes den som pågående") {
            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedTilbake,
                    sluttDato = null,
                ),
            )
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES

            tiltaksgjennomforinger.upsert(
                AFT1.copy(
                    startDato = enManedFrem,
                    sluttDato = null,
                ),
            )
            tiltaksgjennomforinger.get(AFT1.id)!!.status shouldBe TiltaksgjennomforingStatus.PLANLAGT
        }
    }
})
