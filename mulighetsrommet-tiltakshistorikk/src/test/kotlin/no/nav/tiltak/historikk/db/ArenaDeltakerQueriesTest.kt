package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.ArenaDeltakerDbo
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakshistorikk
import no.nav.tiltak.historikk.databaseConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaDeltakerQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val arbeidstreningArenaDeltakelse = ArenaDeltakerDbo(
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
    val mentorArenaDeltakelse = ArenaDeltakerDbo(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        arenaTiltakskode = "MENTOR",
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
        beskrivelse = "Mentortiltak hos Joblearn",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
        registrertIArenaDato = LocalDateTime.of(2002, 1, 1, 0, 0, 0),
    )

    test("CRUD") {
        database.runAndRollback { session ->
            val deltaker = ArenaDeltakerQueries(session)

            deltaker.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
            deltaker.upsertArenaDeltaker(mentorArenaDeltakelse)

            deltaker.getArenaHistorikk(
                identer = listOf(NorskIdent("12345678910")),
                null,
            ) shouldContainExactlyInAnyOrder listOf(
                Tiltakshistorikk.ArenaDeltakelse(
                    id = mentorArenaDeltakelse.id,
                    norskIdent = NorskIdent("12345678910"),
                    arenaTiltakskode = "MENTOR",
                    startDato = LocalDate.of(2002, 2, 1),
                    sluttDato = LocalDate.of(2002, 2, 1),
                    status = ArenaDeltakerStatus.GJENNOMFORES,
                    beskrivelse = "Mentortiltak hos Joblearn",
                    arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                ),
                Tiltakshistorikk.ArenaDeltakelse(
                    id = arbeidstreningArenaDeltakelse.id,
                    norskIdent = NorskIdent("12345678910"),
                    arenaTiltakskode = "ARBTREN",
                    status = ArenaDeltakerStatus.GJENNOMFORES,
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 31),
                    beskrivelse = "Arbeidstrening hos Fretex",
                    arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                ),
            )

            deltaker.deleteArenaDeltaker(mentorArenaDeltakelse.id)

            deltaker.getArenaHistorikk(identer = listOf(NorskIdent("12345678910")), null) shouldBe listOf(
                Tiltakshistorikk.ArenaDeltakelse(
                    id = arbeidstreningArenaDeltakelse.id,
                    norskIdent = NorskIdent("12345678910"),
                    arenaTiltakskode = "ARBTREN",
                    status = ArenaDeltakerStatus.GJENNOMFORES,
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 31),
                    beskrivelse = "Arbeidstrening hos Fretex",
                    arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                ),
            )

            deltaker.deleteArenaDeltaker(arbeidstreningArenaDeltakelse.id)

            deltaker.getArenaHistorikk(identer = listOf(NorskIdent("12345678910")), null).shouldBeEmpty()
        }
    }

    test("maxAgeYears arena") {
        database.runAndRollback { session ->
            val deltaker = ArenaDeltakerQueries(session)

            val mentorArenaDeltakelseUtenSlutt = ArenaDeltakerDbo(
                id = UUID.randomUUID(),
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "MENTOR",
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
                sluttDato = null,
                beskrivelse = "Mentortiltak hos Joblearn",
                arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
                registrertIArenaDato = LocalDateTime.of(2002, 1, 1, 0, 0, 0),
            )

            deltaker.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
            deltaker.upsertArenaDeltaker(mentorArenaDeltakelse)
            deltaker.upsertArenaDeltaker(mentorArenaDeltakelseUtenSlutt)

            deltaker.getArenaHistorikk(listOf(arbeidstreningArenaDeltakelse.norskIdent), 5)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(arbeidstreningArenaDeltakelse.id)

            deltaker.getArenaHistorikk(listOf(arbeidstreningArenaDeltakelse.norskIdent), null)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(
                arbeidstreningArenaDeltakelse.id,
                mentorArenaDeltakelse.id,
                mentorArenaDeltakelseUtenSlutt.id,
            )
        }
    }
})
