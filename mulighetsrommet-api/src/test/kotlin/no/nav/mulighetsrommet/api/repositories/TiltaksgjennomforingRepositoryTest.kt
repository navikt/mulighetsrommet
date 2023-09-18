package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures.avtale1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Arbeidstrening1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.ArenaOppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging2
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val domain = MulighetsrommetTestDomain()

    beforeAny {
        database.db.truncateAll()

        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(TiltakstypeFixtures.Arbeidstrening)
        tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging)

        domain.initialize(database.db)
    }

    context("CRUD") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        test("CRUD adminflate-tiltaksgjennomføringer") {
            tiltaksgjennomforinger.upsert(Oppfolging1)

            tiltaksgjennomforinger.get(Oppfolging1.id) shouldBe TiltaksgjennomforingAdminDto(
                id = Oppfolging1.id,
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = TiltakstypeFixtures.Oppfolging.id,
                    navn = TiltakstypeFixtures.Oppfolging.navn,
                    arenaKode = TiltakstypeFixtures.Oppfolging.tiltakskode,
                ),
                navn = Oppfolging1.navn,
                tiltaksnummer = Oppfolging1.tiltaksnummer,
                arrangor = TiltaksgjennomforingAdminDto.Arrangor(
                    organisasjonsnummer = Oppfolging1.arrangorOrganisasjonsnummer,
                    slettet = true,
                    navn = null,
                    kontaktperson = null,
                ),
                startDato = Oppfolging1.startDato,
                sluttDato = Oppfolging1.sluttDato,
                arenaAnsvarligEnhet = Oppfolging1.arenaAnsvarligEnhet,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                antallPlasser = 12,
                avtaleId = Oppfolging1.avtaleId,
                administrator = null,
                navEnheter = emptyList(),
                sanityId = null,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                stengtFra = null,
                kontaktpersoner = listOf(),
                lokasjonArrangor = "Oslo",
                stengtTil = null,
                navRegion = NavEnhet(navn = "IT", enhetsnummer = "2990"),
                estimertVentetid = null,
            )

            tiltaksgjennomforinger.delete(Oppfolging1.id)

            tiltaksgjennomforinger.get(Oppfolging1.id) shouldBe null
        }

        test("CRUD ArenaTiltaksgjennomforing") {
            val gjennomforingId = UUID.randomUUID()
            val gjennomforingFraArena = ArenaTiltaksgjennomforingDbo(
                id = gjennomforingId,
                navn = "Tiltak for dovne giraffer",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "2023#1",
                arrangorOrganisasjonsnummer = "123456789",
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = "0400",
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                antallPlasser = 10,
                avtaleId = null,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            val gjennomforingDto = TiltaksgjennomforingAdminDto(
                id = gjennomforingId,
                navn = "Tiltak for dovne giraffer",
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = TiltakstypeFixtures.Oppfolging.id,
                    navn = TiltakstypeFixtures.Oppfolging.navn,
                    arenaKode = TiltakstypeFixtures.Oppfolging.tiltakskode,
                ),
                tiltaksnummer = "2023#1",
                arrangor = TiltaksgjennomforingAdminDto.Arrangor(
                    organisasjonsnummer = "123456789",
                    slettet = true,
                    navn = null,
                    kontaktperson = null,
                ),
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = "0400",
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                antallPlasser = 10,
                avtaleId = null,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                estimertVentetid = null,
                administrator = null,
                navEnheter = emptyList(),
                navRegion = null,
                sanityId = null,
                opphav = ArenaMigrering.Opphav.ARENA,
                stengtFra = null,
                stengtTil = null,
                kontaktpersoner = emptyList(),
                lokasjonArrangor = null,
            )

            tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(gjennomforingFraArena)

            tiltaksgjennomforinger.get(gjennomforingFraArena.id).shouldBe(gjennomforingDto)
        }

        test("midlertidig_stengt crud") {
            val gjennomforing = Oppfolging1.copy(
                id = UUID.randomUUID(),
                stengtFra = LocalDate.of(2020, 1, 22),
                stengtTil = LocalDate.of(2020, 4, 22),
            )

            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.stengtFra shouldBe LocalDate.of(2020, 1, 22)
                it.stengtTil shouldBe LocalDate.of(2020, 4, 22)
            }
        }

        test("Skal hente ut navRegion fra avtale for en gitt gjennomføring") {
            val navEnheter = NavEnhetRepository(database.db)
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "NAV Andeby",
                    enhetsnummer = "2990",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            )
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "NAV Gåseby",
                    enhetsnummer = "2980",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            )
            val avtale = avtale1.copy(navRegion = "2990")
            avtaler.upsert(avtale)

            val tiltaksgjennomforing = Oppfolging1.copy(
                avtaleId = avtale.id,
                navEnheter = listOf("2980"),
            )
            tiltaksgjennomforinger.upsert(tiltaksgjennomforing)

            tiltaksgjennomforinger.get(tiltaksgjennomforing.id).should {
                it?.navRegion?.navn shouldBe "NAV Andeby"
                it?.navEnheter?.shouldContain(NavEnhet(enhetsnummer = "2980", "NAV Gåseby"))
            }
        }

        test("navEnheter crud") {
            val enhetRepository = NavEnhetRepository(database.db)
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn1",
                    enhetsnummer = "1",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn2",
                    enhetsnummer = "2",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn3",
                    enhetsnummer = "3",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()

            val gjennomforing = Oppfolging1.copy(
                id = UUID.randomUUID(),
                navEnheter = listOf("1", "2"),
            )

            tiltaksgjennomforinger.upsert(gjennomforing)
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "2", navn = "Navn2"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)

            tiltaksgjennomforinger.upsert(gjennomforing.copy(navEnheter = listOf("3", "1")))
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "3", navn = "Navn3"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)
        }

        test("kontaktpersoner på tiltaksgjennomføring CRUD") {
            val gjennomforing = Oppfolging1.copy(
                kontaktpersoner = listOf(
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt1.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                    ),
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt2.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt2.hovedenhet),
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforing)

            val result = tiltaksgjennomforinger.get(gjennomforing.id)
            result?.kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                TiltaksgjennomforingKontaktperson(
                    navIdent = "DD1",
                    navn = "Donald Duck",
                    mobilnummer = "12345678",
                    epost = "donald.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                ),
                TiltaksgjennomforingKontaktperson(
                    navIdent = "DD2",
                    navn = "Dolly Duck",
                    mobilnummer = "48243214",
                    epost = "dolly.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                ),
            )
            val gjennomforingFjernetKontaktperson = gjennomforing.copy(
                kontaktpersoner = listOf(
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt1.navIdent,
                        navEnheter = listOf(NavAnsattFixture.ansatt1.hovedenhet),
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforingFjernetKontaktperson)

            val oppdatertResult = tiltaksgjennomforinger.get(gjennomforingFjernetKontaktperson.id)
            oppdatertResult?.kontaktpersoner shouldBe listOf(
                TiltaksgjennomforingKontaktperson(
                    navIdent = "DD1",
                    navn = "Donald Duck",
                    mobilnummer = "12345678",
                    epost = "donald.duck@nav.no",
                    navEnheter = listOf("2990"),
                    hovedenhet = "2990",
                ),
            )
        }

        test("Oppdater navEnheter fra Sanity-tiltaksgjennomføringer til database") {
            val enhetRepository = NavEnhetRepository(database.db)
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn1",
                    enhetsnummer = "1",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn2",
                    enhetsnummer = "2",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()

            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.get(Oppfolging1.id).should {
                it!!.navEnheter.shouldBeEmpty()
            }
            tiltaksgjennomforinger.updateEnheter("1", listOf("1", "2"))
            tiltaksgjennomforinger.get(Oppfolging1.id).should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "2", navn = "Navn2"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)

            tiltaksgjennomforinger.updateEnheter("1", listOf("2"))
            tiltaksgjennomforinger.get(Oppfolging1.id).should {
                it!!.navEnheter.shouldContainExactlyInAnyOrder(listOf(NavEnhet(enhetsnummer = "2", navn = "Navn2")))
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(1)
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

            virksomhetRepository.upsert(
                VirksomhetDto(
                    organisasjonsnummer = "999888777",
                    navn = "Rema 2000",
                ),
            )
            val thomas = VirksomhetKontaktperson(
                navn = "Thomas",
                telefon = "22222222",
                epost = "thomas@thetrain.co.uk",
                id = UUID.randomUUID(),
                organisasjonsnummer = "999888777",
                beskrivelse = "beskrivelse",
            )
            virksomhetRepository.upsertKontaktperson(thomas)

            val gjennomforing = Oppfolging1.copy(arrangorKontaktpersonId = thomas.id)

            tiltaksgjennomforinger.upsert(gjennomforing)
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.arrangor.kontaktperson shouldBe thomas
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(arrangorKontaktpersonId = null))
            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.arrangor.kontaktperson shouldBe null
            }
        }
    }

    context("skal migreres") {
        test("skal migreres henter kun der tiltakstypen skal_migreres er true") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            Query("update tiltakstype set skal_migreres = true where id = '${Oppfolging1.tiltakstypeId}'")
                .asUpdate.let { database.db.run(it) }

            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.upsert(Arbeidstrening1)

            tiltaksgjennomforinger.getAll(skalMigreres = true).should {
                it.first shouldBe 1
                it.second[0].id shouldBe Oppfolging1.id
            }
        }
    }

    context("Filtrer på avtale") {
        test("Kun gjennomforinger tilhørende avtale blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(
                Oppfolging1.copy(
                    avtaleId = avtale1.id,
                ),
            )

            val result = tiltaksgjennomforinger.getAll(
                avtaleId = avtale1.id,
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
                Oppfolging1.copy(
                    arrangorOrganisasjonsnummer = "111111111",
                ),
            )
            tiltaksgjennomforinger.upsert(
                Oppfolging2.copy(
                    arrangorOrganisasjonsnummer = "999999999",
                ),
            )

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = "111111111",
            ).should {
                it.second.size shouldBe 1
                it.second[0].id shouldBe Oppfolging1.id
            }

            tiltaksgjennomforinger.getAll(
                arrangorOrgnr = "999999999",
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

    context("Tiltaksgjennomforingadministrator") {
        test("Administratorer crud") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val gjennomforing =
                Oppfolging1.copy(administratorer = listOf(NavAnsattFixture.ansatt1.navIdent))
            tiltaksgjennomforinger.upsert(gjennomforing)

            tiltaksgjennomforinger.get(gjennomforing.id).should {
                it!!.administrator shouldBe TiltaksgjennomforingAdminDto.Administrator(
                    navIdent = NavAnsattFixture.ansatt1.navIdent,
                    navn = "Donald Duck",
                )
            }
            database.assertThat("tiltaksgjennomforing_administrator").hasNumberOfRows(1)
        }
    }

    context("Hente tiltaksgjennomføringer som nærmer seg sluttdato") {
        test("Skal hente gjennomføringer som er 14, 7 eller 1 dag til sluttdato") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val oppfolging14Dager =
                Oppfolging1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.of(2023, 5, 30),
                )
            val gjennomforing7Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 23),
            )
            val oppfolging1Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 17),
            )
            val oppfolging10Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 26),
            )
            tiltaksgjennomforinger.upsert(oppfolging14Dager)
            tiltaksgjennomforinger.upsert(gjennomforing7Dager)
            tiltaksgjennomforinger.upsert(oppfolging1Dager)
            tiltaksgjennomforinger.upsert(oppfolging10Dager)

            val result = tiltaksgjennomforinger.getAllGjennomforingerSomNarmerSegSluttdato(
                currentDate = LocalDate.of(
                    2023,
                    5,
                    16,
                ),
            )
            result.size shouldBe 3
        }
    }

    context("Hente tiltaksgjennomføringer som er midlertidig stengt og som nærmer seg sluttdato for den stengte perioden") {
        test("Skal hente gjennomføringer som er 7 eller 1 dag til stengt-til-datoen") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val oppfolging14Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 30),
            )
            val gjennomforing7Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 23),
                stengtFra = LocalDate.of(2023, 6, 16),
                stengtTil = LocalDate.of(2023, 5, 23),
            )
            val oppfolging1Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 17),
                stengtFra = LocalDate.of(2023, 6, 16),
                stengtTil = LocalDate.of(2023, 5, 17),
            )
            val oppfolging10Dager = Oppfolging1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 26),
            )
            tiltaksgjennomforinger.upsert(oppfolging14Dager)
            tiltaksgjennomforinger.upsert(gjennomforing7Dager)
            tiltaksgjennomforinger.upsert(oppfolging1Dager)
            tiltaksgjennomforinger.upsert(oppfolging10Dager)

            val result = tiltaksgjennomforinger.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(
                currentDate = LocalDate.of(
                    2023,
                    5,
                    16,
                ),
            )
            result.size shouldBe 2
        }
    }

    context("TiltaksgjennomforingTilgjengelighetsstatus") {
        val deltakere = DeltakerRepository(database.db)
        val deltaker = DeltakerDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = Oppfolging1.id,
            status = Deltakerstatus.DELTAR,
            opphav = Deltakeropphav.AMT,
            startDato = null,
            sluttDato = null,
            registrertDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        )

        context("when tilgjengelighet is set to Stengt") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                    ),
                )
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(Oppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
        }

        context("when avslutningsstatus is set") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    ),
                )
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(Oppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
        }

        context("when there are no limits to available seats") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsertArenaTiltaksgjennomforing(
                    ArenaOppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = null,
                    ),
                )
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(ArenaOppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
            }
        }

        context("when there are no available seats") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 0,
                    ),
                )
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(Oppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.VENTELISTE
            }
        }

        context("when all available seats are occupied by deltakelser with status DELTAR") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 1,
                    ),
                )

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.DELTAR))
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(Oppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.VENTELISTE
            }
        }

        context("when deltakelser are no longer DELTAR") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    Oppfolging1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 1,
                    ),
                )

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.AVSLUTTET))
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(Oppfolging1.id)
                    ?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
            }
        }
    }

    context("Filtrering på tiltaksgjennomforingstatus") {
        val dagensDato = LocalDate.of(2023, 2, 1)

        val tiltaksgjennomforingAktiv = Arbeidstrening1
        val tiltaksgjennomforingAvsluttetStatus = Oppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        )
        val tiltaksgjennomforingAvsluttetDato = Oppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            sluttDato = LocalDate.of(2023, 1, 31),
        )
        val tiltaksgjennomforingAvbrutt = Oppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVBRUTT,
        )
        val tiltaksgjennomforingAvlyst = Oppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVLYST,
        )
        val tiltaksgjennomforingPlanlagt = Oppfolging1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            startDato = LocalDate.of(2023, 2, 2),
        )

        beforeAny {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAktiv)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAvsluttetStatus)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAvsluttetDato)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAvbrutt)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingPlanlagt)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingAvlyst)
        }

        test("filtrer på avbrutt") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                dagensDato = dagensDato,
                status = Tiltaksgjennomforingsstatus.AVBRUTT,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvbrutt.id
        }

        test("filtrer på avsluttet") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                dagensDato = dagensDato,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
            )

            result.second shouldHaveSize 2
            result.second.map { it.id }
                .shouldContainAll(tiltaksgjennomforingAvsluttetDato.id, tiltaksgjennomforingAvsluttetStatus.id)
        }

        test("filtrer på gjennomføres") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                status = Tiltaksgjennomforingsstatus.GJENNOMFORES,
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAktiv.id
        }

        test("filtrer på avlyst") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                status = Tiltaksgjennomforingsstatus.AVLYST,
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvlyst.id
        }

        test("filtrer på åpent for innsøk") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                status = Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK,
                dagensDato = dagensDato,
            )

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingPlanlagt.id
        }
    }

    context("filtrering av tiltaksgjennomføringer") {
        val avtaler = AvtaleRepository(database.db)
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

            val gj1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1"))
            val gj2 = Oppfolging1.copy(id = UUID.randomUUID(), arenaAnsvarligEnhet = "1")
            val gj3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("2"))
            val gj4 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1", "2"))

            tiltaksgjennomforinger.upsert(gj1)
            tiltaksgjennomforinger.upsert(gj2)
            tiltaksgjennomforinger.upsert(gj3)
            tiltaksgjennomforinger.upsert(gj4)

            tiltaksgjennomforinger.getAll(navEnhet = "1").should {
                it.first shouldBe 3
                it.second.map { it.id } shouldContainAll listOf(gj1.id, gj2.id, gj4.id)
            }
        }

        test("filtrer på nav_region") {
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "NavRegion",
                    enhetsnummer = "nav_region",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
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
                    overordnetEnhet = "nav_region",
                ),
            ).shouldBeRight()

            val avtale = avtale1.copy(id = UUID.randomUUID(), navRegion = "nav_region")
            avtaler.upsert(avtale)
            val gj1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1"))
            val gj2 = Oppfolging1.copy(id = UUID.randomUUID(), arenaAnsvarligEnhet = "1")
            val gj3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("2"))
            val gj4 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1", "2"))
            val gj5 = Oppfolging1.copy(
                id = UUID.randomUUID(),
                navEnheter = emptyList(),
                arenaAnsvarligEnhet = null,
                avtaleId = avtale.id,
            )

            tiltaksgjennomforinger.upsert(gj1)
            tiltaksgjennomforinger.upsert(gj2)
            tiltaksgjennomforinger.upsert(gj3)
            tiltaksgjennomforinger.upsert(gj4)
            tiltaksgjennomforinger.upsert(gj5)

            tiltaksgjennomforinger.getAll(navRegion = "nav_region")
                .should {
                    it.first shouldBe 3
                    it.second.map { it.id } shouldContainAll listOf(gj4.id, gj3.id, gj5.id)
                }
        }

        test("filtrer på nav_region når ingen avtale er koblet til gjennomføringen") {
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "NAV Nordland",
                    enhetsnummer = "1800",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Nordland 1",
                    enhetsnummer = "1898",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            navEnheter.upsert(
                NavEnhetDbo(
                    navn = "Nordland 2",
                    enhetsnummer = "1854",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "1800",
                ),
            ).shouldBeRight()

            val gj1 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1898"))
            val gj2 = Oppfolging1.copy(id = UUID.randomUUID(), arenaAnsvarligEnhet = "1800")
            val gj3 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1898"))
            val gj4 = Oppfolging1.copy(id = UUID.randomUUID(), navEnheter = listOf("1800"))

            tiltaksgjennomforinger.upsert(gj1)
            tiltaksgjennomforinger.upsert(gj2)
            tiltaksgjennomforinger.upsert(gj3)
            tiltaksgjennomforinger.upsert(gj4)

            tiltaksgjennomforinger.getAll(navRegion = "1800")
                .should {
                    it.first shouldBe 1
                    it.second.map { it.id } shouldContainAll listOf(gj2.id)
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
                        tiltaksnummer = "$it",
                        arrangorOrganisasjonsnummer = "123456789",
                        arenaAnsvarligEnhet = "2990",
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                        startDato = LocalDate.of(2022, 1, 1),
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                        lokasjonArrangor = "0139 Oslo",
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

    context("Lokasjon til veilederflate fra gjennomføringer") {
        val avtaler = AvtaleRepository(database.db)
        test("Skal hente ut distinkt liste med lokasjoner basert på brukers enhet eller fylkesenhet") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val enhetRepository = NavEnhetRepository(database.db)
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn1",
                    enhetsnummer = "0400",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Navn2",
                    enhetsnummer = "0482",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = "0400",
                ),
            ).shouldBeRight()

            val avtale = avtale1.copy(navRegion = "0400")
            val gjennomforing01 = Oppfolging1.copy(
                lokasjonArrangor = "0139 Oslo",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            )
            val gjennomforing02 = Oppfolging1.copy(
                lokasjonArrangor = "8756 Kristiansand",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            )
            val gjennomforing03 = Oppfolging1.copy(
                lokasjonArrangor = "0139 Oslo",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            )
            avtaler.upsert(avtale)
            tiltaksgjennomforinger.upsert(gjennomforing01)
            tiltaksgjennomforinger.upsert(gjennomforing02)
            tiltaksgjennomforinger.upsert(gjennomforing03)

            val result = tiltaksgjennomforinger.getLokasjonerForEnhet("0482", "0400")
            result.size shouldBe 2
            result shouldContainExactly listOf("0139 Oslo", "8756 Kristiansand")
        }
    }
})
