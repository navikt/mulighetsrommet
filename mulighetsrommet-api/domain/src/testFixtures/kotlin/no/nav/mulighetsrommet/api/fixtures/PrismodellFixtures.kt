package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

object PrismodellFixtures {
    fun createPrismodell(
        id: UUID = UUID.randomUUID(),
        type: PrismodellType,
        valuta: Valuta = Valuta.NOK,
        prisbetingelser: String? = null,
        satser: List<AvtaltSats> = emptyList(),
        tilsagnPerDeltaker: Boolean? = false,
        totalbelop: Int? = null,
        tilskudd: Map<Opplaeringtilskudd.Kode, Int>? = null,
        aarsak: String? = null,
    ): Prismodell = Prismodell.from(
        type = type,
        id = id,
        valuta = valuta,
        prisbetingelser = prisbetingelser,
        satser = satser,
        tilsagnPerDeltaker = tilsagnPerDeltaker,
        totalbelop = totalbelop,
        tilskudd = tilskudd,
        aarsak = aarsak,
    )

    val ForhandsgodkjentAft = Prismodell.ForhandsgodkjentPrisPerManedsverk(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 21_730.NOK),
        ),
    )

    val ForhandsgodkjentVtas = Prismodell.ForhandsgodkjentPrisPerManedsverk(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 16_848.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 17_455.NOK),
        ),
    )

    val ForhandsgodkjentVtao = Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 7_321.NOK),
        ),
    )

    val AvtaltPrisPerTimeOppfolging = Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.NOK),
        ),
    )

    val AvtaltPrisPerManedsverk = Prismodell.AvtaltPrisPerManedsverk(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        prisbetingelser = null,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.NOK),
        ),
    )

    val AnnenAvtaltPris = Prismodell.AnnenAvtaltPris(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        tilsagnPerDeltaker = false,
        prisbetingelser = null,
        totalbelop = null,
    )

    // Maps prismodell IDs to their system IDs (for seeding in test databases)
    val systemIds: Map<UUID, String> = mapOf(
        ForhandsgodkjentAft.id to Tiltakskode.ARBEIDSFORBEREDENDE_TRENING.name,
        ForhandsgodkjentVtas.id to Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name,
        ForhandsgodkjentVtao.id to Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER.name,
    )
}
