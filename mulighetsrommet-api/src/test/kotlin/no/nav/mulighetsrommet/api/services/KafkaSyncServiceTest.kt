package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class KafkaSyncServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeContainer {
        database.db.truncateAll()
    }

    val lastSuccessDate = LocalDate.of(2023, 2, 14)
    val today = LocalDate.of(2023, 2, 16)

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.now().plusYears(1),
    )

    fun createArenaTiltaksgjennomforing(
        startDato: LocalDate = LocalDate.of(2023, 2, 15),
        sluttDato: LocalDate = LocalDate.now().plusYears(1),
        avslutningsstatus: Avslutningsstatus = Avslutningsstatus.AVSLUTTET,
    ): ArenaTiltaksgjennomforingDbo {
        return ArenaTiltaksgjennomforingDbo(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = "12345",
            arrangorOrganisasjonsnummer = "123456789",
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = "2990",
            avslutningsstatus = avslutningsstatus,
            tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
            antallPlasser = 12,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            opphav = ArenaMigrering.Opphav.ARENA,
            avtaleId = null,
            fremmoteDato = null,
            fremmoteSted = null,
        )
    }

    fun ArenaTiltaksgjennomforingDbo.toDto(tiltaksgjennomforingsstatus: Tiltaksgjennomforingsstatus): TiltaksgjennomforingDto {
        return TiltaksgjennomforingDto(
            id = id,
            tiltakstype = TiltaksgjennomforingDto.Tiltakstype(
                id = tiltakstype.id,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            virksomhetsnummer = arrangorOrganisasjonsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            status = tiltaksgjennomforingsstatus,
            oppstart = oppstart,
        )
    }

    fun TiltakstypeDbo.toDto(tiltakstypestatus: Tiltakstypestatus): TiltakstypeDto {
        return TiltakstypeDto(
            id = id,
            navn = navn,
            arenaKode = tiltakskode,
            registrertIArenaDato = registrertDatoIArena,
            sistEndretIArenaDato = sistEndretDatoIArena,
            fraDato = fraDato,
            tilDato = tilDato,
            rettPaaTiltakspenger = rettPaaTiltakspenger,
            status = tiltakstypestatus,
            sanityId = null,
        )
    }

    context("oppdater statuser på tiltaksgjennomføringer") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val kafkaSyncService =
            KafkaSyncService(
                tiltaksgjennomforingRepository,
                tiltakstypeRepository,
                tiltaksgjennomforingKafkaProducer,
                mockk(),
            )

        val startdatoInnenforMenAvsluttetStatus = createArenaTiltaksgjennomforing()
        val startdatoInnenfor =
            createArenaTiltaksgjennomforing(avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET)
        val sluttdatoInnenforMenAvbruttStatus = createArenaTiltaksgjennomforing(
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.AVBRUTT,
        )
        val sluttdatoInnenfor = createArenaTiltaksgjennomforing(
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        )
        val datoerUtenfor = createArenaTiltaksgjennomforing(
            startDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        )
        val navEnheter = NavEnhetRepository(database.db)

        test("oppdater statuser på kafka på relevante tiltaksgjennomføringer") {
            tiltakstypeRepository.upsert(tiltakstype)
            navEnheter.upsert(NavEnhetFixtures.IT)

            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(startdatoInnenforMenAvsluttetStatus)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(startdatoInnenfor)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(sluttdatoInnenforMenAvbruttStatus)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(sluttdatoInnenfor)
            tiltaksgjennomforingRepository.upsertArenaTiltaksgjennomforing(datoerUtenfor)

            kafkaSyncService.oppdaterTiltaksgjennomforingsstatus(today, lastSuccessDate)

            verifyAll {
                tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(Tiltaksgjennomforingsstatus.GJENNOMFORES))
                tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(Tiltaksgjennomforingsstatus.AVSLUTTET))
            }
        }
    }

    context("oppdater statuser på tiltakstyper") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val kafkaSyncService =
            KafkaSyncService(mockk(), tiltakstypeRepository, mockk(), tiltakstypeKafkaProducer)

        val startdatoInnenfor =
            tiltakstype.copy(id = UUID.randomUUID(), fraDato = LocalDate.of(2023, 2, 15))
        val sluttdatoInnenfor =
            tiltakstype.copy(id = UUID.randomUUID(), fraDato = LocalDate.of(2023, 2, 13), tilDato = lastSuccessDate)

        test("oppdater statuser på kafka på relevante tiltakstyper") {
            tiltakstypeRepository.upsert(tiltakstype)
            tiltakstypeRepository.upsert(startdatoInnenfor)
            tiltakstypeRepository.upsert(sluttdatoInnenfor)

            kafkaSyncService.oppdaterTiltakstypestatus(today, lastSuccessDate)

            verifyAll {
                tiltakstypeKafkaProducer.publish(startdatoInnenfor.toDto(Tiltakstypestatus.Aktiv))
                tiltakstypeKafkaProducer.publish(sluttdatoInnenfor.toDto(Tiltakstypestatus.Avsluttet))
            }
        }
    }
})
