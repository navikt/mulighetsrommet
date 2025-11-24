package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import no.nav.tiltak.historikk.kafka.consumers.toGjennomforingDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkDatabaseTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("Virksomhet") {
        val db = TiltakshistorikkDatabase(database.db)

        test("oppretter, henter og sletter virksomhet") {
            val virksomhetForetak = VirksomhetDbo(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = "Virksomhet Foretak",
                organisasjonsform = "AS",
                slettetDato = null,
            )

            val virksomhetAvdeling = VirksomhetDbo(
                organisasjonsnummer = Organisasjonsnummer("876543210"),
                overordnetEnhetOrganisasjonsnummer = Organisasjonsnummer("987654321"),
                navn = "Virksomhet Avdeling",
                organisasjonsform = "BEDR",
                slettetDato = null,
            )

            db.session {
                queries.virksomhet.upsert(virksomhetForetak)
                queries.virksomhet.upsert(virksomhetAvdeling)

                val hentet = queries.virksomhet.get(virksomhetAvdeling.organisasjonsnummer)
                hentet shouldBe virksomhetAvdeling

                queries.virksomhet.delete(virksomhetAvdeling.organisasjonsnummer)

                val slettet = queries.virksomhet.get(virksomhetAvdeling.organisasjonsnummer)
                slettet.shouldBeNull()
            }
        }

        test("oppdaterer eksisterende virksomhet ved upsert") {
            val virksomhet = VirksomhetDbo(
                organisasjonsnummer = Organisasjonsnummer("888999777"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = "Original Navn",
                organisasjonsform = "AS",
                slettetDato = null,
            )

            db.session {
                queries.virksomhet.upsert(virksomhet)

                val updated = virksomhet.copy(navn = "Oppdatert Navn", organisasjonsform = "ASA")
                queries.virksomhet.upsert(updated)

                val hentet = queries.virksomhet.get(virksomhet.organisasjonsnummer)
                hentet shouldBe updated
            }
        }
    }

    context("Arena deltaker") {
        val arenaArbeidstrening = TestFixtures.Gjennomforing.arenaArbeidstrening
        val arbeidstreningArenaDeltakelse = TiltakshistorikkArenaDeltaker(
            id = UUID.randomUUID(),
            arenaGjennomforingId = arenaArbeidstrening.id,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            sluttDato = LocalDateTime.of(2024, 1, 31, 0, 0, 0),
            arenaRegDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            dagerPerUke = 2.5,
            deltidsprosent = 50.0,
        )

        val arenaMentor = TestFixtures.Gjennomforing.arenaMentor
        val mentorArenaDeltakelse = TiltakshistorikkArenaDeltaker(
            id = UUID.randomUUID(),
            arenaGjennomforingId = arenaMentor.id,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
            sluttDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
            arenaRegDato = LocalDateTime.of(2002, 1, 1, 0, 0, 0),
            arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            dagerPerUke = 5.0,
            deltidsprosent = 100.0,
        )

        var hovedenhet = TestFixtures.Virksomhet.arrangorHovedenhet
        var underenhet = TestFixtures.Virksomhet.arrangor.copy(
            overordnetEnhetOrganisasjonsnummer = hovedenhet.organisasjonsnummer,
        )

        var db = TiltakshistorikkDatabase(database.db)

        beforeAny {
            db.session {
                queries.virksomhet.upsert(hovedenhet)
                queries.virksomhet.upsert(underenhet)
                queries.arenaGjennomforing.upsert(arenaArbeidstrening)
                queries.arenaGjennomforing.upsert(arenaMentor)
            }
        }

        test("oppretter, henter og sletter Arena-deltakere") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
                queries.arenaDeltaker.upsertArenaDeltaker(mentorArenaDeltakelse)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("12345678910")),
                    maxAgeYears = null,
                ) shouldContainExactlyInAnyOrder listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = mentorArenaDeltakelse.id,
                        norskIdent = NorskIdent("12345678910"),
                        startDato = LocalDate.of(2002, 2, 1),
                        sluttDato = LocalDate.of(2002, 2, 1),
                        tittel = "Mentor hos Arrangør",
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "MENTOR",
                            navn = "Mentor",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = arenaMentor.id,
                            navn = "Mentortiltak hos Joblearn",
                            deltidsprosent = 100f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(hovedenhet.organisasjonsnummer, hovedenhet.navn),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(underenhet.organisasjonsnummer, underenhet.navn),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = arbeidstreningArenaDeltakelse.id,
                        norskIdent = NorskIdent("12345678910"),
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        tittel = "Arbeidstrening hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "ARBTREN",
                            navn = "Arbeidstrening",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = arenaArbeidstrening.id,
                            navn = "Arbeidstrening hos Fretex",
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(hovedenhet.organisasjonsnummer, hovedenhet.navn),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(underenhet.organisasjonsnummer, underenhet.navn),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )

                queries.arenaDeltaker.deleteArenaDeltaker(mentorArenaDeltakelse.id)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("12345678910")),
                    maxAgeYears = null,
                ) shouldBe listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = arbeidstreningArenaDeltakelse.id,
                        norskIdent = NorskIdent("12345678910"),
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        tittel = "Arbeidstrening hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "ARBTREN",
                            navn = "Arbeidstrening",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = arenaArbeidstrening.id,
                            navn = "Arbeidstrening hos Fretex",
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(hovedenhet.organisasjonsnummer, hovedenhet.navn),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(underenhet.organisasjonsnummer, underenhet.navn),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )

                queries.arenaDeltaker.deleteArenaDeltaker(arbeidstreningArenaDeltakelse.id)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("12345678910")),
                    maxAgeYears = null,
                ).shouldBeEmpty()
            }
        }

        test("filtrerer Arena-deltakere basert på maxAgeYears") {
            val mentorArenaDeltakelseUtenSlutt = TiltakshistorikkArenaDeltaker(
                id = UUID.randomUUID(),
                arenaGjennomforingId = arenaMentor.id,
                norskIdent = NorskIdent("12345678910"),
                status = ArenaDeltakerStatus.GJENNOMFORES,
                startDato = LocalDateTime.of(2002, 2, 1, 0, 0, 0),
                sluttDato = null,
                arenaRegDato = LocalDateTime.of(2002, 1, 1, 0, 0, 0),
                arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                dagerPerUke = 5.0,
                deltidsprosent = 100.0,
            )

            db.transaction {
                queries.arenaDeltaker.upsertArenaDeltaker(arbeidstreningArenaDeltakelse)
                queries.arenaDeltaker.upsertArenaDeltaker(mentorArenaDeltakelse)
                queries.arenaDeltaker.upsertArenaDeltaker(mentorArenaDeltakelseUtenSlutt)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(arbeidstreningArenaDeltakelse.norskIdent),
                    maxAgeYears = 5,
                ).map { it.id } shouldContainExactlyInAnyOrder listOf(arbeidstreningArenaDeltakelse.id)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(arbeidstreningArenaDeltakelse.norskIdent),
                    maxAgeYears = null,
                ).map { it.id } shouldContainExactlyInAnyOrder listOf(
                    arbeidstreningArenaDeltakelse.id,
                    mentorArenaDeltakelse.id,
                    mentorArenaDeltakelseUtenSlutt.id,
                )
            }
        }

        test("henter Arena-historikk for flere identiteter") {
            db.transaction {
                val deltaker1 = TiltakshistorikkArenaDeltaker(
                    id = UUID.randomUUID(),
                    arenaGjennomforingId = arenaArbeidstrening.id,
                    norskIdent = NorskIdent("11111111111"),
                    status = ArenaDeltakerStatus.GJENNOMFORES,
                    startDato = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                    sluttDato = LocalDateTime.of(2020, 6, 1, 0, 0, 0),
                    arenaRegDato = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                    arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                    dagerPerUke = null,
                    deltidsprosent = null,
                )

                val deltaker2 = TiltakshistorikkArenaDeltaker(
                    id = UUID.randomUUID(),
                    arenaGjennomforingId = arenaMentor.id,
                    norskIdent = NorskIdent("22222222222"),
                    status = ArenaDeltakerStatus.DELTAKELSE_AVBRUTT,
                    startDato = LocalDateTime.of(2021, 1, 1, 0, 0, 0),
                    sluttDato = LocalDateTime.of(2021, 3, 1, 0, 0, 0),
                    arenaRegDato = LocalDateTime.of(2021, 1, 1, 0, 0, 0),
                    arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                    dagerPerUke = null,
                    deltidsprosent = null,
                )

                queries.arenaDeltaker.upsertArenaDeltaker(deltaker1)
                queries.arenaDeltaker.upsertArenaDeltaker(deltaker2)

                val historikk = queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("11111111111"), NorskIdent("22222222222")),
                    maxAgeYears = null,
                )

                historikk.map { it.id } shouldContainExactlyInAnyOrder listOf(deltaker1.id, deltaker2.id)
            }
        }
    }

    context("Komet deltaker") {
        val gruppeAmo = TestFixtures.Gjennomforing.gruppeAmo
        val amtDeltaker = TestFixtures.Deltaker.gruppeAmo

        val db = TiltakshistorikkDatabase(database.db)

        var hovedenhet = TestFixtures.Virksomhet.arrangorHovedenhet
        var underenhet = TestFixtures.Virksomhet.arrangor.copy(
            overordnetEnhetOrganisasjonsnummer = hovedenhet.organisasjonsnummer,
        )

        beforeAny {
            db.session {
                queries.virksomhet.upsert(hovedenhet)
                queries.virksomhet.upsert(underenhet)
                queries.gjennomforing.upsert(gruppeAmo.toGjennomforingDbo())
            }
        }

        test("oppretter og henter Komet-deltaker med tilhørende gjennomføring") {
            db.transaction {
                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(amtDeltaker.personIdent)),
                    maxAgeYears = null,
                ) shouldBe listOf(
                    TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
                        id = amtDeltaker.id,
                        norskIdent = NorskIdent("10101010100"),
                        startDato = null,
                        sluttDato = null,
                        tittel = "Arbeidsmarkedsopplæring (gruppe) hos Arrangør Foretak",
                        status = DeltakerStatus(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                        ),
                        tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
                            tiltakskode = gruppeAmo.tiltakskode,
                            navn = "Arbeidsmarkedsopplæring (gruppe)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = gruppeAmo.id,
                            navn = gruppeAmo.navn,
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(hovedenhet.organisasjonsnummer, hovedenhet.navn),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(underenhet.organisasjonsnummer, underenhet.navn),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )
            }
        }

        test("filtrerer Komet-deltakere basert på maxAgeYears") {
            db.transaction {
                val amtDeltakerReg2005 = AmtDeltakerV1Dto(
                    id = UUID.randomUUID(),
                    gjennomforingId = gruppeAmo.id,
                    personIdent = "10101010100",
                    startDato = null,
                    sluttDato = null,
                    status = DeltakerStatus(
                        type = DeltakerStatusType.VENTER_PA_OPPSTART,
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
                        type = DeltakerStatusType.VENTER_PA_OPPSTART,
                        aarsak = null,
                        opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                    ),
                    registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                    endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                    dagerPerUke = 2.5f,
                    prosentStilling = null,
                    deltakelsesmengder = listOf(),
                )

                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker)
                queries.kometDeltaker.upsertKometDeltaker(amtDeltakerReg2005)
                queries.kometDeltaker.upsertKometDeltaker(amtDeltakerReg2005Slutt2024)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(amtDeltaker.personIdent)),
                    maxAgeYears = null,
                ).map { it.id } shouldContainExactlyInAnyOrder listOf(
                    amtDeltaker.id,
                    amtDeltakerReg2005.id,
                    amtDeltakerReg2005Slutt2024.id,
                )

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(amtDeltaker.personIdent)),
                    maxAgeYears = 5,
                ).map { it.id } shouldContainExactlyInAnyOrder listOf(
                    amtDeltaker.id,
                    amtDeltakerReg2005Slutt2024.id,
                )
            }
        }

        test("sletter Komet-deltaker") {
            db.transaction {
                val testDeltaker = AmtDeltakerV1Dto(
                    id = UUID.randomUUID(),
                    gjennomforingId = gruppeAmo.id,
                    personIdent = "99999999999",
                    startDato = null,
                    sluttDato = null,
                    status = DeltakerStatus(
                        type = DeltakerStatusType.VENTER_PA_OPPSTART,
                        aarsak = null,
                        opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                    ),
                    registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                    endretDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                    dagerPerUke = 2.5f,
                    prosentStilling = null,
                    deltakelsesmengder = listOf(),
                )

                queries.kometDeltaker.upsertKometDeltaker(testDeltaker)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(testDeltaker.personIdent)),
                    maxAgeYears = null,
                ).map { it.id } shouldBe listOf(testDeltaker.id)

                queries.kometDeltaker.deleteKometDeltaker(testDeltaker.id)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(testDeltaker.personIdent)),
                    maxAgeYears = null,
                ).shouldBeEmpty()
            }
        }
    }
})
