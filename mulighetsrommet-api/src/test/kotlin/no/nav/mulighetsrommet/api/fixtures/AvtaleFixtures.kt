package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleFixtures(private val database: FlywayDatabaseTestListener) {
    val tiltakstypeId: UUID = UUID.fromString("0c565576-6a74-4bc2-ad5a-765580014ef9")

    fun runBeforeTests() {
        database.db.clean()
        database.db.migrate()

        val tiltakstypeRepository = TiltakstypeRepository(database.db)

        tiltakstypeRepository.upsert(
            TiltakstypeDbo(
                tiltakstypeId,
                "",
                "",
                rettPaaTiltakspenger = true,
                registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                fraDato = LocalDate.of(2023, 1, 11),
                tilDato = LocalDate.of(2023, 1, 12),
            ),
        ).getOrThrow()
    }

    fun upsertTiltakstype(tiltakstyper: List<TiltakstypeDbo>) {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        tiltakstyper.forEach {
            tiltakstypeRepository.upsert(it)
        }
    }

    fun upsertAvtaler(avtaler: List<AvtaleDbo>): AvtaleRepository {
        val avtaleRepository = AvtaleRepository(database.db)

        avtaler.forEachIndexed { index, avtaleDbo ->
            val avtale = avtaleDbo.copy(avtalenummer = "2023#${index + 1}-${UUID.randomUUID()}")
            avtaleRepository.upsert(avtale).getOrThrow()
        }

        return avtaleRepository
    }

    fun createAvtaleForTiltakstype(
        tiltakstypeId: UUID = this.tiltakstypeId,
        navn: String = "Avtalenavn",
        avtalenummer: String = "2023#1",
        navRegion: String? = null,
        avtaletype: Avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus: Avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        startDato: LocalDate = LocalDate.of(2023, 1, 11),
        sluttDato: LocalDate = LocalDate.of(2023, 2, 28),
        ansvarlige: List<String> = emptyList(),
        navEnheter: List<String> = emptyList(),
        leverandorUnderenheter: List<String> = emptyList(),
    ): AvtaleDbo {
        return AvtaleDbo(
            id = UUID.randomUUID(),
            navn = navn,
            avtalenummer = avtalenummer,
            tiltakstypeId = tiltakstypeId,
            leverandorOrganisasjonsnummer = "12345678910",
            leverandorUnderenheter = leverandorUnderenheter,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = null,
            navRegion = navRegion,
            avtaletype = avtaletype,
            avslutningsstatus = avslutningsstatus,
            prisbetingelser = null,
            opphav = AvtaleDbo.Opphav.MR_ADMIN_FLATE,
            ansvarlige = ansvarlige,
            navEnheter = navEnheter,
        )
    }
}
