package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.util.UUID

object PrismodellFixtures {
    fun createPrismodellDbo(
        id: UUID = UUID.randomUUID(),
        type: PrismodellType = PrismodellType.ANNEN_AVTALT_PRIS,
        valuta: Valuta = Valuta.NOK,
        prisbetingelser: String? = null,
        satser: List<AvtaltSats> = emptyList(),
    ): PrismodellDbo = PrismodellDbo(
        id = id,
        systemId = null,
        valuta = valuta,
        type = type,
        prisbetingelser = prisbetingelser,
        satser = satser,
    )

    val ForhandsgodkjentAft = PrismodellDbo(
        id = UUID.randomUUID(),
        systemId = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        valuta = Valuta.NOK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.withValuta(Valuta.NOK)),
            AvtaltSats(LocalDate.of(2026, 1, 1), 21_730.withValuta(Valuta.NOK)),
        ),
    )

    val ForhandsgodkjentVta = PrismodellDbo(
        id = UUID.randomUUID(),
        systemId = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        valuta = Valuta.NOK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 16_848.withValuta(Valuta.NOK)),
            AvtaltSats(LocalDate.of(2026, 1, 1), 17_455.withValuta(Valuta.NOK)),
        ),
    )

    val AvtaltPrisPerTimeOppfolging = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.withValuta(Valuta.NOK)),
        ),
    )

    val AvtaltPrisPerManedsverk = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.withValuta(Valuta.NOK)),
        ),
    )

    val AnnenAvtaltPris = createPrismodellDbo()
}
