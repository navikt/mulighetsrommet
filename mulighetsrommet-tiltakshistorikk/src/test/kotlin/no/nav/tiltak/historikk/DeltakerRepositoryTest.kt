package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.arena.ArenaDeltakerDbo
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.db.DeltakerQueries
import no.nav.tiltak.historikk.db.GruppetiltakQueries
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val gruppeAmo = TiltaksgjennomforingEksternV1Dto(
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
        status = GjennomforingStatus.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
        apentForPamelding = true,
        antallPlasser = 10,
        opprettetTidspunkt = LocalDateTime.now(),
        oppdatertTidspunkt = LocalDateTime.now(),
    )
    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.randomUUID(),
        gjennomforingId = gruppeAmo.id,
        personIdent = "10101010100",
        startDato = null,
        sluttDato = null,
        status = DeltakerStatus(
            type = DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        ),
        registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        endretDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        dagerPerUke = 2.5f,
        prosentStilling = null,
        deltakelsesmengder = listOf(),
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
        database.runAndRollback { session ->
            val deltaker = DeltakerQueries(session)

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

    test("kometHistorikk") {
        database.runAndRollback { session ->
            val gruppetiltak = GruppetiltakQueries(session)
            gruppetiltak.upsert(gruppeAmo)

            val deltaker = DeltakerQueries(session)
            deltaker.upsertKometDeltaker(amtDeltaker)

            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null) shouldBe listOf(
                Tiltakshistorikk.GruppetiltakDeltakelse(
                    id = amtDeltaker.id,
                    norskIdent = NorskIdent("10101010100"),
                    startDato = null,
                    sluttDato = null,
                    status = DeltakerStatus(
                        type = DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
                        aarsak = null,
                        opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                    ),
                    gjennomforing = Tiltakshistorikk.Gjennomforing(
                        id = gruppeAmo.id,
                        navn = gruppeAmo.navn,
                        tiltakskode = gruppeAmo.tiltakstype.tiltakskode,
                    ),
                    arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                ),
            )
        }
    }

    test("maxAgeYears komet") {
        database.runAndRollback { session ->
            val gruppetiltak = GruppetiltakQueries(session)
            gruppetiltak.upsert(gruppeAmo)

            val deltaker = DeltakerQueries(session)

            val amtDeltakerReg2005 = AmtDeltakerV1Dto(
                id = UUID.randomUUID(),
                gjennomforingId = gruppeAmo.id,
                personIdent = "10101010100",
                startDato = null,
                sluttDato = null,
                status = DeltakerStatus(
                    type = DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                ),
                registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                dagerPerUke = 2.5f,
                prosentStilling = null,
                deltakelsesmengder = listOf(),
            )
            val amtDeltakerReg2005Slutt2024 = AmtDeltakerV1Dto(
                id = UUID.randomUUID(),
                gjennomforingId = gruppeAmo.id,
                personIdent = "10101010100",
                startDato = null,
                sluttDato = LocalDate.of(2024, 1, 1),
                status = DeltakerStatus(
                    type = DeltakerStatus.DeltakerStatusType.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                ),
                registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                dagerPerUke = 2.5f,
                prosentStilling = null,
                deltakelsesmengder = listOf(),
            )
            deltaker.upsertKometDeltaker(amtDeltaker)
            deltaker.upsertKometDeltaker(amtDeltakerReg2005)
            deltaker.upsertKometDeltaker(amtDeltakerReg2005Slutt2024)

            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(
                amtDeltaker.id,
                amtDeltakerReg2005.id,
                amtDeltakerReg2005Slutt2024.id,
            )
            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), 5)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(
                amtDeltaker.id,
                amtDeltakerReg2005Slutt2024.id,
            )
        }
    }

    test("maxAgeYears arena") {
        database.runAndRollback { session ->
            val deltaker = DeltakerQueries(session)

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
