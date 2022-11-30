package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.repositories.AnsattTiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo.Tilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val tiltaksgjennomforingId = UUID.fromString("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee")

    beforeSpec {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltakstypeId = UUID.fromString("0c565576-6a74-4bc2-ad5a-765580014ef9")

        tiltakstypeRepository.upsert(
            TiltakstypeDbo(
                tiltakstypeId,
                "",
                "",
                rettPaaTiltakspenger = true,
                registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                fraDato = LocalDate.of(2023, 1, 11),
                tilDato = LocalDate.of(2023, 1, 12)
            )
        )

        tiltaksgjennomforingRepository.upsert(
            TiltaksgjennomforingDbo(
                id = tiltaksgjennomforingId,
                navn = "",
                tiltakstypeId = tiltakstypeId,
                tiltaksnummer = "",
                virksomhetsnummer = "976663934",
                enhet = "",
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                startDato = LocalDate.of(2022, 1, 1),
                tilgjengelighet = Tilgjengelighetsstatus.Ledig,
                antallPlasser = null,
            )
        )
    }

    context("TiltaksgjennomforingService") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val ansattTiltaksgjennomforingRepository = AnsattTiltaksgjennomforingRepository(database.db)
        val service = TiltaksgjennomforingService(tiltaksgjennomforingRepository, ansattTiltaksgjennomforingRepository)

        test("Insert favoritt i liste") {
            service.lagreGjennomforingTilAnsattsListe(tiltaksgjennomforingId, "1")
            database.assertThat("ansatt_tiltaksgjennomforing").row().value("navident").isEqualTo("1")
                .value("tiltaksgjennomforing_id").isEqualTo("046b57eb-1c7e-4165-ac5d-39ad21ebf4ee")
        }

        test("Fjern gjennomføring fra favorittliste") {
            service.lagreGjennomforingTilAnsattsListe(tiltaksgjennomforingId, "1")
            service.fjernGjennomforingFraAnsattsListe(tiltaksgjennomforingId, "1")
            database.assertThat("ansatt_tiltaksgjennomforing").hasNumberOfRows(0)
        }
    }
})
