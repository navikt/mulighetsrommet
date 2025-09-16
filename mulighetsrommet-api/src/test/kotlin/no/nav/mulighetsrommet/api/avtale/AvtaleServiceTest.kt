package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
        arrangorService: ArrangorService = mockk(relaxed = true),
        navEnhetService: NavEnhetService = NavEnhetService(db = database.db),
    ) = AvtaleService(
        database.db,
        gjennomforingPublisher,
        arrangorService,
        navEnhetService,
    )

    context("Upsert avtale") {
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
            val request = AvtaleFixtures.avtaleRequest

            avtaleService.upsert(request, bertilNavIdent).shouldBeRight()

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = AvtaleFixtures.avtaleRequest.id),
                    any(),
                    any(),
                )
            }
        }
    }

    context("Avbryte avtale") {
        val avtaleService = createAvtaleService()

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avbruttAvtale = AvtaleFixtures.oppfolgingDbo.copy(
                id = UUID.randomUUID(),
            )
            val avsluttetAvtale = AvtaleFixtures.oppfolgingDbo.copy(
                id = UUID.randomUUID(),
                status = AvtaleStatusType.AVSLUTTET,
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
            val avtale = AvtaleFixtures.oppfolgingDbo
            val oppfolging1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )
            val oppfolging2 = GjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging1, oppfolging2),
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
            val avtale = AvtaleFixtures.oppfolgingDbo
            val oppfolging1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatusType.AVBRUTT,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging1),
            ).initialize(database.db)

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

            val avtale = AvtaleFixtures.avtaleRequest
                .copy(administratorer = listOf(identAnsatt1))
            avtaleService.upsert(avtale, identAnsatt1).shouldBeRight()

            database.run {
                queries.notifications.getAll().shouldBeEmpty()
            }
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val avtale = AvtaleFixtures.avtaleRequest
                .copy(administratorer = listOf(identAnsatt2))
            avtaleService.upsert(avtale, identAnsatt1).shouldBeRight()

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

        val avtale = AvtaleFixtures.oppfolgingDbo.copy(
            startDato = yesterday,
            sluttDato = yesterday,
            status = AvtaleStatusType.AVSLUTTET,
            opsjonsmodell = Opsjonsmodell(
                type = OpsjonsmodellType.TO_PLUSS_EN,
                opsjonMaksVarighet = theDayAfterTomorrow,
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
                        opsjonsmodell = Opsjonsmodell(
                            type = OpsjonsmodellType.TO_PLUSS_EN,
                            opsjonMaksVarighet = avtale.startDato.plusYears(10),
                        ),
                    ),
                ),
            ).initialize(database.db)
            val sluttDato = avtale.sluttDato!!

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

            avtaleService.slettOpsjon(avtale.id, dto.opsjonerRegistrert[0].id, bertilNavIdent, today).shouldBeRight().should {
                it.opsjonerRegistrert.shouldBeEmpty()
            }
        }
    }

    test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
        val arrangorService = mockk<ArrangorService>()
        coEvery { arrangorService.getArrangorOrSyncFromBrreg(Organisasjonsnummer("223442332")) } returns BrregError.NotFound.left()

        createAvtaleService(arrangorService = arrangorService).upsert(
            AvtaleFixtures.avtaleRequest.copy(
                arrangor = AvtaleRequest.Arrangor(
                    hovedenhet = Organisasjonsnummer("223442332"),
                    underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                    kontaktpersoner = emptyList(),
                ),
            ),
            NavIdent("B123456"),
        ).shouldBeLeft() shouldBe
            listOf(
                FieldError(
                    "/arrangorHovedenhet",
                    "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                ),
            )
    }

    context("status endringer") {
        test("status blir UTKAST når avtalen lagres uten en arrangør") {
            resolveStatus(AvtaleFixtures.avtaleRequest.copy(arrangor = null), null, LocalDate.now()) shouldBe AvtaleStatusType.UTKAST
        }

        test("status blir AKTIV når avtalen lagres med sluttdato i fremtiden") {
            resolveStatus(
                AvtaleFixtures.avtaleRequest.copy(
                    arrangor = AvtaleRequest.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                        underenheter = emptyList(),
                        kontaktpersoner = emptyList(),
                    ),
                ),
                null,
                LocalDate.now(),
            ) shouldBe AvtaleStatusType.AKTIV
        }

        test("status blir AVSLUTTET når avtalen lagres med en sluttdato som er passert") {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            resolveStatus(
                AvtaleFixtures.avtaleRequest.copy(
                    sluttDato = yesterday,
                    arrangor = AvtaleRequest.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                        underenheter = emptyList(),
                        kontaktpersoner = emptyList(),
                    ),
                ),
                null,
                today,
            ) shouldBe AvtaleStatusType.AVSLUTTET
        }

        test("status forblir AVBRUTT på en avtale som allerede er AVBRUTT") {
            val today = LocalDate.now()
            val avtale = AvtaleFixtures.oppfolging

            val request = AvtaleFixtures.avtaleRequest.copy(
                arrangor = AvtaleRequest.Arrangor(
                    hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                    underenheter = emptyList(),
                    kontaktpersoner = emptyList(),
                ),
                id = avtale.id,
                startDato = today,
                sluttDato = today,
            )
            resolveStatus(
                request,
                avtale.copy(status = AvtaleStatus.Avbrutt(tidspunkt = LocalDateTime.now(), aarsaker = emptyList(), forklaring = null)),
                LocalDate.now(),
            ) shouldBe AvtaleStatusType.AVBRUTT
        }
    }
})
