package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltakstype1 = TiltakstypeFixtures.Arbeidstrening

    val tiltakstype2 = TiltakstypeFixtures.Oppfolging

    val avtaleFixtures = AvtaleFixtures(database)

    val avtale1 = avtaleFixtures.createAvtaleForTiltakstype(tiltakstypeId = tiltakstype1.id)

    val avtale2 = avtaleFixtures.createAvtaleForTiltakstype(tiltakstypeId = tiltakstype2.id)

    val gjennomforing1 = TiltaksgjennomforingFixtures.Arbeidstrening1

    val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging1

    beforeAny {
        database.db.truncateAll()

        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype1)
        tiltakstyper.upsert(tiltakstype2)

        avtaleFixtures.upsertAvtaler(listOf(avtale1, avtale2))
    }

    context("CRUD") {
        test("CRUD") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()

            tiltaksgjennomforinger.get(gjennomforing1.id).shouldBeRight() shouldBe TiltaksgjennomforingAdminDto(
                id = gjennomforing1.id,
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = tiltakstype1.id,
                    navn = tiltakstype1.navn,
                    arenaKode = tiltakstype1.tiltakskode,
                ),
                navn = gjennomforing1.navn,
                tiltaksnummer = gjennomforing1.tiltaksnummer,
                arrangorOrganisasjonsnummer = gjennomforing1.arrangorOrganisasjonsnummer,
                startDato = gjennomforing1.startDato,
                sluttDato = gjennomforing1.sluttDato,
                arenaAnsvarligEnhet = gjennomforing1.arenaAnsvarligEnhet,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                antallPlasser = null,
                avtaleId = gjennomforing1.avtaleId,
                ansvarlig = null,
                navEnheter = emptyList(),
                sanityId = null,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                stengtFra = null,
                kontaktpersoner = listOf(),
            )

            tiltaksgjennomforinger.delete(gjennomforing1.id)

            tiltaksgjennomforinger.get(gjennomforing1.id) shouldBeRight null
        }

        test("CRUD ArenaTiltaksgjennomforing") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            val gjennomforingId = UUID.randomUUID()
            val gjennomforingFraArena = ArenaTiltaksgjennomforingDbo(
                id = gjennomforingId,
                navn = "Tiltak for dovne giraffer",
                tiltakstypeId = tiltakstype1.id,
                tiltaksnummer = "2023#1",
                arrangorOrganisasjonsnummer = "123456789",
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = "0400",
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                antallPlasser = 10,
                avtaleId = avtale1.id,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            val gjennomforingDto = TiltaksgjennomforingAdminDto(
                id = gjennomforingId,
                navn = "Tiltak for dovne giraffer",
                tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                    id = tiltakstype1.id,
                    navn = tiltakstype1.navn,
                    arenaKode = tiltakstype1.tiltakskode,
                ),
                tiltaksnummer = "2023#1",
                arrangorOrganisasjonsnummer = "123456789",
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 2, 2),
                arenaAnsvarligEnhet = "0400",
                tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                antallPlasser = 10,
                avtaleId = avtale1.id,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                arrangorNavn = null,
                arrangorKontaktperson = null,
                status = Tiltaksgjennomforingsstatus.AVSLUTTET,
                estimertVentetid = null,
                ansvarlig = null,
                navEnheter = emptyList(),
                navRegion = null,
                sanityId = null,
                oppstartsdato = null,
                opphav = ArenaMigrering.Opphav.ARENA,
                stengtFra = null,
                stengtTil = null,
                kontaktpersoner = emptyList(),
                lokasjonArrangor = null,
            )
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(gjennomforingFraArena).shouldBeRight()
            tiltaksgjennomforingRepository.get(gjennomforingFraArena.id).shouldBeRight(gjennomforingDto)
        }

        test("midlertidig_stengt crud") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val gjennomforing = gjennomforing1.copy(
                stengtFra = LocalDate.of(2020, 1, 22),
                stengtTil = LocalDate.of(2020, 4, 22),
            )
            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()

            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.stengtFra shouldBe LocalDate.of(2020, 1, 22)
                it.stengtTil shouldBe LocalDate.of(2020, 4, 22)
            }
        }

        test("Skal hente ut navRegion fra avtale for en gitt gjennomføring") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
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
            avtaleFixtures.upsertAvtaler(listOf(avtale))
            val tiltaksgjennomforing =
                TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtale.id, navEnheter = listOf("2980"))
            tiltaksgjennomforinger.upsert(tiltaksgjennomforing).shouldBeRight()
            tiltaksgjennomforinger.get(tiltaksgjennomforing.id).shouldBeRight().should {
                it?.navRegion shouldBe "NAV Andeby"
                it?.navEnheter?.shouldContain(NavEnhet(enhetsnummer = "2980", "NAV Gåseby"))
            }
        }

        test("navEnheter crud") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
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

            val gjennomforing = gjennomforing1.copy(navEnheter = listOf("1", "2"))

            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "2", navn = "Navn2"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)

            tiltaksgjennomforinger.upsert(gjennomforing.copy(navEnheter = listOf("3", "1"))).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "3", navn = "Navn3"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)
        }

        test("kontaktpersoner på tiltaksgjennomføring CRUD") {
            val domain = MulighetsrommetTestDomain()
            domain.initialize(database.db)

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                kontaktpersoner = listOf(
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = domain.ansatt1.navIdent,
                        navEnheter = listOf(domain.ansatt1.hovedenhet),
                    ),
                    TiltaksgjennomforingKontaktpersonDbo(
                        navIdent = domain.ansatt2.navIdent,
                        navEnheter = listOf(domain.ansatt2.hovedenhet),
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()

            val result = tiltaksgjennomforinger.get(gjennomforing.id).getOrThrow()
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
                        navIdent = domain.ansatt1.navIdent,
                        navEnheter = listOf(domain.ansatt1.hovedenhet),
                    ),
                ),
            )
            tiltaksgjennomforinger.upsert(gjennomforingFjernetKontaktperson).shouldBeRight()

            val oppdatertResult = tiltaksgjennomforinger.get(gjennomforingFjernetKontaktperson.id).getOrThrow()
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
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
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

            val gjennomforing = gjennomforing1.copy(tiltaksnummer = "2023#1")

            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.navEnheter.shouldBeEmpty()
            }
            tiltaksgjennomforinger.updateEnheter("1", listOf("1", "2")).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.navEnheter shouldContainExactlyInAnyOrder listOf(
                    NavEnhet(enhetsnummer = "1", navn = "Navn1"),
                    NavEnhet(enhetsnummer = "2", navn = "Navn2"),
                )
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(2)

            tiltaksgjennomforinger.updateEnheter("1", listOf("2")).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.navEnheter.shouldContainExactlyInAnyOrder(listOf(NavEnhet(enhetsnummer = "2", navn = "Navn2")))
            }
            database.assertThat("tiltaksgjennomforing_nav_enhet").hasNumberOfRows(1)
        }

        test("update sanity_id") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val id = UUID.randomUUID()

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.updateSanityTiltaksgjennomforingId(gjennomforing1.id, id).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing1.id).shouldBeRight().should {
                it!!.sanityId.shouldBe(id.toString())
            }
        }

        test("virksomhet_kontaktperson") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
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
            )
            virksomhetRepository.upsertKontaktperson(thomas)

            val gjennomforing = gjennomforing1.copy(arrangorKontaktpersonId = thomas.id)

            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.arrangorKontaktperson shouldBe thomas
            }

            tiltaksgjennomforinger.upsert(gjennomforing.copy(arrangorKontaktpersonId = null)).shouldBeRight()
            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.arrangorKontaktperson shouldBe null
            }
        }
    }

    context("Filtrer på avtale") {
        test("Kun gjennomforinger tilhørende avtale blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing2.copy(avtaleId = avtale1.id)).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing2.copy(id = UUID.randomUUID(), avtaleId = avtale2.id))
                .shouldBeRight()

            val result = tiltaksgjennomforinger.getAll(
                filter = AdminTiltaksgjennomforingFilter(avtaleId = avtale1.id),
            )
                .shouldBeRight().second
            result shouldHaveSize 1
            result.first().id shouldBe gjennomforing2.id
        }
    }

    context("Filtrer på arrangør") {
        test("Kun gjennomforinger tilhørende arrangør blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing2.copy(arrangorOrganisasjonsnummer = "999999999"))
                .shouldBeRight()

            tiltaksgjennomforinger.getAll(
                filter = AdminTiltaksgjennomforingFilter(arrangorOrgnr = "222222222"),
            ).shouldBeRight().should {
                it.second.size shouldBe 1
                it.second[0].id shouldBe gjennomforing1.id
            }

            tiltaksgjennomforinger.getAll(
                filter = AdminTiltaksgjennomforingFilter(arrangorOrgnr = "999999999"),
            ).shouldBeRight().should {
                it.second.size shouldBe 1
                it.second[0].id shouldBe gjennomforing2.id
            }
        }
    }

    context("Cutoffdato") {
        test("Gamle tiltaksgjennomføringer blir ikke tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(
                gjennomforing1.copy(
                    id = UUID.randomUUID(),
                    sluttDato = LocalDate.of(2022, 12, 31),
                ),
            ).shouldBeRight()

            val result =
                tiltaksgjennomforinger.getAll(filter = AdminTiltaksgjennomforingFilter()).shouldBeRight().second
            result shouldHaveSize 1
            result.first().id shouldBe gjennomforing1.id
        }

        test("Tiltaksgjennomføringer med sluttdato som er null blir tatt med") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = null)).shouldBeRight()

            tiltaksgjennomforinger.getAll(filter = AdminTiltaksgjennomforingFilter())
                .shouldBeRight().second shouldHaveSize 2
        }
    }

    context("TiltaksgjennomforingAnsvarlig") {
        test("Ansvarlige crud") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

            val domain = MulighetsrommetTestDomain()
            domain.initialize(database.db)

            val gjennomforing = gjennomforing1.copy(ansvarlige = listOf(domain.ansatt1.navIdent))
            tiltaksgjennomforinger.upsert(gjennomforing).shouldBeRight()

            tiltaksgjennomforinger.get(gjennomforing.id).shouldBeRight().should {
                it!!.ansvarlig shouldBe domain.ansatt1.navIdent
            }
            database.assertThat("tiltaksgjennomforing_ansvarlig").hasNumberOfRows(1)
        }
    }

    context("Hente tiltaksgjennomføringer som nærmer seg sluttdato") {
        test("Skal hente gjennomføringer som er 14, 7 eller 1 dag til sluttdato") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val gjennomforing14Dager =
                gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 30))
            val gjennomforing7Dager = gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 23))
            val gjennomforing1Dager = gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 17))
            val gjennomforing10Dager =
                gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 26))
            tiltaksgjennomforinger.upsert(gjennomforing14Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing7Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing1Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing10Dager).shouldBeRight()

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
            val gjennomforing14Dager =
                gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 30))
            val gjennomforing7Dager = gjennomforing1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 23),
                stengtFra = LocalDate.of(2023, 6, 16),
                stengtTil = LocalDate.of(2023, 5, 23),
            )
            val gjennomforing1Dager = gjennomforing1.copy(
                id = UUID.randomUUID(),
                sluttDato = LocalDate.of(2023, 5, 17),
                stengtFra = LocalDate.of(2023, 6, 16),
                stengtTil = LocalDate.of(2023, 5, 17),
            )
            val gjennomforing10Dager =
                gjennomforing1.copy(id = UUID.randomUUID(), sluttDato = LocalDate.of(2023, 5, 26))
            tiltaksgjennomforinger.upsert(gjennomforing14Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing7Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing1Dager).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing10Dager).shouldBeRight()

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
        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype1)

        val deltakere = DeltakerRepository(database.db)
        val deltaker = DeltakerDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = gjennomforing1.id,
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
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT,
                    ),
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
        }

        context("when avslutningsstatus is set") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    ),
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Stengt") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.STENGT
            }
        }

        context("when there are no limits to available seats") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = null,
                    ),
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
            }
        }

        context("when there are no available seats") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 0,
                    ),
                ).shouldBeRight()
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.VENTELISTE
            }
        }

        context("when all available seats are occupied by deltakelser with status DELTAR") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 1,
                    ),
                ).shouldBeRight()

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.DELTAR))
            }

            test("should have tilgjengelighet set to Venteliste") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.VENTELISTE
            }
        }

        context("when deltakelser are no longer DELTAR") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            beforeAny {
                tiltaksgjennomforinger.upsert(
                    gjennomforing1.copy(
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = 1,
                    ),
                ).shouldBeRight()

                deltakere.upsert(deltaker.copy(status = Deltakerstatus.AVSLUTTET))
            }

            test("should have tilgjengelighet set to Ledig") {
                tiltaksgjennomforinger.get(gjennomforing1.id)
                    .shouldBeRight()?.tilgjengelighet shouldBe TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
            }
        }
    }

    context("Filtrering på tiltaksgjennomforingstatus") {
        val defaultFilter = AdminTiltaksgjennomforingFilter(
            dagensDato = LocalDate.of(2023, 2, 1),
        )

        val tiltaksgjennomforingAktiv = gjennomforing1
        val tiltaksgjennomforingAvsluttetStatus =
            gjennomforing1.copy(id = UUID.randomUUID(), avslutningsstatus = Avslutningsstatus.AVSLUTTET)
        val tiltaksgjennomforingAvsluttetDato = gjennomforing1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            sluttDato = LocalDate.of(2023, 1, 31),
        )
        val tiltaksgjennomforingAvbrutt = gjennomforing1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVBRUTT,
        )
        val tiltaksgjennomforingAvlyst = gjennomforing1.copy(
            id = UUID.randomUUID(),
            avslutningsstatus = Avslutningsstatus.AVLYST,
        )
        val tiltaksgjennomforingPlanlagt = gjennomforing1.copy(
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
                filter = defaultFilter.copy(status = Tiltaksgjennomforingsstatus.AVBRUTT),
            ).shouldBeRight()

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvbrutt.id
        }

        test("filtrer på avsluttet") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                filter = defaultFilter.copy(status = Tiltaksgjennomforingsstatus.AVSLUTTET),
            ).shouldBeRight()

            result.second shouldHaveSize 2
            result.second.map { it.id }
                .shouldContainAll(tiltaksgjennomforingAvsluttetDato.id, tiltaksgjennomforingAvsluttetStatus.id)
        }

        test("filtrer på gjennomføres") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                filter = defaultFilter.copy(status = Tiltaksgjennomforingsstatus.GJENNOMFORES),
            ).shouldBeRight()

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAktiv.id
        }

        test("filtrer på avlyst") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                filter = defaultFilter.copy(status = Tiltaksgjennomforingsstatus.AVLYST),
            ).shouldBeRight()

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingAvlyst.id
        }

        test("filtrer på åpent for innsøk") {
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)

            val result = tiltaksgjennomforingRepository.getAll(
                filter = defaultFilter.copy(status = Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK),
            ).shouldBeRight()

            result.second shouldHaveSize 1
            result.second[0].id shouldBe tiltaksgjennomforingPlanlagt.id
        }
    }

    context("pagination") {
        beforeAny {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            (1..105).forEach {
                tiltaksgjennomforinger.upsert(
                    TiltaksgjennomforingDbo(
                        id = UUID.randomUUID(),
                        navn = "Tiltak - $it",
                        tiltakstypeId = tiltakstype1.id,
                        tiltaksnummer = "$it",
                        arrangorOrganisasjonsnummer = "123456789",
                        arenaAnsvarligEnhet = "2990",
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                        startDato = LocalDate.of(2022, 1, 1),
                        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
                        antallPlasser = null,
                        ansvarlige = emptyList(),
                        navEnheter = emptyList(),
                        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                        lokasjonArrangor = "0139 Oslo",
                    ),
                )
            }
        }

        test("default pagination gets first 50 tiltak") {
            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            val (totalCount, items) = tiltaksgjennomforinger.getAll(
                filter = AdminTiltaksgjennomforingFilter(),
            )
                .shouldBeRight()

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
                AdminTiltaksgjennomforingFilter(),
            ).shouldBeRight()

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
                AdminTiltaksgjennomforingFilter(),
            ).shouldBeRight()

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
                AdminTiltaksgjennomforingFilter(),
            ).shouldBeRight()

            items.size shouldBe 105
            items.first().navn shouldBe "Tiltak - 1"
            items.last().navn shouldBe "Tiltak - 99"

            totalCount shouldBe 105
        }
    }

    context("Lokasjon til veilederflate fra gjennomføringer") {
        test("Skal hente ut distinkt liste med lokasjoner basert på brukers enhet eller fylkesenhet") {
            val avtaleFixtures = AvtaleFixtures(database)
            avtaleFixtures.runBeforeTests()
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

            val avtale = avtaleFixtures.createAvtaleForTiltakstype(navRegion = "0400")
            val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                lokasjonArrangor = "0139 Oslo",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = avtaleFixtures.tiltakstypeId,
            )
            val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                lokasjonArrangor = "8756 Kristiansand",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = avtaleFixtures.tiltakstypeId,
            )
            val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                lokasjonArrangor = "0139 Oslo",
                id = UUID.randomUUID(),
                navEnheter = listOf("0482"),
                avtaleId = avtale.id,
                tiltakstypeId = avtaleFixtures.tiltakstypeId,
            )
            avtaleFixtures.upsertAvtaler(listOf(avtale))
            tiltaksgjennomforinger.upsert(gjennomforing1).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing2).shouldBeRight()
            tiltaksgjennomforinger.upsert(gjennomforing3).shouldBeRight()

            val result = tiltaksgjennomforinger.getLokasjonerForEnhet("0482", "0400")
            result.size shouldBe 2
            result shouldContainExactly listOf("0139 Oslo", "8756 Kristiansand")
        }
    }
})
