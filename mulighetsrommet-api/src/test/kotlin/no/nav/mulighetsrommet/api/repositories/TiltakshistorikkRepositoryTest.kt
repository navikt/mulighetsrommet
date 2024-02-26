package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltakstypeIndividuell = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Høyere utdanning",
        tiltakskode = "HOYEREUTD",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    beforeSpec {
        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstypeIndividuell)
    }

    context("deleteByExpirationDate") {
        val historikkUtenTilDato = ArenaTiltakshistorikkDbo.IndividueltTiltak(
            id = UUID.randomUUID(),
            tiltakstypeId = tiltakstypeIndividuell.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = null,
            tilDato = null,
            registrertIArenaDato = LocalDateTime.of(2018, 1, 1, 0, 0),
            beskrivelse = "Utdanning",
            arrangorOrganisasjonsnummer = "123456789",
        )

        val historikkMedTilDato = ArenaTiltakshistorikkDbo.IndividueltTiltak(
            id = UUID.randomUUID(),
            tiltakstypeId = tiltakstypeIndividuell.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.of(2019, 1, 1, 0, 0),
            tilDato = LocalDateTime.of(2019, 12, 1, 0, 0),
            registrertIArenaDato = LocalDateTime.of(2018, 1, 1, 0, 0),
            beskrivelse = "Utdanning",
            arrangorOrganisasjonsnummer = "123456789",
        )

        test("should not delete historikk that is newer than the specified expiration date") {
            val tiltakshistorikk = TiltakshistorikkRepository(database.db)
            tiltakshistorikk.upsert(historikkUtenTilDato).shouldBeRight()
            tiltakshistorikk.upsert(historikkMedTilDato).shouldBeRight()

            tiltakshistorikk.deleteByExpirationDate(LocalDate.of(2015, 1, 1)).shouldBeRight(0)

            tiltakshistorikk.getTiltakshistorikkForBruker(listOf("12345678910")).shouldHaveSize(2)
        }

        test("should delete historikk that is older than the specified expiration date") {
            val tiltakshistorikk = TiltakshistorikkRepository(database.db)
            tiltakshistorikk.upsert(historikkUtenTilDato).shouldBeRight()
            tiltakshistorikk.upsert(historikkMedTilDato).shouldBeRight()

            tiltakshistorikk.deleteByExpirationDate(LocalDate.of(2019, 1, 1)).shouldBeRight(1)

            tiltakshistorikk.getTiltakshistorikkForBruker(listOf("12345678910")).shouldHaveSize(1)

            tiltakshistorikk.deleteByExpirationDate(LocalDate.of(2020, 1, 1)).shouldBeRight(1)

            tiltakshistorikk.getTiltakshistorikkForBruker(listOf("12345678910")).shouldHaveSize(0)
        }
    }

    test("delete by ident liste") {
        val historikk1 = ArenaTiltakshistorikkDbo.IndividueltTiltak(
            id = UUID.randomUUID(),
            tiltakstypeId = tiltakstypeIndividuell.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = null,
            tilDato = null,
            registrertIArenaDato = LocalDateTime.of(2018, 1, 1, 0, 0),
            beskrivelse = "Utdanning",
            arrangorOrganisasjonsnummer = "123456789",
        )

        val historikk2 = ArenaTiltakshistorikkDbo.IndividueltTiltak(
            id = UUID.randomUUID(),
            tiltakstypeId = tiltakstypeIndividuell.id,
            norskIdent = "22222678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.of(2019, 1, 1, 0, 0),
            tilDato = LocalDateTime.of(2019, 12, 1, 0, 0),
            registrertIArenaDato = LocalDateTime.of(2018, 1, 1, 0, 0),
            beskrivelse = "Utdanning",
            arrangorOrganisasjonsnummer = "123456789",
        )

        val tiltakshistorikk = TiltakshistorikkRepository(database.db)
        tiltakshistorikk.upsert(historikk1).shouldBeRight()
        tiltakshistorikk.upsert(historikk2).shouldBeRight()

        tiltakshistorikk.deleteTiltakshistorikkForIdenter(listOf("12345678910", "xyz", "123456789101234567"))

        tiltakshistorikk.getTiltakshistorikkForBruker(listOf("12345678910")).shouldHaveSize(0)
        tiltakshistorikk.getTiltakshistorikkForBruker(listOf("22222678910")).shouldHaveSize(1)
    }
})
