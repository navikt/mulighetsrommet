package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.ValutaType
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
        type = type,
        prisbetingelser = prisbetingelser,
        satser = satser,
    )

    val ForhandsgodkjentAft = PrismodellDbo(
        id = UUID.randomUUID(),
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(
                gjelderFra = LocalDate.of(2025, 1, 1),
                sats = 20_975,
            ),
            AvtaltSats(
                gjelderFra = LocalDate.of(2026, 1, 1),
                sats = 21_730,
            ),
        ),
    )

    val ForhandsgodkjentVta = PrismodellDbo(
        id = UUID.randomUUID(),
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(
                gjelderFra = LocalDate.of(2025, 1, 1),
                sats = 16_848,
            ),
            AvtaltSats(
                gjelderFra = LocalDate.of(2026, 1, 1),
                sats = 17_455,
            ),
        ),
    )

    val AvtaltPrisPerTimeOppfolging = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        satser = listOf(AvtaltSats(gjelderFra = LocalDate.of(2023, 1, 1), sats = 1234, ValutaType.NOK)),
    )

    val AnnenAvtaltPris = createPrismodellDbo()
}
