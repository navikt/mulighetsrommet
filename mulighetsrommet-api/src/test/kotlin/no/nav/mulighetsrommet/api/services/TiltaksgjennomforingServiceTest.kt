package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import no.nav.mulighetsrommet.api.repositories.AnsattTiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    beforeSpec {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tiltakstypeReposity = TiltakstypeRepository(database.db)
        val tiltakstypeId = UUID.fromString("0c565576-6a74-4bc2-ad5a-765580014ef9")

        tiltakstypeReposity.upsert(
            TiltakstypeDbo(
                tiltakstypeId,
                "",
                ""
            )
        )

        tiltaksgjennomforingRepository.upsert(
            TiltaksgjennomforingDbo(
                id = UUID.fromString("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee"),
                navn = null,
                tiltakstypeId = tiltakstypeId,
                tiltaksnummer = "",
                virksomhetsnummer = null,
                fraDato = null,
                tilDato = null,
                enhet = ""
            )
        )
    }

    context("TiltaksgjennomforingService") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val ansattTiltaksgjennomforingRepository = AnsattTiltaksgjennomforingRepository(database.db)
        val service = TiltaksgjennomforingService(tiltaksgjennomforingRepository, ansattTiltaksgjennomforingRepository)

        test("Insert favoritt i liste") {
            val table = Table(database.db.getDatasource(), "ansatt_tiltaksgjennomforing")

            service.lagreGjennomforingTilAnsattsListe("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee", "1")
            assertThat(table).row(0).column("navident").value().isEqualTo("1")
            assertThat(table).row(0).column("tiltaksgjennomforing_id").value().isEqualTo("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee")
        }

        test("Fjern gjennomføring fra favorittliste") {
            val table = Table(database.db.getDatasource(), "ansatt_tiltaksgjennomforing")

            service.lagreGjennomforingTilAnsattsListe("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee", "1")
            service.fjernGjennomforingFraAnsattsListe("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee", "1")
            assertThat(table).hasNumberOfRows(0)
        }
    }
})
