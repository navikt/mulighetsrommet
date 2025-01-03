package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.ApiDatabaseTestListener
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate

class UpdateApentForPameldingTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                ),
                TiltaksgjennomforingFixtures.GruppeAmo1.copy(
                    startDato = startDato,
                    sluttDato = sluttDato,
                    oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                ),
            ),
        ) {
            Queries.gjennomforing.setApentForPamelding(TiltaksgjennomforingFixtures.Jobbklubb1.id, true)
            Queries.gjennomforing.setApentForPamelding(TiltaksgjennomforingFixtures.GruppeAmo1.id, true)
        }

        val service = TiltaksgjennomforingService(
            database.db,
            mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true),
            NotificationRepository(database.db.db),
            mockk<TiltaksgjennomforingValidator>(),
            EndringshistorikkService(database.db.db),
            mockk<NavAnsattService>(relaxed = true),
        )

        val updateApentForPamelding = UpdateApentForPamelding(
            config = UpdateApentForPamelding.Config(disabled = true),
            db = database.db.db,
            tiltaksgjennomforingService = service,
        )

        beforeEach {
            database.run {
                domain.setup(it)
            }
        }

        test("beholder påmelding når dato er før startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = startDato.minusDays(1))

            database.run {
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
            }
        }

        test("stenger påmelding for tiltak med felles oppstart når dato er lik startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now())

            database.run {
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe false
                }
            }
        }

        test("beholder påmelding når dato er etter startDato på tiltaket") {
            updateApentForPamelding.stengTiltakMedFellesOppstartForPamelding(startDato = LocalDate.now().plusDays(1))

            database.run {
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.Jobbklubb1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
                Queries.gjennomforing.get(TiltaksgjennomforingFixtures.GruppeAmo1.id).shouldNotBeNull().should {
                    it.apentForPamelding shouldBe true
                }
            }
        }
    }
})
