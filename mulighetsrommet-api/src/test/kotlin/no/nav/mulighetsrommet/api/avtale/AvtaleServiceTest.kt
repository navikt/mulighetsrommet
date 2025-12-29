package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.model.AvbrytAvtaleAarsak
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.databaseConfig
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
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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
        ),
        prismodeller = listOf(),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
        arrangorService: ArrangorService = ArrangorService(database.db, mockk()),
    ) = AvtaleService(
        config = AvtaleService.Config(mapOf()),
        database.db,
        arrangorService,
        gjennomforingPublisher,
    )

    context("opprett avtale") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("oppretter avtale med prismodell") {
            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                prismodell = PrismodellFixtures.AvtaltPrisPerTimeOppfolging,
            )

            avtaleService.create(request, bertilNavIdent).shouldBeRight().prismodeller.shouldHaveSize(1).should {
                it.first().id shouldBe PrismodellFixtures.AvtaltPrisPerTimeOppfolging.id
            }
        }

        test("oppretter avtale med arrangør hentet fra brreg") {
            val brregClient = mockk<BrregClient>()
            val orgnrHovedenhet = Organisasjonsnummer("999999999")
            val orgnrUnderenhet = Organisasjonsnummer("888888888")
            coEvery { brregClient.getBrregEnhet(orgnrHovedenhet) } returns BrregHovedenhetDto(
                organisasjonsnummer = orgnrHovedenhet,
                organisasjonsform = "AS",
                navn = "Ny arrangør hovedenhet",
                overordnetEnhet = null,
                postadresse = null,
                forretningsadresse = null,
            ).right()
            coEvery { brregClient.getBrregEnhet(orgnrUnderenhet) } returns BrregUnderenhetDto(
                organisasjonsnummer = orgnrUnderenhet,
                organisasjonsform = "AS",
                navn = "Ny arrangør underenhet",
                overordnetEnhet = orgnrHovedenhet,
            ).right()

            val arrangorService = ArrangorService(database.db, brregClient)

            val avtaleService = createAvtaleService(arrangorService = arrangorService)

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

        test("skedulerer publisering av gjennomføringer tilhørende avtalen ved endring av prismodell") {
            val avtale = AvtaleFixtures.oppfolging
            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val request = listOf(
                PrismodellRequest(
                    id = gjennomforing.prismodellId,
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    satser = emptyList(),
                    prisbetingelser = null,
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

        test("endre prismodeller") {
            val avtale = AvtaleFixtures.oppfolging
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val prismodell1Request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                satser = emptyList(),
                prisbetingelser = null,
            )
            val prismodell2Request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.ANNEN_AVTALT_PRIS,
                satser = emptyList(),
                prisbetingelser = null,
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

        test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
            val brregClient = mockk<BrregClient>()
            coEvery { brregClient.getBrregEnhet(Organisasjonsnummer("223442332")) } returns BrregError.NotFound.left()

            val arrangorService = ArrangorService(db = database.db, brregClient = brregClient)

            val avtaleService = createAvtaleService(arrangorService = arrangorService)

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
                        "/arrangorHovedenhet",
                        "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                    ),
                ),
            )
        }
    }

    context("Avbryte avtale") {
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
            }.initialize(database.db)

            avtaleService.avbrytAvtale(
                avbruttAvtale.id,
                tidspunkt = LocalDateTime.now(),
                avbruttAv = bertilNavIdent,
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    aarsaker = listOf(AvbrytAvtaleAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                ),
            ).shouldBeLeft(
                listOf(FieldError.root("Avtalen er allerede avbrutt")),
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
                listOf(FieldError.root("Avtalen er allerede avsluttet")),
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
            ).initialize(database.db)

            avtaleService.avbrytAvtale(
                avtale.id,
                tidspunkt = LocalDateTime.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytAvtaleAarsak.ANNET),
                    forklaring = null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.root("Avtalen har 2 aktive gjennomføringer og kan derfor ikke avbrytes")),
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
                    tidspunkt = LocalDate.of(2025, 1, 15).atStartOfDay(),
                    aarsaker = listOf(AvbrytGjennomforingAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

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

    context("Administrator-notification") {
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

            val request = AvtaleFixtures.createAvtaleRequest(
                Tiltakskode.OPPFOLGING,
                administratorer = listOf(identAnsatt2),
            )
            avtaleService.create(request, identAnsatt1).shouldBeRight()

            database.run {
                queries.notifications.getAll().shouldHaveSize(1).first().should {
                    it.user shouldBe identAnsatt2
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
            ).initialize(database.db)

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
            ).initialize(database.db)
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
            ).initialize(database.db)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            val dto = avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()
            dto.should {
                it.status.type shouldBe AvtaleStatusType.AKTIV
                it.sluttDato shouldBe tomorrow
                it.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)
            }

            avtaleService.slettOpsjon(avtale.id, dto.opsjonerRegistrert[0].id, bertilNavIdent, today).shouldBeRight()
                .should {
                    it.status.type shouldBe AvtaleStatusType.AVSLUTTET
                    it.sluttDato shouldBe yesterday
                    it.opsjonerRegistrert.shouldBeEmpty()
                }
        }

        test("opsjon kan bare slettes hvis den er den siste registrerte") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()

            val request2 = OpprettOpsjonLoggRequest(
                nySluttDato = tomorrow,
                type = OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
            )
            val dto = avtaleService.registrerOpsjon(avtale.id, request2, bertilNavIdent, today).shouldBeRight()

            avtaleService.slettOpsjon(avtale.id, dto.opsjonerRegistrert[0].id, bertilNavIdent).shouldBeLeft(
                FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen"),
            )
        }

        test("opsjon kan ikke utløses etter at det er besluttet at ingen flere opsjoner skal utløses") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

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
            ).initialize(database.db)

            val request = OpprettOpsjonLoggRequest(
                nySluttDato = null,
                type = OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON,
            )
            val dto = avtaleService.registrerOpsjon(avtale.id, request, bertilNavIdent, today).shouldBeRight()
            dto.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)

            avtaleService.slettOpsjon(avtale.id, dto.opsjonerRegistrert[0].id, bertilNavIdent, today).shouldBeRight()
                .should {
                    it.opsjonerRegistrert.shouldBeEmpty()
                }
        }
    }
})
