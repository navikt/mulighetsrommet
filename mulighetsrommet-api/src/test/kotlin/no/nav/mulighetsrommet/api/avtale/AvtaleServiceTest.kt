package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService

class AvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<AvtaleDbo>().right()
        }
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        brregClient: BrregClient = mockk(relaxed = true),
        gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
    ) = AvtaleService(
        database.db,
        ArrangorService(database.db, brregClient),
        TiltakstypeService(database.db),
        validator,
        gjennomforingPublisher,
    )

    context("Upsert avtale") {
        val brregClient = mockk<BrregClient>()
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(brregClient, gjennomforingPublisher)

        test("får ikke opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            every { validator.validate(any(), any()) } returns listOf(
                FieldError("navn", "Dårlig navn"),
            ).left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(FieldError("navn", "Dårlig navn")),
            )
        }

        test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
            val request = AvtaleFixtures.avtaleRequest.copy(
                arrangor = AvtaleFixtures.avtaleRequest.arrangor?.copy(
                    hovedenhet = Organisasjonsnummer("888777435"),
                    underenheter = listOf(),
                ),
            )

            coEvery { brregClient.getBrregEnhet(Organisasjonsnummer("888777435")) } returns BrregError.NotFound.left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(
                    FieldError(
                        "/arrangor/hovedenhet",
                        "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                    ),
                ),
            )
        }

        test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
            val request = AvtaleFixtures.avtaleRequest

            avtaleService.upsert(request, bertilNavIdent)

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = request.id),
                    any(),
                    any(),
                )
            }
        }

        test("status blir UTKAST når avtalen lagres uten en arrangør") {
            val request = AvtaleFixtures.avtaleRequest.copy(arrangor = null)

            avtaleService.upsert(request, bertilNavIdent).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.UTKAST
            }
        }

        test("status blir AKTIV når avtalen lagres med sluttdato i fremtiden") {
            val today = LocalDate.of(2025, 1, 1)

            val request = AvtaleFixtures.avtaleRequest.copy(
                startDato = today,
                sluttDato = today,
            )

            avtaleService.upsert(request, bertilNavIdent, today).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.AKTIV
            }
        }

        test("status blir AVSLUTTET når avtalen lagres med en sluttdato som er passert") {
            val today = LocalDate.of(2025, 1, 1)
            val yesterday = today.minusDays(1)

            val request = AvtaleFixtures.avtaleRequest.copy(
                startDato = yesterday,
                sluttDato = yesterday,
            )

            avtaleService.upsert(request, bertilNavIdent, today).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.AVSLUTTET
            }
        }

        test("status forblir AVBRUTT på en avtale som allerede er AVBRUTT") {
            val today = LocalDate.of(2025, 1, 1)

            val avtale = AvtaleFixtures.oppfolging

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ) {
                queries.avtale.setStatus(
                    avtale.id,
                    AvtaleStatus.AVBRUTT,
                    today.atStartOfDay(),
                    AvbruttAarsak.BudsjettHensyn,
                )
            }.initialize(database.db)

            val request = AvtaleFixtures.avtaleRequest.copy(
                id = avtale.id,
                startDato = today,
                sluttDato = today,
            )

            avtaleService.upsert(request, bertilNavIdent, today).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.AVBRUTT
            }
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
                status = AvtaleStatus.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avbruttAvtale, avsluttetAvtale),
            ) {
                queries.avtale.setStatus(
                    avbruttAvtale.id,
                    AvtaleStatus.AVBRUTT,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                )
            }.initialize(database.db)

            avtaleService.avbrytAvtale(avbruttAvtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                FieldError.root("Avtalen er allerede avbrutt"),
            )
            avtaleService.avbrytAvtale(avsluttetAvtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                FieldError.root("Avtalen er allerede avsluttet"),
            )
        }

        test("beskrivelse er påkrevd dersom årsaken er Annet") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)

            avtaleService.avbrytAvtale(AvtaleFixtures.oppfolging.id, bertilNavIdent, AvbruttAarsak.Annet(""))
                .shouldBeLeft(
                    FieldError.root("Beskrivelse er obligatorisk når “Annet” er valgt som årsak"),
                )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging
            val oppfolging1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatus.GJENNOMFORES,
            )
            val oppfolging2 = GjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatus.GJENNOMFORES,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging1, oppfolging2),
            ).initialize(database.db)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                FieldError.root("Avtalen har 2 aktive gjennomføringer og kan derfor ikke avbrytes"),
            )
        }

        test("Man skal få avbryte dersom det ikke finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging
            val oppfolging1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatus.AVBRUTT,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
                gjennomforinger = listOf(oppfolging1),
            ).initialize(database.db)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Annet(":)")).shouldBeRight().should {
                it.status.shouldBeTypeOf<AvtaleStatusDto.Avbrutt>().beskrivelse shouldBe ":)"
            }
        }
    }

    context("avslutt avtale") {
        val avtaleService = createAvtaleService()

        test("blir valideringsfeil hvis avtalen ikke er aktiv") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                status = AvtaleStatus.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            avtaleService.avsluttAvtale(avtale.id, LocalDateTime.now(), bertilNavIdent).shouldBeLeft(
                FieldError.root("Avtalen må være aktiv for å kunne avsluttes"),
            )
        }

        test("tidspunkt for avslutning må være etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 1, 31),
                status = AvtaleStatus.AKTIV,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val avsluttetTidspunkt = LocalDate.of(2025, 1, 31).atStartOfDay()
            avtaleService.avsluttAvtale(avtale.id, avsluttetTidspunkt, bertilNavIdent).shouldBeLeft(
                FieldError.root("Avtalen kan ikke avsluttes før sluttdato"),
            )
        }

        test("avslutter avtale og oppdaterer status") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 1, 31),
                status = AvtaleStatus.AKTIV,
            )

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val avsluttetTidspunkt = LocalDate.of(2025, 2, 1).atStartOfDay()
            avtaleService.avsluttAvtale(avtale.id, avsluttetTidspunkt, bertilNavIdent).shouldBeRight().should {
                it.status shouldBe AvtaleStatusDto.Avsluttet
            }
        }
    }

    context("Administrator-notification") {
        val avtaleService = createAvtaleService()

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent

            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt1))
            avtaleService.upsert(avtale, identAnsatt1)

            database.run {
                queries.notifications.getAll().shouldBeEmpty()
            }
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt2))
            avtaleService.upsert(avtale, identAnsatt1)

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
            startDato = yesterday,
            sluttDato = yesterday,
            status = AvtaleStatus.AVSLUTTET,
            opsjonsmodell = Opsjonsmodell(
                type = OpsjonsmodellType.TO_PLUSS_EN,
                opsjonMaksVarighet = theDayAfterTomorrow,
            ),
        )

        test("opsjon kan ikke utløses hvis ny sluttdato er etter maks varighet for opsjon") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val entry = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = yesterday,
                sluttdato = today.plusMonths(1),
                status = OpsjonLoggStatus.OPSJON_UTLOST,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry, today).shouldBeLeft(
                FieldError.of(
                    "Ny sluttdato er forbi maks varighet av avtalen",
                    OpsjonLoggEntry::sluttdato,
                ),
            )
        }

        test("registrering og sletting av opsjoner påvirker avtalens sluttdato og status") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val entry = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = yesterday,
                sluttdato = tomorrow,
                status = OpsjonLoggStatus.OPSJON_UTLOST,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry, today).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.AKTIV
                it.sluttDato shouldBe tomorrow
                it.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)
            }

            avtaleService.slettOpsjon(avtale.id, entry.id, bertilNavIdent, today).shouldBeRight().should {
                it.status.type shouldBe AvtaleStatus.AVSLUTTET
                it.sluttDato shouldBe yesterday
                it.opsjonerRegistrert.shouldBeEmpty()
            }
        }

        test("opsjon kan bare slettes hvis den er den siste registrerte") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val entry1 = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = yesterday,
                sluttdato = tomorrow,
                status = OpsjonLoggStatus.OPSJON_UTLOST,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry1, today).shouldBeRight()

            val entry2 = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = tomorrow,
                sluttdato = theDayAfterTomorrow,
                status = OpsjonLoggStatus.OPSJON_UTLOST,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry2, today).shouldBeRight()

            avtaleService.slettOpsjon(avtale.id, entry1.id, bertilNavIdent).shouldBeLeft(
                FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen"),
            )
        }

        test("opsjon kan ikke utløses etter at det er besluttet at ingen flere opsjoner skal utløses") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val entry = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                // TODO : unødvendig å sende med fra frontend
                forrigeSluttdato = yesterday,
                sluttdato = null,
                status = OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry, today).shouldBeRight()

            val entry2 = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = tomorrow,
                sluttdato = theDayAfterTomorrow,
                status = OpsjonLoggStatus.OPSJON_UTLOST,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry2, today).shouldBeLeft(
                FieldError.of("Kan ikke utløse flere opsjoner", OpsjonLoggEntry::status),
            )
        }

        test("skal kunne slette opsjon som er registrert med status SKAL_IKKE_UTLOSE_OPSJON") {
            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ).initialize(database.db)

            val entry = OpsjonLoggEntry(
                id = UUID.randomUUID(),
                avtaleId = avtale.id,
                forrigeSluttdato = null,
                sluttdato = null,
                status = OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON,
                registretDato = today,
                registrertAv = bertilNavIdent,
            )
            avtaleService.registrerOpsjon(entry, today).shouldBeRight().should {
                it.opsjonerRegistrert.shouldNotBeNull().shouldHaveSize(1)
            }

            avtaleService.slettOpsjon(avtale.id, entry.id, bertilNavIdent, today).shouldBeRight().should {
                it.opsjonerRegistrert.shouldBeEmpty()
            }
        }
    }
})
