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
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
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
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        coEvery { validator.validate(any(), any()) } answers {
            firstArg<AvtaleDbo>().right()
        }
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
    ) = AvtaleService(
        database.db,
        validator,
        gjennomforingPublisher,
    )

    context("Upsert avtale") {
        val brregClient = mockk<BrregClient>()
        val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(gjennomforingPublisher)

        test("får ikke opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            coEvery { validator.validate(any(), any()) } returns listOf(
                FieldError("navn", "Dårlig navn"),
            ).left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(FieldError("navn", "Dårlig navn")),
            )
        }

        test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
            val request = AvtaleFixtures.avtaleRequest

            coEvery { validator.validate(any(), any()) } returns AvtaleFixtures.oppfolging.right()
            avtaleService.upsert(request, bertilNavIdent)

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadGjennomforinger.Input(avtaleId = AvtaleFixtures.oppfolging.id),
                    any(),
                    any(),
                )
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
            val oppfolging1 = GjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                status = GjennomforingStatus.AVBRUTT,
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
                it.status.shouldBeTypeOf<AvtaleStatusDto.Avbrutt>().forklaring shouldBe ":)"
            }
        }
    }

    context("Administrator-notification") {
        val avtaleService = createAvtaleService()

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent

            val avtale = AvtaleFixtures.avtaleRequest
            coEvery { validator.validate(any(), any()) } returns AvtaleFixtures.oppfolging
                .copy(administratorer = listOf(identAnsatt1))
                .right()
            avtaleService.upsert(avtale, identAnsatt1)

            database.run {
                queries.notifications.getAll().shouldBeEmpty()
            }
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val avtale = AvtaleFixtures.avtaleRequest
            coEvery { validator.validate(any(), any()) } returns AvtaleFixtures.oppfolging
                .copy(administratorer = listOf(identAnsatt2))
                .right()
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
