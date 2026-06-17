package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

object PrismodellFixtures {
    fun createPrismodellDbo(
        id: UUID = UUID.randomUUID(),
        systemId: String? = null,
        type: PrismodellType,
        valuta: Valuta = Valuta.NOK,
        prisbetingelser: String? = null,
        satser: List<AvtaltSats> = emptyList(),
        tilsagnPerDeltaker: Boolean? = false,
        totalbelop: Int? = null,
        tilskudd: Map<Opplaeringtilskudd.Kode, Int>? = null,
        aarsak: String? = null,
    ): PrismodellDbo = PrismodellDbo(
        id = id,
        systemId = systemId,
        valuta = valuta,
        type = type,
        prisbetingelser = prisbetingelser,
        satser = satser,
        tilsagnPerDeltaker = tilsagnPerDeltaker,
        totalbelop = totalbelop,
        tilskudd = tilskudd,
        aarsak = aarsak,
    )

    val ForhandsgodkjentAft = createPrismodellDbo(
        systemId = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 20_975.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 21_730.NOK),
        ),
    )

    val ForhandsgodkjentVtas = createPrismodellDbo(
        systemId = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 16_848.NOK),
            AvtaltSats(LocalDate.of(2026, 1, 1), 17_455.NOK),
        ),
    )

    val ForhandsgodkjentVtao = createPrismodellDbo(
        systemId = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_ORDINAER.name,
        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS,
        satser = listOf(
            AvtaltSats(LocalDate.of(2025, 1, 1), 7_321.NOK),
        ),
    )

    val AvtaltPrisPerTimeOppfolging = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.NOK),
        ),
    )

    val AvtaltPrisPerManedsverk = createPrismodellDbo(
        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        satser = listOf(
            AvtaltSats(LocalDate.of(2023, 1, 1), 1234.NOK),
        ),
    )

    val AnnenAvtaltPris = createPrismodellDbo(
        type = PrismodellType.ANNEN_AVTALT_PRIS,
        tilsagnPerDeltaker = false,
    )
}
