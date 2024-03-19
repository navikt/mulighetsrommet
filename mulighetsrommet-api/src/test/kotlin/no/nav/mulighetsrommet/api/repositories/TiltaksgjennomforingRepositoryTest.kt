package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.*
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.ArenaOppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.EnkelAmo1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
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
                it.arrangor shouldBe TiltaksgjennomforingAdminDto.Arrangor(
                    id = VirksomhetFixtures.underenhet1.id,
                    organisasjonsnummer = VirksomhetFixtures.underenhet1.organisasjonsnummer,
                    navn = VirksomhetFixtures.underenhet1.navn,
                    slettet = false,
                    kontaktpersoner = emptyList(),
                )
                it.startDato shouldBe Oppfolging1.startDato
                it.sluttDato shouldBe Oppfolging1.sluttDato
                it.arenaAnsvarligEnhet shouldBe null
                it.status shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
                it.apentForInnsok shouldBe true
                it.antallPlasser shouldBe 12
                it.avtaleId shouldBe Oppfolging1.avtaleId
                it.administratorer shouldBe listOf(TiltaksgjennomforingAdminDto.Administrator(navIdent = NavIdent("DD1"), navn = "Donald Duck"))
                it.navEnheter shouldBe listOf(NavEnhetFixtures.Gjovik)
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
                it.arrangor shouldBe TiltaksgjennomforingAdminDto.Arrangor(
                    id = VirksomhetFixtures.hovedenhet.id,
                    organisasjonsnummer = VirksomhetFixtures.hovedenhet.organisasjonsnummer,
                    navn = VirksomhetFixtures.hovedenhet.navn,
                    slettet = false,
                    kontaktpersoner = emptyList(),
                )
                it.startDato shouldBe LocalDate.of(2023, 1, 1)
                it.sluttDato shouldBe LocalDate.of(2023, 2, 2)
                it.arenaAnsvarligEnhet shouldBe NavEnhetFixtures.Innlandet
                it.apentForInnsok shouldBe false
                it.antallPlasser shouldBe 10
                it.avtaleId shouldBe null
                it.oppstart shouldBe TiltaksgjennomforingOppstartstype.FELLES
                it.status shouldBe Tiltaksgjennomforingsstatus.AVSLUTTET
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
                virksomheter = listOf(VirksomhetFixtures.hovedenhet, VirksomhetFixtures.underenhet1),
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

        test("virksomhet_kontaktperson") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val thomas = VirksomhetKontaktperson(
                id = UUID.randomUUID(),
                virksomhetId = VirksomhetFixtures.hovedenhet.id,
                navn = "Thomas",
                telefon = "22222222",
                epost = "thomas@thetrain.co.uk",
                beskrivelse = "beskrivelse",
            )
            val jens = VirksomhetKontaktperson(
                id = UUID.randomUUID(),
                virksomhetId = VirksomhetFixtures.hovedenhet.id,
                navn = "Jens",
                telefon = "22222224",
                epost = "jens@theshark.co.uk",
                beskrivelse = "beskrivelse2",
            )
            virksomhetRepository.upsertKontaktperson(thomas)
            virksomhetRepository.upsertKontaktperson(jens)

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

            tiltaksgjennomforinger.get(ArenaOppfolging1.id).shouldNotBeNull().arenaAnsvarligEnhet.shouldBeNull()
        }

        test("Publisert for alle må settes eksplisitt") {
            val gjennomforing = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe false

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisert shouldBe true
        }

        test("skal vises til veileder basert til publisert og avslutningsstatus") {
            val gjennomforing = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.setAvslutningsstatus(gjennomforing.id, Avslutningsstatus.AVSLUTTET)
            tiltaksgjennomforinger.setPublisert(gjennomforing.id, false)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe false

            tiltaksgjennomforinger.setPublisert(gjennomforing.id, true)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe false

            tiltaksgjennomforinger.setAvslutningsstatus(gjennomforing.id, Avslutningsstatus.IKKE_AVSLUTTET)
            tiltaksgjennomforinger.get(gjennomforing.id)?.publisertForAlle shouldBe true
        }
    }

    context("skal migreres") {
        test("skal migreres henter kun der tiltakstypen er definert") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.upsert(EnkelAmo1)

            tiltaksgjennomforinger.getAll(skalMigreres = true).should {
                it.first shouldBe 1
                it.second[0].id shouldBe Oppfolging1.id
            }
        }
    }

    test("get by opphav") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        tiltaksgjennomforinger.upsert(Oppfolging1)
        tiltaksgjennomforinger.setOpphav(Oppfolging1.id, ArenaMigrering.Opphav.ARENA)
        tiltaksgjennomforinger.upsert(Oppfolging2)

        tiltaksgjennomforinger.getAll(opphav = null).should {
            it.first shouldBe 2
        }

        tiltaksgjennomforinger.getAll(opphav = ArenaMigrering.Opphav.ARENA).should {
            it.first shouldBe 1
            it.second[0].id shouldBe Oppfolging1.id
        }

        tiltaksgjennomforinger.getAll(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE).should {
            it.first shouldBe 1
            it.second[0].id shouldBe Oppfolging2.id
        }
    }

    context("Filtrer på avtale") {
        test("Kun gjennomforinger tilhørende avtale blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                ),
            )

            val result = tiltaksgjennomforinger.getAll(
                avtaleId = AvtaleFixtures.oppfolging.id,
            )
                .second
            result shouldHaveSize 1
            result.first().id shouldBe Oppfolging1.id
        }
    }

    context("Filtrer på arrangør") {
        test("Kun gjennomføringer tilhørende arrangør blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(arrangorVirksomhetId = VirksomhetFixtures.underenhet1.id),
            )
            tiltaksgjennomforinger.upsert(
                Oppfolging2.copy(arrangorVirksomhetId = VirksomhetFixtures.underenhet2.id),
            )

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = listOf(VirksomhetFixtures.underenhet1.organisasjonsnummer),
            ).should {
                it.second.size shouldBe 1
                it.second[0].id shouldBe Oppfolging1.id
            }

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = listOf(VirksomhetFixtures.underenhet2.organisasjonsnummer),
            ).should {
                it.second.size shouldBe 1
                it.second[0].id shouldBe Oppfolging2.id
            }
        }
    }

    context("Cutoffdato") {
        test("Gamle tiltaksgjennomføringer blir ikke tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

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

            val result =
                tiltaksgjennomforinger.getAll().second
            result shouldHaveSize 2
        }

        test("Tiltaksgjennomføringer med sluttdato som er null blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = null,
                ),
            )

            tiltaksgjennomforinger.getAll()
                .second shouldHaveSize 2
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
                currentDate = LocalDate.of(
                    2023,
                    5,
                    16,
                ),
            )

            result.map { Pair(it.id, it.administratorer) } shouldContainExactlyInAnyOrder listOf(
                Pair(oppfolging14Dager.id, listOf(NavIdent("DD1"))),
                Pair(oppfolging7Dager.id, listOf()),
                Pair(oppfolging1Dager.id, listOf()),
            )
        }
    }

    context("Filtrering på tiltaksgjennomforingstatus") {
        val dagensDato = LocalDate.of(2023, 2, 1)

        val tiltaksgjennomforingAktiv = AFT1
        val tiltaksgjennomforingAvsluttetStatus = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        )
        val tiltaksgjennomforingAvsluttetDato = ArenaOppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            sluttDato = LocalDate.of(2023, 1, 31),
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
            startDato = LocalDate.of(2023, 2, 2),
        )

        beforeAny {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAktiv)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvsluttetStatus)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvsluttetDato)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvbrutt)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingPlanlagt)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(tiltaksgjennomforingAvlyst)
        }

        test("filtrer på avbrutt") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                dagensDato = dagensDato,
                statuser = listOf(Tiltaksgjennomforingsstatus.AVBRUTT),
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvbrutt.id
        }

        test("filtrer på avsluttet") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                dagensDato = dagensDato,
                statuser = listOf(Tiltaksgjennomforingsstatus.AVSLUTTET),
            )

            result.second shouldHaveSize 2
            result.second.map { it.id }
                .shouldContainAll(tiltaksgjennomforingAvsluttetDato.id, tiltaksgjennomforingAvsluttetStatus.id)
        }

        test("filtrer på gjennomføres") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(Tiltaksgjennomforingsstatus.GJENNOMFORES),
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAktiv.id
        }

        test("filtrer på avlyst") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(Tiltaksgjennomforingsstatus.AVLYST),
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvlyst.id
        }

        test("filtrer på åpent for innsøk") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                statuser = listOf(Tiltaksgjennomforingsstatus.PLANLAGT),
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingPlanlagt.id
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val navEnheter = NavEnhetRepository(database.db)

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
                it.first shouldBe 3
                it.second.map { tg -> tg.id } shouldContainAll listOf(tg1.id, tg2.id, tg4.id)
            }
        }

        test("filtrer på nav_region") {
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "NavRegion",
                    enhetsnummer = "0100",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Navn1",
                    enhetsnummer = "0101",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Navn2",
                    enhetsnummer = "0102",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "0100",
                ),
            ).shouldBeRight()

            val tg1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("0101"), navRegion = "2990")
            tiltaksgjennomforinger.upsert(tg1)

            val tg2 = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(tg2)
            Query("update tiltaksgjennomforing set arena_ansvarlig_enhet = '0101' where id = '${tg2.id}'")
                .asUpdate
                .let { database.db.run(it) }

            val tg3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("0102"), navRegion = "0100")
            tiltaksgjennomforinger.upsert(tg3)

            val tg4 = Oppfolging1.copy(id = UUID.randomUUID())
            tiltaksgjennomforinger.upsert(tg4)
            Query("update tiltaksgjennomforing set arena_ansvarlig_enhet = '0102' where id = '${tg4.id}'")
                .asUpdate
                .let { database.db.run(it) }

            tiltaksgjennomforinger.getAll(navRegioner = listOf("0100"))
                .should {
                    it.second.map { tg -> tg.id } shouldContainExactlyInAnyOrder listOf(tg3.id, tg4.id)
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
                it.first shouldBe 2
                it.second.map { tg -> tg.id } shouldContainAll listOf(tg1.id, tg2.id)
            }

            tiltaksgjennomforinger.getAll(administratorNavIdent = NavAnsattFixture.ansatt2.navIdent).should {
                it.first shouldBe 1
                it.second.map { tg -> tg.id } shouldContainAll listOf(tg2.id)
            }
        }
    }

    context("pagination") {
        beforeAny {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            (1..105).forEach {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        id = UUID.randomUUID(),
                        navn = "Tiltak - $it",
                        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                        arrangorVirksomhetId = VirksomhetFixtures.underenhet1.id,
                        startDato = LocalDate.of(2022, 1, 1),
                        apentForInnsok = true,
                        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                        stedForGjennomforing = "0139 Oslo",
                    ),
                )
            }
        }

        test("default pagination gets first 50 tiltak") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val (totalCount, items) = tiltaksgjennomforinger.getAll()

            items.size shouldBe DEFAULT_PAGINATION_LIMIT
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 49"

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 59-76") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    4,
                    20,
                ),
            )

            items.size shouldBe 20
            items.first().navn shouldBe "Tiltak - 59"
            items.last().navn shouldBe "Tiltak - 76"

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 95-99") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    3,
                ),
            )

            items.size shouldBe 5
            items.first().navn shouldBe "Tiltak - 95"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-105") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                PaginationParams(
                    nullableLimit = 200,
                ),
            )

            items.size shouldBe 105
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }
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

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val gjennomforing = Oppfolging1.copy(
            id = UUID.randomUUID(),
            faneinnhold = faneinnhold,
        )

        tiltaksgjennomforinger.upsert(gjennomforing)

        tiltaksgjennomforinger.get(gjennomforing.id).should {
            it!!.faneinnhold!!.forHvem!![0] shouldBe faneinnhold.forHvem!![0]
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

            tiltaksgjennomforinger.upsert(Oppfolging1.copy(navEnheter = listOf("2990")))
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

            tiltaksgjennomforinger.upsert(Oppfolging1.copy(navEnheter = listOf("2990", "0400")))
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
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(navn = "erik"))
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
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(apentForInnsok = true, navEnheter = listOf("2990")))
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
})
