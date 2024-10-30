package no.nav.mulighetsrommet.tiltakshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.dto.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val gruppetiltak = TiltaksgjennomforingEksternV1Dto(
        id = UUID.randomUUID(),
        tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
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
    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.randomUUID(),
        gjennomforingId = gruppetiltak.id,
        personIdent = "10101010100",
        startDato = null,
        sluttDato = null,
        status = DeltakerStatus(
            type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        ),
        registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        endretDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        dagerPerUke = 2.5f,
        prosentStilling = null,
    )
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
        val deltakerRepository = DeltakerRepository(database.db)

        deltakerRepository.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
        deltakerRepository.upsertArenaDeltaker(mentorArenaDeltakelse)

        deltakerRepository.getArenaHistorikk(identer = listOf(NorskIdent("12345678910")), null) shouldContainExactlyInAnyOrder listOf(
            Tiltakshistorikk.ArenaDeltakelse(
                id = mentorArenaDeltakelse.id,
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "MENTOR",
                startDato = LocalDate.of(2002, 2, 1),
                sluttDato = LocalDate.of(2002, 2, 1),
                status = ArenaDeltakerStatus.GJENNOMFORES,
                beskrivelse = "Mentortiltak hos Joblearn",
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                registrertTidspunkt = LocalDateTime.of(2002, 1, 1, 0, 0, 0),
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
                registrertTidspunkt = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            ),
        )

        deltakerRepository.deleteArenaDeltaker(mentorArenaDeltakelse.id)

        deltakerRepository.getArenaHistorikk(identer = listOf(NorskIdent("12345678910")), null) shouldBe listOf(
            Tiltakshistorikk.ArenaDeltakelse(
                id = arbeidstreningArenaDeltakelse.id,
                norskIdent = NorskIdent("12345678910"),
                arenaTiltakskode = "ARBTREN",
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 31),
                beskrivelse = "Arbeidstrening hos Fretex",
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                registrertTidspunkt = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            ),
        )

        deltakerRepository.deleteArenaDeltaker(arbeidstreningArenaDeltakelse.id)

        deltakerRepository.getArenaHistorikk(identer = listOf(NorskIdent("12345678910")), null).shouldBeEmpty()
    }

    test("kometHistorikk") {
        val deltakerRepository = DeltakerRepository(database.db)
        val gruppetiltakRepository = GruppetiltakRepository(database.db)

        gruppetiltakRepository.upsert(gruppetiltak)

        deltakerRepository.upsertKometDeltaker(amtDeltaker)

        deltakerRepository.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null) shouldBe listOf(
            Tiltakshistorikk.GruppetiltakDeltakelse(
                id = amtDeltaker.id,
                norskIdent = NorskIdent("10101010100"),
                startDato = null,
                sluttDato = null,
                status = DeltakerStatus(
                    type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                ),
                gjennomforing = Tiltakshistorikk.Gjennomforing(
                    id = gruppetiltak.id,
                    navn = gruppetiltak.navn,
                    tiltakskode = gruppetiltak.tiltakstype.tiltakskode,
                ),
                arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                registrertTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            ),
        )
    }

    test("maxAgeYears komet") {
        val deltakerRepository = DeltakerRepository(database.db)
        val gruppetiltakRepository = GruppetiltakRepository(database.db)

        gruppetiltakRepository.upsert(gruppetiltak)

        val amtDeltakerReg2005 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = gruppetiltak.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = DeltakerStatus(
                type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            ),
            registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            dagerPerUke = 2.5f,
            prosentStilling = null,
        )
        val amtDeltakerReg2005Slutt2024 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = gruppetiltak.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = LocalDate.of(2024, 1, 1),
            status = DeltakerStatus(
                type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            ),
            registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
            dagerPerUke = 2.5f,
            prosentStilling = null,
        )
        deltakerRepository.upsertKometDeltaker(amtDeltaker)
        deltakerRepository.upsertKometDeltaker(amtDeltakerReg2005)
        deltakerRepository.upsertKometDeltaker(amtDeltakerReg2005Slutt2024)

        deltakerRepository.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null)
            .map { it.id } shouldContainExactlyInAnyOrder listOf(
            amtDeltaker.id,
            amtDeltakerReg2005.id,
            amtDeltakerReg2005Slutt2024.id,
        )
        deltakerRepository.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), 5)
            .map { it.id } shouldContainExactlyInAnyOrder listOf(
            amtDeltaker.id,
            amtDeltakerReg2005Slutt2024.id,
        )
    }

    test("maxAgeYears arena") {
        val deltakerRepository = DeltakerRepository(database.db)
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

        deltakerRepository.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
        deltakerRepository.upsertArenaDeltaker(mentorArenaDeltakelse)
        deltakerRepository.upsertArenaDeltaker(mentorArenaDeltakelseUtenSlutt)

        deltakerRepository.getArenaHistorikk(listOf(arbeidstreningArenaDeltakelse.norskIdent), 5)
            .map { it.id } shouldContainExactlyInAnyOrder listOf(arbeidstreningArenaDeltakelse.id)

        deltakerRepository.getArenaHistorikk(listOf(arbeidstreningArenaDeltakelse.norskIdent), null)
            .map { it.id } shouldContainExactlyInAnyOrder listOf(
            arbeidstreningArenaDeltakelse.id,
            mentorArenaDeltakelse.id,
            mentorArenaDeltakelseUtenSlutt.id,
        )
    }
})
