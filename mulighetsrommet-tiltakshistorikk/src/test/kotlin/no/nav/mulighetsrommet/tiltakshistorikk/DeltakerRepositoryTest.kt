package no.nav.mulighetsrommet.tiltakshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import java.time.LocalDate
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

        tiltakshistorikk.getArenaHistorikk(identer = listOf(NorskIdent("12345678910"))) shouldBe listOf(
            Tiltakshistorikk.ArenaDeltakelse(
                id = mentor.id,
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "MENTOR",
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDate.of(2024, 2, 1),
                sluttDato = LocalDate.of(2024, 2, 29),
                beskrivelse = "Mentortiltak hos Joblearn",
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
            ),
            Tiltakshistorikk.ArenaDeltakelse(
                id = arbeidstrening.id,
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "ARBTREN",
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 31),
                beskrivelse = "Arbeidstrening hos Fretex",
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
            ),
        )

        tiltakshistorikk.deleteArenaDeltaker(mentor.id)

        tiltakshistorikk.getArenaHistorikk(identer = listOf(NorskIdent("12345678910"))) shouldBe listOf(
            Tiltakshistorikk.ArenaDeltakelse(
                id = arbeidstrening.id,
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "ARBTREN",
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 31),
                beskrivelse = "Arbeidstrening hos Fretex",
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
            ),
        )

        tiltakshistorikk.deleteArenaDeltaker(arbeidstrening.id)

        tiltakshistorikk.getArenaHistorikk(identer = listOf(NorskIdent("12345678910"))).shouldBeEmpty()
    }

    test("kometHistorikk") {
        val deltakerRepository = DeltakerRepository(database.db)
        val gruppetiltakRepository = GruppetiltakRepository(database.db)

        val tiltak = TiltaksgjennomforingV1Dto(
            id = UUID.randomUUID(),
            tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Gruppe AMO",
                arenaKode = "GRUPPEAMO",
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            ),
            navn = "Gruppe AMO",
            virksomhetsnummer = "123123123",
            startDato = LocalDate.now(),
            sluttDato = null,
            status = TiltaksgjennomforingStatus.GJENNOMFORES,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
        )
        gruppetiltakRepository.upsert(tiltak)

        val deltakelsesdato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
        val amtDeltaker1 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = tiltak.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = AmtDeltakerStatus(
                type = AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = deltakelsesdato,
            ),
            registrertDato = deltakelsesdato,
            endretDato = deltakelsesdato,
            dagerPerUke = 2.5f,
            prosentStilling = null,
        )
        deltakerRepository.upsertKometDeltaker(amtDeltaker1)

        deltakerRepository.getKometHistorikk(listOf(NorskIdent(amtDeltaker1.personIdent))) shouldBe listOf(
            Tiltakshistorikk.GruppetiltakDeltakelse(
                id = amtDeltaker1.id,
                norskIdent = NorskIdent("10101010100"),
                startDato = null,
                sluttDato = null,
                status = AmtDeltakerStatus(
                    type = AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = deltakelsesdato,
                ),
                gjennomforing = Tiltakshistorikk.Gjennomforing(
                    id = tiltak.id,
                    navn = tiltak.navn,
                    tiltakskode = tiltak.tiltakstype.tiltakskode,
                ),
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
            ),
        )
    }
})
