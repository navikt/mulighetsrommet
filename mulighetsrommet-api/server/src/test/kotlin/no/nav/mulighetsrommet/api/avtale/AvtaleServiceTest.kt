package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Virksomhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.domain.tiltak.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.AFT,
        ),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft),
    )

    beforeEach {
        domain.initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
        syncArrangor: SyncArrangorUseCase = SyncArrangorUseCase(database.admin, mockk()),
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ) = AvtaleService(
        config = AvtaleService.Config(mapOf()),
        database.api,
        syncArrangor,
        TiltakstypeService(TiltakstypeService.Config(features), database.admin),
        gjennomforingPublisher,
    )

    context("opprett avtale") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("systemet bestemmer prismodell når forhåndsgodkjent avtale opprettes") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT),
                prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft),
            ).initialize(database.api)

            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                avtaletype = Avtaletype.FORHANDSGODKJENT,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
                prismodell = listOf(),
            )

            avtaleService.create(request, bertilNavIdent).shouldBeRight().prismodeller.shouldHaveSize(1).should {
                it.first().id shouldBe PrismodellFixtures.ForhandsgodkjentAft.id
            }
        }

        test("oppretter avtale med prismodell") {
            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                prismodell = listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging),
            )

            avtaleService.create(request, bertilNavIdent).shouldBeRight().prismodeller.shouldHaveSize(1).should {
                it.first().id shouldBe PrismodellFixtures.AvtaltPrisPerTimeOppfolging.id
            }
        }

        test("oppretter avtale med arrangør hentet fra brreg") {
            val orgnrHovedenhet = Organisasjonsnummer("999999999")
            val orgnrUnderenhet = Organisasjonsnummer("888888888")
            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(orgnrHovedenhet) } returns VirksomhetOppslag.Funnet(
                    Virksomhet.Hovedenhet(
                        organisasjonsnummer = orgnrHovedenhet,
                        organisasjonsform = "AS",
                        navn = "Ny arrangør hovedenhet",
                    ),
                ).right()
                coEvery { hentVirksomhet(orgnrUnderenhet) } returns VirksomhetOppslag.Funnet(
                    Virksomhet.Underenhet(
                        organisasjonsnummer = orgnrUnderenhet,
                        organisasjonsform = "AS",
                        navn = "Ny arrangør underenhet",
                        overordnetEnhet = orgnrHovedenhet,
                    ),
                ).right()
            }

            val syncArrangor = SyncArrangorUseCase(database.admin, enhetsregister)

            val avtaleService = createAvtaleService(syncArrangor = syncArrangor)

            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                arrangor = DetaljerRequest.Arrangor(
                    hovedenhet = orgnrHovedenhet,
                    underenheter = listOf(orgnrUnderenhet),
                    kontaktpersoner = listOf(),
                ),
            )

            avtaleService.create(request, bertilNavIdent).shouldBeRight().arrangor.shouldNotBeNull().should {
                it.organisasjonsnummer shouldBe orgnrHovedenhet
                it.navn shouldBe "Ny arrangør hovedenhet"
                it.underenheter.shouldHaveSize(1).first().should { underenhet ->
                    underenhet.organisasjonsnummer shouldBe orgnrUnderenhet
                    underenhet.navn shouldBe "Ny arrangør underenhet"
                }
            }
        }

        test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
            val request = AvtaleFixtures.createAvtaleRequest(Tiltakskode.OPPFOLGING)

            avtaleService.create(request, bertilNavIdent).shouldBeRight()

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = request.id),
                    any(),
                    any(),
                )
            }
        }

        test("får ikke opprette avtale dersom tiltakstype er utfaset") {
            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                prismodell = listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging),
            )

            val avtaleService = createAvtaleService(
                features = mapOf(
                    Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.UTFASET),
                ),
            )

            avtaleService.create(request, bertilNavIdent).shouldBeLeft() shouldBe listOf(
                FieldError.of("Avtaler kan ikke opprettes for denne tiltakstypen fordi den er utfaset"),
            )
        }

        test("får ikke opprette avtaler for tiltakstyper som er ment for enkeltplasser") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Arbeidstrening),
            ).initialize(database.api)

            val arbeidstrening = AvtaleFixtures.createAvtaleRequest(Tiltakskode.ARBEIDSTRENING)
            avtaleService.create(arbeidstrening, bertilNavIdent).shouldBeLeft() shouldBe listOf(
                FieldError.of("Avtaler kan ikke opprettes for denne tiltakstypen"),
            )

            val hoyereUtdanning = AvtaleFixtures.createAvtaleRequest(Tiltakskode.HOYERE_UTDANNING)
            avtaleService.create(hoyereUtdanning, bertilNavIdent).shouldBeLeft() shouldBe listOf(
                FieldError.of("Avtaler kan ikke opprettes for denne tiltakstypen"),
            )
        }

        test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(Organisasjonsnummer("223442332")) } returns EnhetsregisterError.IkkeFunnet.left()
            }

            val syncArrangor = SyncArrangorUseCase(database.admin, enhetsregister)

            val avtaleService = createAvtaleService(syncArrangor = syncArrangor)

            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                arrangor = DetaljerRequest.Arrangor(
                    hovedenhet = Organisasjonsnummer("223442332"),
                    underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                    kontaktpersoner = emptyList(),
                ),
            )
            avtaleService.create(request, bertilNavIdent).shouldBeLeft(
                listOf(
                    FieldError(
                        "/detaljer/arrangor/hovedenhet",
                        "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                    ),
                ),
            )
        }
    }

    context("rediger detaljer") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("oppdaterer detaljer, returnerer oppdatert avtale og skedulerer publisering av gjennomføringer") {
            val request = AvtaleFixtures.createAvtaleRequest(Tiltakskode.OPPFOLGING)
            avtaleService.create(request, bertilNavIdent).shouldBeRight()

            avtaleService.upsertDetaljer(
                request.id,
                request.detaljer.copy(navn = "Nytt avtalenavn"),
                bertilNavIdent,
            ).shouldBeRight().navn shouldBe "Nytt avtalenavn"

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = request.id),
                    any(),
                    any(),
                )
            }
        }

        test("gjør ingen endring og skedulerer ikke ny publisering når detaljene er uendret") {
            val request = AvtaleFixtures.createAvtaleRequest(Tiltakskode.OPPFOLGING)
            avtaleService.create(request, bertilNavIdent).shouldBeRight()

            val noopPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
            val noopService = createAvtaleService(noopPublisher)

            noopService.upsertDetaljer(
                request.id,
                request.detaljer,
                bertilNavIdent,
            ).shouldBeRight().navn shouldBe request.detaljer.navn

            verify(exactly = 0) {
                noopPublisher.schedule(any(), any(), any())
            }
        }

        test("kan ikke endre tiltakskode etter at avtalen er opprettet") {
            val request = AvtaleFixtures.createAvtaleRequest(Tiltakskode.OPPFOLGING)
            avtaleService.create(request, bertilNavIdent).shouldBeRight()

            val aftDetaljer = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                avtaletype = Avtaletype.FORHANDSGODKJENT,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
            ).detaljer

            avtaleService.upsertDetaljer(request.id, aftDetaljer, bertilNavIdent).shouldBeLeft(
                listOf(
                    FieldError(
                        "/detaljer/tiltakskode",
                        "Tiltakstype kan ikke endres etter at avtalen er opprettet",
                    ),
                ),
            )
        }
    }

    context("rediger personvern") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        val avtale = AvtaleFixtures.oppfolging

        test("krever beskrivelse når 'annet' er valgt") {
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertPersonvern(
                avtale.id,
                PersonvernRequest(
                    personopplysninger = emptyList(),
                    annetChecked = true,
                    annetBeskrivelse = " ",
                    personvernBekreftet = false,
                ),
                bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")),
            )
        }

        test("beskrivelse kan maks være 300 tegn") {
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertPersonvern(
                avtale.id,
                PersonvernRequest(
                    personopplysninger = emptyList(),
                    annetChecked = true,
                    annetBeskrivelse = "a".repeat(301),
                    personvernBekreftet = false,
                ),
                bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse kan maks være 300 tegn")),
            )
        }

        test("oppdaterer personvernopplysninger, returnerer oppdatert avtale og skedulerer publisering") {
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertPersonvern(
                avtale.id,
                PersonvernRequest(
                    personopplysninger = listOf(Personopplysning.Type.NAVN, Personopplysning.Type.FODSELSDATO),
                    annetChecked = true,
                    annetBeskrivelse = "Annen personopplysning",
                    personvernBekreftet = true,
                ),
                bertilNavIdent,
            ).shouldBeRight().should {
                it.personvernBekreftet shouldBe true
                it.personopplysninger.map { p -> p.type }.shouldContainExactlyInAnyOrder(
                    Personopplysning.Type.NAVN,
                    Personopplysning.Type.FODSELSDATO,
                    Personopplysning.Type.ANNET,
                )
                it.personopplysninger.find { p -> p.type == Personopplysning.Type.ANNET }
                    .shouldNotBeNull().beskrivelse shouldBe "Annen personopplysning"
            }

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = avtale.id),
                    any(),
                    any(),
                )
            }
        }
    }

    context("rediger veilederinformasjon") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        val avtale = AvtaleFixtures.oppfolging

        test("oppdaterer veilederinformasjon, returnerer oppdatert avtale og skedulerer publisering") {
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertVeilederinfo(
                avtale.id,
                VeilederinfoRequest(
                    navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
                    beskrivelse = "Ny beskrivelse for veiledere",
                    faneinnhold = null,
                ),
                bertilNavIdent,
            ).shouldBeRight().should {
                it.beskrivelse shouldBe "Ny beskrivelse for veiledere"
                it.navEnheter shouldContainExactlyInAnyOrder setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502"))
            }

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = avtale.id),
                    any(),
                    any(),
                )
            }
        }

        test("krever minst én Nav-region og én Nav-enhet blant valgte enheter") {
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertVeilederinfo(
                avtale.id,
                VeilederinfoRequest(
                    navEnheter = emptyList(),
                    beskrivelse = null,
                    faneinnhold = null,
                ),
                bertilNavIdent,
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
            )
        }
    }

    context("rediger prismodeller") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("endre prismodeller") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val prismodell1Request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                satser = emptyList(),
                prisbetingelser = null,
                tilsagnPerDeltaker = false,
            )
            val prismodell2Request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                valuta = Valuta.NOK,
                satser = emptyList(),
                prisbetingelser = null,
                tilsagnPerDeltaker = false,
            )

            avtaleService
                .upsertPrismodell(avtale.id, listOf(prismodell1Request, prismodell2Request), bertilNavIdent)
                .shouldBeRight()
                .prismodeller.map { it.id }
                .shouldContainExactlyInAnyOrder(prismodell1Request.id, prismodell2Request.id)

            avtaleService
                .upsertPrismodell(avtale.id, listOf(prismodell2Request), bertilNavIdent)
                .shouldBeRight()
                .prismodeller.map { it.id }
                .shouldContainExactlyInAnyOrder(prismodell2Request.id)

            avtaleService
                .upsertPrismodell(avtale.id, listOf(), bertilNavIdent)
                .shouldBeLeft()
                .shouldContainExactlyInAnyOrder(FieldError("/prismodeller", "Minst én prismodell er påkrevd"))
        }

        test("tillater ikke oppretting av forhåndsgodkjente prismodeller") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT),
            ).initialize(database.api)

            val request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                valuta = Valuta.NOK,
                prisbetingelser = null,
                satser = listOf(AvtaltSatsRequest(LocalDate.of(2025, 1, 1), 100)),
                tilsagnPerDeltaker = false,
            )

            avtaleService.upsertPrismodell(AvtaleFixtures.oppfolging.id, listOf(request), bertilNavIdent)
                .shouldBeLeft()
                .shouldContain(
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke opprettes med typen Fast sats per benyttet tiltaksplass per måned",
                    ),
                )

            avtaleService.upsertPrismodell(AvtaleFixtures.AFT.id, listOf(request), bertilNavIdent)
                .shouldBeLeft()
                .shouldContain(
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke opprettes for forhåndsgodkjente avtaler",
                    ),
                )
        }

        test("skedulerer publisering av gjennomføringer tilhørende avtalen ved endring av prismodell") {
            val prismodell = PrismodellFixtures.AvtaltPrisPerTimeOppfolging
            val avtale = AvtaleFixtures.oppfolging
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.api)

            val request = listOf(
                PrismodellRequest(
                    id = prismodell.id,
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    valuta = Valuta.NOK,
                    satser = emptyList(),
                    prisbetingelser = null,
                    tilsagnPerDeltaker = false,
                ),
            )

            avtaleService.upsertPrismodell(avtale.id, request, bertilNavIdent).shouldBeRight()

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = avtale.id),
                    any(),
                    any(),
                )
            }
        }
    }

    context("avbryt avtale") {
        val avtaleService = createAvtaleService()

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avbruttAvtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
            )
            val avsluttetAvtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                detaljerDbo = AvtaleFixtures.detaljerDbo().copy(status = AvtaleStatusType.AVSLUTTET),
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avbruttAvtale, avsluttetAvtale),
            ) {
                queries.avtale.setStatus(
                    avbruttAvtale.id,
                    AvtaleStatusType.AVBRUTT,
                    tidspunkt = LocalDateTime.now(),
                    aarsaker = listOf(AvbrytAvtaleAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.api)

            avtaleService.avbrytAvtale(
                avbruttAvtale.id,
                tidspunkt = LocalDateTime.now(),
                avbruttAv = bertilNavIdent,
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    aarsaker = listOf(AvbrytAvtaleAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                ),
            ).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avbrutt")),
            )
            avtaleService.avbrytAvtale(
                avsluttetAvtale.id,
                tidspunkt = LocalDateTime.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytAvtaleAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avsluttet")),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )
            val gjennomforing2 = GjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing1, gjennomforing2),
            ).initialize(database.api)

            avtaleService.avbrytAvtale(
                avtale.id,
                tidspunkt = LocalDateTime.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytAvtaleAarsak.ANNET),
                    forklaring = null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.of("Avtalen har 2 aktive gjennomføringer og kan derfor ikke avbrytes")),
            )
        }

        test("Man skal få avbryte dersom det ikke finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(avtaleId = avtale.id)

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
            ) {
                queries.gjennomforing.setStatus(
                    gjennomforing.id,
                    status = GjennomforingStatusType.AVBRUTT,
                    sluttDato = LocalDate.of(2025, 1, 14),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.api)

            avtaleService.avbrytAvtale(
                avtale.id,
                tidspunkt = LocalDateTime.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    aarsaker = listOf(AvbrytAvtaleAarsak.ANNET),
                    forklaring = ":)",
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeRight().should {
                it.status.shouldBeTypeOf<AvtaleStatus.Avbrutt>().forklaring shouldBe ":)"
            }
        }
    }

    context("avslutt avtale") {
        val avtaleService = createAvtaleService()

        test("kan bare avslutte avtale som er aktiv") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(status = AvtaleStatusType.UTKAST),
            )
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            shouldThrow<IllegalStateException> {
                avtaleService.avsluttAvtale(avtale.id, LocalDateTime.now(), bertilNavIdent)
            }.message shouldBe "Avtalen må være aktiv for å kunne avsluttes"
        }

        test("kan ikke avslutte avtale før sluttdato") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            shouldThrow<IllegalStateException> {
                avtaleService.avsluttAvtale(avtale.id, LocalDateTime.now(), bertilNavIdent)
            }.message shouldBe "Avtalen kan ikke avsluttes før sluttdato"
        }

        test("avslutter avtale når tidspunktet er etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            val avsluttetTidspunkt = avtale.detaljerDbo.sluttDato!!.plusDays(1).atStartOfDay()

            avtaleService.avsluttAvtale(avtale.id, avsluttetTidspunkt, bertilNavIdent).should {
                it.id shouldBe avtale.id
                it.status.type shouldBe AvtaleStatusType.AVSLUTTET
            }
        }
    }

    context("notifikasjoner") {
        val avtaleService = createAvtaleService()

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent

            val avtale = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                administratorer = listOf(identAnsatt1),
            )
            avtaleService.create(avtale, identAnsatt1).shouldBeRight()

            database.run {
                queries.notifications.getAll().shouldBeEmpty()
            }
        }

        test("Bare nye administratorer får notification når man endrer avtale") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val endretAv = NavIdent("B123456")
            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                administratorer = listOf(identAnsatt2),
            )
            avtaleService.create(request, endretAv).shouldBeRight()

            database.api.session {
                queries.notifications.getAll().shouldHaveSize(1).should { (first) ->
                    first.user shouldBe identAnsatt2
                }
            }

            avtaleService.upsertDetaljer(
                request.id,
                request = request.detaljer.copy(administratorer = listOf(identAnsatt1, identAnsatt2)),
                navIdent = endretAv,
            ).shouldBeRight()

            database.api.session {
                queries.notifications.getAll().shouldHaveSize(2).should { (first, second) ->
                    first.user shouldBe identAnsatt1
                    second.user shouldBe identAnsatt2
                }
            }
        }
    }

    context("opsjoner") {
        val avtaleService = createAvtaleService()

        val today = LocalDate.of(2025, 6, 1)
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        val theDayAfterTomorrow = today.plusDays(2)

        val avtale = AvtaleFixtures.oppfolging.copy(
            detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                startDato = yesterday,
                sluttDato = yesterday,
                status = AvtaleStatusType.AVSLUTTET,
                opsjonsmodell = Opsjonsmodell(
                    type = OpsjonsmodellType.TO_PLUSS_EN,
                    opsjonMaksVarighet = theDayAfterTomorrow,
                ),
            ),
        )

        test("opsjon kan ikke utløses hvis ny sluttdato er etter maks varighet for opsjon") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = today.plusMonths(1),
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeLeft(
                listOf(
                    FieldError.of(
                        "Ny sluttdato er forbi maks varighet av avtalen",
                        OpprettOpsjonLoggRequest::nySluttDato,
                    ),
                ),
            )
        }

        test("opsjon med ETT_AAR øker sluttDato med 1 år minus en dag") {
            MulighetsrommetTestDomain(
                avtaler = listOf(
                    avtale.copy(
                        detaljerDbo = avtale.detaljerDbo.copy(
                            opsjonsmodell = Opsjonsmodell(
                                type = OpsjonsmodellType.TO_PLUSS_EN,
                                opsjonMaksVarighet = avtale.detaljerDbo.startDato.plusYears(10),
                            ),
                        ),
                    ),
                ),
            ).initialize(database.api)
            val sluttDato = avtale.detaljerDbo.sluttDato!!

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = null,
                type = OpprettOpsjonLoggRequest.Type.ETT_AAR,
            )
            avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight().should {
                it.sluttDato shouldBe sluttDato.plusYears(1)
            }
        }

        test("registrering og sletting av opsjoner påvirker avtalens sluttdato og status") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            val avtale = avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()
            avtale.should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
                it.sluttDato shouldBe tomorrow
                it.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)
            }

            avtaleService.slettOpsjon(
                avtale.id,
                avtale.opsjonerRegistrert[0].id,
                bertilNavIdent,
                today,
            ).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatusType.AVSLUTTET
                it.sluttDato shouldBe yesterday
                it.opsjonerRegistrert.shouldBeEmpty()
            }
        }

        test("opsjon kan bare slettes hvis den er den siste registrerte") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()

            val request2 = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            val avtale = avtaleService.registrerOpsjon(avtale.id, request2, bertilNavIdent, today).shouldBeRight()

            avtaleService.slettOpsjon(avtale.id, avtale.opsjonerRegistrert[0].id, bertilNavIdent).shouldBeLeft(
                FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen"),
            )
        }

        test("opsjon kan ikke utløses etter at det er besluttet at ingen flere opsjoner skal utløses") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = null,
                type = OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON,
            )
            avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()

            val request2 = OpprettOpsjonLoggRequest(
                nySluttDato = theDayAfterTomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            avtaleService.registrerOpsjon(avtale.id, request2, bertilNavIdent, today).shouldBeLeft(
                listOf(
                    FieldError.of("Kan ikke utløse flere opsjoner", OpprettOpsjonLoggRequest::type),
                ),
            )
        }

        test("skal kunne slette opsjon som er registrert med status SKAL_IKKE_UTLOSE_OPSJON") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.api)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = null,
                type = OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON,
            )
            val avtale = avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()

            avtale.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)

            avtaleService.slettOpsjon(
                avtale.id,
                avtale.opsjonerRegistrert[0].id,
                bertilNavIdent,
                today,
            ).shouldBeRight().should {
                it.opsjonerRegistrert.shouldBeEmpty()
            }
        }
    }

    context("rammedetaljer") {
        val avtaleService = createAvtaleService()

        test("legger til og fjerner rammedetaljer for avtale med prismodell som støtter det") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertRammedetaljer(
                avtale.id,
                RammedetaljerRequest(totalRamme = 500_000L, utbetaltArena = 100_000L),
                bertilNavIdent,
            ).shouldBeRight().id shouldBe avtale.id

            database.api.session { queries.rammedetaljer.get(avtale.id) }.shouldNotBeNull().should {
                it.totalRamme shouldBe 500_000L
                it.utbetaltArena shouldBe 100_000L
                it.valuta shouldBe Valuta.NOK
            }

            avtaleService.upsertRammedetaljer(
                avtale.id,
                RammedetaljerRequest(totalRamme = null, utbetaltArena = null),
                bertilNavIdent,
            ).shouldBeRight().id shouldBe avtale.id

            database.api.session { queries.rammedetaljer.get(avtale.id) }.shouldBeNull()
        }

        test("kan ikke legge til rammedetaljer for avtale med forhåndsgodkjent prismodell") {
            val avtale = AvtaleFixtures.AFT
            MulighetsrommetTestDomain(avtaler = listOf(avtale)).initialize(database.api)

            avtaleService.upsertRammedetaljer(
                avtale.id,
                RammedetaljerRequest(totalRamme = 100_000L, utbetaltArena = null),
                bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError("/totalRamme", "Rammedetaljer kan kun legges til anskaffet avtaler")),
            )
        }

        test("sletter eksisterende rammedetaljer") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ) {
                queries.rammedetaljer.upsert(
                    RammedetaljerDbo(
                        avtaleId = avtale.id,
                        valuta = Valuta.NOK,
                        totalRamme = 200_000L,
                        utbetaltArena = 50_000L,
                    ),
                )
            }.initialize(database.api)

            avtaleService.deleteRammedetaljer(avtale.id, bertilNavIdent).id shouldBe avtale.id

            database.api.session { queries.rammedetaljer.get(avtale.id) }.shouldBeNull()
        }
    }

    context("frikoble kontaktperson fra avtale") {
        val avtaleService = createAvtaleService()

        test("fjerner kontaktperson fra avtalens arrangør") {
            val p1 = ArrangorFixtures.kontaktperson(arrangorId = ArrangorFixtures.hovedenhet.id)
            val avtale = AvtaleFixtures.oppfolging.copy(
                detaljerDbo = AvtaleFixtures.oppfolging.detaljerDbo.copy(
                    arrangor = AvtaleFixtures.oppfolging.detaljerDbo.arrangor?.copy(
                        kontaktpersoner = listOf(p1.id),
                    ),
                ),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(
                    ArrangorFixtures.hovedenhet.registrerKontaktpersoner(listOf(p1)),
                    ArrangorFixtures.underenhet1,
                    ArrangorFixtures.underenhet2,
                ),
                avtaler = listOf(avtale),
            ).initialize(database.api)

            avtaleService.get(avtale.id).shouldNotBeNull()
                .arrangor.shouldNotBeNull()
                .kontaktpersoner.shouldHaveSize(1)

            avtaleService.frikobleKontaktpersonFraAvtale(p1.id, avtale.id, bertilNavIdent)
                .arrangor.shouldNotBeNull()
                .kontaktpersoner.shouldBeEmpty()
        }
    }
})
