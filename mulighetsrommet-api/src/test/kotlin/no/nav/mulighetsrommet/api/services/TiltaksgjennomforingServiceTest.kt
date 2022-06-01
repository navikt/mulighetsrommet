package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
import org.slf4j.LoggerFactory

class TiltaksgjennomforingServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    beforeSpec {
        // Create tiltakstype needed by tiltaksgjennomføringer
        val tiltakstypeService = TiltakstypeService(listener.db, LoggerFactory.getLogger("TiltakstypeService"))

        tiltakstypeService.createTiltakstype(
            Tiltakstype(
                navn = "Oppfølging",
                innsatsgruppe = 1,
                tiltakskode = "INDOPPFOLG",
            )
        )
        tiltakstypeService.createTiltakstype(
            Tiltakstype(
                navn = "Arbeidstrening",
                innsatsgruppe = 1,
                tiltakskode = "ARBTREN",
            )
        )
    }

    context("CRUD") {
        val service = TiltaksgjennomforingService(listener.db, LoggerFactory.getLogger("TiltaksgjennomforingService"))

        test("should create tiltaksgjennomforing") {
            val gjennomforing0 = service.createTiltaksgjennomforing(
                Tiltaksgjennomforing(
                    navn = "Oppfølging",
                    tiltakskode = "INDOPPFOLG",
                    tiltaksnummer = 111,
                    arenaId = 1000,
                    sakId = 1000,
                )
            )

            val gjennomforing1 = service.createTiltaksgjennomforing(
                Tiltaksgjennomforing(
                    navn = "Arbeidstrening",
                    tiltakskode = "ARBTREN",
                    tiltaksnummer = 222,
                    arenaId = 1001,
                    sakId = 1001,
                )
            )

            gjennomforing0.id shouldBe 1
            gjennomforing1.id shouldBe 2
        }

        test("should get tiltaksgjennomføring by id") {
            val tiltaksgjennomforing = service.getTiltaksgjennomforingById(1)

            tiltaksgjennomforing shouldBe Tiltaksgjennomforing(
                id = 1,
                navn = "Oppfølging",
                tiltakskode = "INDOPPFOLG",
                tiltaksnummer = 111,
                arenaId = 1000,
                sakId = 1000,
            )
        }

        test("should get tiltaksgjennomføringer") {
            service.getTiltaksgjennomforinger() shouldHaveSize 2
        }

        test("should get tiltaksgjennomføringer by tiltakskode") {
            service.getTiltaksgjennomforingerByTiltakskode("ARBTREN") shouldHaveSize 1
        }

        test("should update tiltaksgjennomføring") {
            val tiltaksgjennomforing = service.updateTiltaksgjennomforing(
                1001,
                Tiltaksgjennomforing(
                    navn = "Arbeidstrening",
                    tiltakskode = "ARBTREN",
                    tiltaksnummer = 333,
                    arenaId = 1001,
                    sakId = 1001,
                )
            )

            tiltaksgjennomforing shouldBe Tiltaksgjennomforing(
                id = 2,
                navn = "Arbeidstrening",
                tiltakskode = "ARBTREN",
                tiltaksnummer = 333,
                arenaId = 1001,
                sakId = 1001,
            )
        }
    }
})
