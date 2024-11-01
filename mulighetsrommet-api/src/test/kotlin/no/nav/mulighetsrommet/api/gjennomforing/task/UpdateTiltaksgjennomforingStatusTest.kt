package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UpdateTiltaksgjennomforingStatusTest :
    FunSpec({

        val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

        val lastSuccessDate = LocalDate.of(2023, 2, 14)
        val today = LocalDate.of(2023, 2, 16)

        context("oppdater statuser på tiltaksgjennomføringer") {
            val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = UpdateTiltaksgjennomforingStatus(
                mockk(),
                TiltaksgjennomforingRepository(database.db),
                tiltaksgjennomforingKafkaProducer,
            )

            fun TiltaksgjennomforingDbo.toDto(status: TiltaksgjennomforingStatus) = TiltaksgjennomforingEksternV1Dto(
                id = id,
                tiltakstype = TiltakstypeFixtures.Oppfolging.run {
                    TiltaksgjennomforingEksternV1Dto.Tiltakstype(
                        id = id,
                        navn = navn,
                        arenaKode = arenaKode,
                        tiltakskode = tiltakskode!!,
                    )
                },
                navn = navn,
                virksomhetsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer.value,
                startDato = startDato,
                sluttDato = sluttDato,
                status = status,
                oppstart = oppstart,
                tilgjengeligForArrangorFraOgMedDato = null,
            )

            val startdatoInnenforMenAvsluttetStatus = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2023, 2, 15),
                sluttDato = LocalDate.now().plusYears(1),
            )
            val startdatoInnenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2023, 2, 15),
                sluttDato = LocalDate.now().plusYears(1),
            )
            val sluttdatoInnenforMenAvbruttStatus = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                startDato = lastSuccessDate,
                sluttDato = lastSuccessDate,
            )
            val sluttdatoInnenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                startDato = lastSuccessDate,
                sluttDato = lastSuccessDate,
            )
            val datoerUtenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                startDato = lastSuccessDate,
                sluttDato = LocalDate.now().plusYears(1),
            )
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(
                    startdatoInnenforMenAvsluttetStatus,
                    startdatoInnenfor,
                    sluttdatoInnenforMenAvbruttStatus,
                    sluttdatoInnenfor,
                    datoerUtenfor,
                ),
            )

            val gjennomforinger = TiltaksgjennomforingRepository(database.db)

            beforeEach {
                domain.initialize(database.db)
                gjennomforinger.avbryt(
                    startdatoInnenforMenAvsluttetStatus.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                )
                gjennomforinger.avbryt(
                    sluttdatoInnenforMenAvbruttStatus.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                )
            }

            afterEach {
                database.db.truncateAll()
            }

            test("oppdater statuser på kafka på relevante tiltaksgjennomføringer") {
                task.oppdaterTiltaksgjennomforingStatus(today, lastSuccessDate)

                verifyAll {
                    tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(TiltaksgjennomforingStatus.GJENNOMFORES))
                    tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(TiltaksgjennomforingStatus.AVSLUTTET))
                }
            }

            test("avpubliserer når tiltak blir avsluttet på relevante tiltaksgjennomføringer") {
                gjennomforinger.setPublisert(startdatoInnenfor.id, true)
                gjennomforinger.setPublisert(sluttdatoInnenfor.id, true)
                task.oppdaterTiltaksgjennomforingStatus(today, lastSuccessDate)

                verifyAll {
                    tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(TiltaksgjennomforingStatus.GJENNOMFORES))
                    tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(TiltaksgjennomforingStatus.AVSLUTTET))
                }
                gjennomforinger.get(startdatoInnenfor.id)?.publisert shouldBe true
                gjennomforinger.get(sluttdatoInnenfor.id)?.publisert shouldBe false
            }
        }

        context("tiltak i egen regi") {
            val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val task = UpdateTiltaksgjennomforingStatus(
                mockk(),
                TiltaksgjennomforingRepository(database.db),
                tiltaksgjennomforingKafkaProducer,
            )

            val startdatoInnenfor = TiltaksgjennomforingFixtures.IPS1.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2023, 2, 15),
                sluttDato = LocalDate.now().plusYears(1),
            )

            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.IPS),
                avtaler = listOf(AvtaleFixtures.IPS),
                gjennomforinger = listOf(startdatoInnenfor),
            )

            beforeEach {
                domain.initialize(database.db)
            }

            afterEach {
                database.db.truncateAll()
            }

            test("oppdaterer ikke status på kafka") {
                task.oppdaterTiltaksgjennomforingStatus(today, lastSuccessDate)

                verify(exactly = 0) {
                    tiltaksgjennomforingKafkaProducer.publish(any())
                }
            }
        }
    })
