package no.nav.mulighetsrommet.tiltakshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    test("CRUD") {
        val tiltakshistorikk = DeltakerRepository(database.db)

        val arbeidstrening = ArenaDeltakerDbo(
            id = UUID.randomUUID(),
            norskIdent = NorskIdent("12345678910"),
            arenaTiltakskode = "ARBTREN",
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            sluttDato = LocalDateTime.of(2024, 1, 31, 0, 0, 0),
            beskrivelse = "Arbeidstrening hos Fretex",
            arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
            registrertIArenaDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        )

        val mentor = ArenaDeltakerDbo(
            id = UUID.randomUUID(),
            norskIdent = NorskIdent("12345678910"),
            arenaTiltakskode = "MENTOR",
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDateTime.of(2024, 2, 1, 0, 0, 0),
            sluttDato = LocalDateTime.of(2024, 2, 29, 0, 0, 0),
            beskrivelse = "Mentortiltak hos Joblearn",
            arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
            registrertIArenaDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        )

        tiltakshistorikk.upsertArenaDeltaker(arbeidstrening)
        tiltakshistorikk.upsertArenaDeltaker(mentor)

        tiltakshistorikk.getArenaDeltakelser(identer = listOf("12345678910")) shouldBe listOf(
            mentor,
            arbeidstrening,
        )

        tiltakshistorikk.deleteArenaDeltaker(mentor.id)

        tiltakshistorikk.getArenaDeltakelser(identer = listOf("12345678910")) shouldBe listOf(
            arbeidstrening,
        )

        tiltakshistorikk.deleteArenaDeltaker(arbeidstrening.id)

        tiltakshistorikk.getArenaDeltakelser(identer = listOf("12345678910")).shouldBeEmpty()
    }
})
