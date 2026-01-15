package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.ValutaType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

object PrismodellFixtures {
    fun createPrismodellDbo(
        id: UUID = UUID.randomUUID(),
        type: PrismodellType = PrismodellType.ANNEN_AVTALT_PRIS,
        prisbetingelser: String? = null,
        satser: List<AvtaltSats> = emptyList(),
    ): PrismodellDbo = PrismodellDbo(
        id = id,
        systemId = null,
        type = type,
        prisbetingelser = prisbetingelser,
        satser = satser,
    )

    val ForhandsgodkjentAft = PrismodellDbo(
        id = UUID.randomUUID(),
        systemId = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 20_975, ValutaType.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 21_730, ValutaType.NOK),
        ),
    )

    val ForhandsgodkjentVta = PrismodellDbo(
        id = UUID.randomUUID(),
        systemId = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 16_848, ValutaType.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 17_455, ValutaType.NOK),
        ),
    )

    val AvtaltPrisPerTimeOppfolging = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        satser = listOf(AvtaltSats(LocalDate.of(2023, 1, 1), 1234, ValutaType.NOK)),
    )

    val AnnenAvtaltPris = createPrismodellDbo()
}
