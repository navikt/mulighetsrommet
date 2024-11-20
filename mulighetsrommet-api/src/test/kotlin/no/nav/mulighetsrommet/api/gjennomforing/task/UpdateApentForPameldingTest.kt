package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate

class UpdateApentForPameldingTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("steng påmelding for tiltak med felles oppstart") {
        val startDato = LocalDate.now()
        val sluttDato = LocalDate.now().plusMonths(1)

        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Jobbklubb, TiltakstypeFixtures.GruppeAmo),
            avtaler = listOf(AvtaleFixtures.jobbklubb, AvtaleFixtures.gruppeAmo),
            gjennomforinger = listOf(
                TiltaksgjennomforingFixtures.Jobbklubb1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    apentForPamelding = true,
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                ),
                TiltaksgjennomforingFixtures.GruppeAmo1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    apentForPamelding = true,
                    oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                ),
            ),
        )

        val gjennomforinger = TiltaksgjennomforingRepository(database.db)

        val service = TiltaksgjennomforingService(
            AvtaleRepository(database.db),
            gjennomforinger,
            TilsagnRepository(database.db),
            mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true),
            NotificationRepository(database.db),
            mockk<TiltaksgjennomforingValidator>(),
            EndringshistorikkService(database.db),
            mockk<NavAnsattService>(relaxed = true),
            database.db,
        )

        val updateApentForPamelding = UpdateApentForPamelding(
            config = UpdateApentForPamelding.Config(disabled = true),
            db = database.db,
            tiltaksgjennomforingService = service,
        )

        test("beholder påmelding når dato er før startDato på tiltaket") {
            domain.initialize(database.db)

            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = startDato.minusDays(1))

            gjennomforinger.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe true
            }
            gjennomforinger.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe true
            }
        }

        test("stenger påmelding for tiltak med felles oppstart når dato er lik startDato på tiltaket") {
            domain.initialize(database.db)

            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now())

            gjennomforinger.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe true
            }
            gjennomforinger.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe false
            }
        }

        test("beholder påmelding når dato er etter startDato på tiltaket") {
            domain.initialize(database.db)

            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now().plusDays(1))

            gjennomforinger.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe true
            }
            gjennomforinger.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                it.apentForPamelding shouldBe true
            }
        }
    }
})
