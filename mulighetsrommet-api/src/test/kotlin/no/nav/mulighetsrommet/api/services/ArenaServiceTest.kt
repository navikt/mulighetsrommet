package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.domain.*
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ArenaServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    context("ArenaService") {

        val service = ArenaService(listener.db, LoggerFactory.getLogger("ArenaService"))

        test("upsert tiltakstype") {
            val tiltakstype = Tiltakstype(
                navn = "Arbeidstrening",
                innsatsgruppe = 1,
                tiltakskode = "ARBTREN",
                fraDato = LocalDateTime.now(),
                tilDato = LocalDateTime.now().plusYears(1)
            )

            val created = service.upsertTiltakstype(tiltakstype)
            created shouldBe tiltakstype.copy(id = 1)

            val updated = service.upsertTiltakstype(tiltakstype.copy(innsatsgruppe = 2))
            updated shouldBe tiltakstype.copy(id = 1, innsatsgruppe = 2)
        }

        test("upsert tiltaksgjennomføring") {
            val tiltaksgjennomforing = Tiltaksgjennomforing(
                navn = "Arbeidstrening",
                arrangorId = 1,
                tiltakskode = "ARBTREN",
                tiltaksnummer = 1,
                arenaId = 123,
                sakId = 123,
            )

            val created = service.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            created shouldBe tiltaksgjennomforing.copy(id = 1)

            val updated = service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(tiltaksnummer = 2))
            updated shouldBe tiltaksgjennomforing.copy(id = 1, tiltaksnummer = 2)
        }

        test("upsert deltaker") {
            val deltaker = Deltaker(
                arenaId = 123,
                tiltaksgjennomforingId = 123,
                personId = 111,
                status = Deltakerstatus.VENTER
            )

            val created = service.upsertDeltaker(deltaker)
            created shouldBe deltaker.copy(id = 1)

            val updated = service.upsertDeltaker(deltaker.copy(status = Deltakerstatus.DELTAR))
            updated shouldBe deltaker.copy(id = 1, status = Deltakerstatus.DELTAR)
        }
    }
})
