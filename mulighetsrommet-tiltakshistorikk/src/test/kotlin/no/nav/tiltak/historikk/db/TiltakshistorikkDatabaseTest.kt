package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.model.Virksomhet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakshistorikkDatabaseTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("Virksomhet") {
        val db = TiltakshistorikkDatabase(database.db)

        test("oppretter, henter og sletter virksomhet") {
            val virksomhetForetak = Virksomhet(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = "Virksomhet Foretak",
                organisasjonsform = "AS",
                slettetDato = null,
            )

            val virksomhetAvdeling = Virksomhet(
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
            val virksomhet = Virksomhet(
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
        val arenaArbeidstrening = TestFixtures.ArenaGjennomforing.Arbeidstrening
        val arbeidstreningArenaDeltakelse = TiltakshistorikkArenaDeltaker(
            id = UUID.randomUUID(),
            arenaGjennomforingId = arenaArbeidstrening.id,
            arenaDeltakerId = 1,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2024, 1, 31),
            arenaRegDato = Instant.parse("2023-12-31T23:00:00Z"),
            arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
            dagerPerUke = 2.5,
            deltidsprosent = 50.0,
        )

        val arenaMentor = TestFixtures.ArenaGjennomforing.Mentor
        val mentorArenaDeltakelse = TiltakshistorikkArenaDeltaker(
            id = UUID.randomUUID(),
            arenaGjennomforingId = arenaMentor.id,
            arenaDeltakerId = 2,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDate.of(2002, 2, 1),
            sluttDato = LocalDate.of(2002, 2, 1),
            arenaRegDato = Instant.parse("2001-12-31T23:00:00Z"),
            arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
            dagerPerUke = 5.0,
            deltidsprosent = 100.0,
        )

        var hovedenhet = TestFixtures.Virksomhet.ArrangorHovedenhet
        var underenhet = TestFixtures.Virksomhet.Arrangor.copy(
            overordnetEnhetOrganisasjonsnummer = hovedenhet.organisasjonsnummer,
        )

        var db = TiltakshistorikkDatabase(database.db)

        beforeAny {
            db.session {
                queries.virksomhet.upsert(hovedenhet)
                queries.virksomhet.upsert(underenhet)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Mentor)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Arbeidstrening)
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
                ) shouldContainExactlyInAnyOrder listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = mentorArenaDeltakelse.id,
                        arenaId = 2,
                        norskIdent = NorskIdent("12345678910"),
                        startDato = LocalDate.of(2002, 2, 1),
                        sluttDato = LocalDate.of(2002, 2, 1),
                        opprettetTidspunkt = Instant.parse("2001-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
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
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(
                                hovedenhet.organisasjonsnummer,
                                hovedenhet.navn,
                            ),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(
                                underenhet.organisasjonsnummer,
                                underenhet.navn,
                            ),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = arbeidstreningArenaDeltakelse.id,
                        arenaId = 1,
                        norskIdent = NorskIdent("12345678910"),
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        opprettetTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
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
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(
                                hovedenhet.organisasjonsnummer,
                                hovedenhet.navn,
                            ),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(
                                underenhet.organisasjonsnummer,
                                underenhet.navn,
                            ),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )

                queries.arenaDeltaker.deleteArenaDeltaker(mentorArenaDeltakelse.id)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("12345678910")),
                ) shouldBe listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        id = arbeidstreningArenaDeltakelse.id,
                        arenaId = 1,
                        norskIdent = NorskIdent("12345678910"),
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        opprettetTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
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
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(
                                hovedenhet.organisasjonsnummer,
                                hovedenhet.navn,
                            ),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(
                                underenhet.organisasjonsnummer,
                                underenhet.navn,
                            ),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )

                queries.arenaDeltaker.deleteArenaDeltaker(arbeidstreningArenaDeltakelse.id)

                queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("12345678910")),
                ).shouldBeEmpty()
            }
        }

        test("henter Arena-historikk for flere identiteter") {
            db.transaction {
                val deltaker1 = TiltakshistorikkArenaDeltaker(
                    id = UUID.randomUUID(),
                    arenaGjennomforingId = arenaArbeidstrening.id,
                    arenaDeltakerId = 4,
                    norskIdent = NorskIdent("11111111111"),
                    status = ArenaDeltakerStatus.GJENNOMFORES,
                    startDato = LocalDate.of(2020, 1, 1),
                    sluttDato = LocalDate.of(2020, 6, 1),
                    arenaRegDato = Instant.parse("2019-12-31T23:00:00Z"),
                    arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
                    dagerPerUke = null,
                    deltidsprosent = null,
                )

                val deltaker2 = TiltakshistorikkArenaDeltaker(
                    id = UUID.randomUUID(),
                    arenaGjennomforingId = arenaMentor.id,
                    arenaDeltakerId = 5,
                    norskIdent = NorskIdent("22222222222"),
                    status = ArenaDeltakerStatus.DELTAKELSE_AVBRUTT,
                    startDato = LocalDate.of(2021, 1, 1),
                    sluttDato = LocalDate.of(2021, 3, 1),
                    arenaRegDato = Instant.parse("2020-12-31T23:00:00Z"),
                    arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
                    dagerPerUke = null,
                    deltidsprosent = null,
                )

                queries.arenaDeltaker.upsertArenaDeltaker(deltaker1)
                queries.arenaDeltaker.upsertArenaDeltaker(deltaker2)

                val historikk = queries.arenaDeltaker.getArenaHistorikk(
                    identer = listOf(NorskIdent("11111111111"), NorskIdent("22222222222")),
                )

                historikk.map { it.id } shouldContainExactlyInAnyOrder listOf(deltaker1.id, deltaker2.id)
            }
        }
    }

    context("Komet deltaker") {
        val gruppeAmo = TestFixtures.Gjennomforing.GruppeAmo
        val kometDeltaker = TestFixtures.Deltaker.GruppeAmo

        val db = TiltakshistorikkDatabase(database.db)

        var hovedenhet = TestFixtures.Virksomhet.ArrangorHovedenhet
        var underenhet = TestFixtures.Virksomhet.Arrangor.copy(
            overordnetEnhetOrganisasjonsnummer = hovedenhet.organisasjonsnummer,
        )

        beforeAny {
            db.session {
                queries.virksomhet.upsert(hovedenhet)
                queries.virksomhet.upsert(underenhet)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.GruppeAmo)
                queries.gjennomforing.upsert(gruppeAmo)
            }
        }

        test("oppretter og henter Komet-deltaker med tilhørende gjennomføring") {
            db.transaction {
                queries.kometDeltaker.upsertKometDeltaker(kometDeltaker)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(kometDeltaker.personIdent)),
                ) shouldBe listOf(
                    TiltakshistorikkV1Dto.TeamKometDeltakelse(
                        id = kometDeltaker.id,
                        norskIdent = NorskIdent("10101010100"),
                        startDato = null,
                        sluttDato = null,
                        opprettetTidspunkt = Instant.parse("2021-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2021-12-31T23:00:00Z"),
                        tittel = "Arbeidsmarkedsopplæring (gruppe) hos Arrangør Foretak",
                        status = TiltakshistorikkV1Dto.TeamKometDeltakelse.Status(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
                        ),
                        tiltakstype = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
                            tiltakskode = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                            navn = "Arbeidsmarkedsopplæring (gruppe)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = gruppeAmo.id,
                            navn = gruppeAmo.navn,
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(
                                hovedenhet.organisasjonsnummer,
                                hovedenhet.navn,
                            ),
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(
                                underenhet.organisasjonsnummer,
                                underenhet.navn,
                            ),
                        ),
                        deltidsprosent = 50f,
                        dagerPerUke = 2.5f,
                    ),
                )
            }
        }

        test("sletter Komet-deltaker") {
            db.transaction {
                val testDeltaker = kometDeltaker.copy(
                    id = UUID.randomUUID(),
                    gjennomforingId = gruppeAmo.id,
                    personIdent = "99999999999",
                    startDato = null,
                    sluttDato = null,
                    prosentStilling = null,
                    dagerPerUke = null,
                )

                queries.kometDeltaker.upsertKometDeltaker(testDeltaker)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(testDeltaker.personIdent)),
                ).map { it.id } shouldBe listOf(testDeltaker.id)

                queries.kometDeltaker.deleteKometDeltaker(testDeltaker.id)

                queries.kometDeltaker.getKometHistorikk(
                    identer = listOf(NorskIdent(testDeltaker.personIdent)),
                ).shouldBeEmpty()
            }
        }
    }
})
