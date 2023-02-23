package no.nav.mulighetsrommet.arena.adapter.fixtures

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.arena.adapter.models.arena.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

fun createArenaAvtaleInfoEvent(
    operation: ArenaEvent.Operation,
    avtale: ArenaAvtaleInfo = AvtaleFixtures.ArenaAvtaleInfo,
    modify: (avtale: ArenaAvtaleInfo) -> ArenaAvtaleInfo = { it }
): ArenaEvent = modify(avtale).let {
    createArenaEvent(
        ArenaTable.AvtaleInfo,
        it.AVTALE_ID.toString(),
        operation,
        Json.encodeToJsonElement(it).toString()
    )
}

fun createArenaSakEvent(
    operation: ArenaEvent.Operation,
    sak: ArenaSak = SakFixtures.ArenaTiltakSak,
    modify: (sak: ArenaSak) -> ArenaSak = { it }
): ArenaEvent = modify(sak).let {
    createArenaEvent(
        ArenaTable.Sak,
        it.SAK_ID.toString(),
        operation,
        Json.encodeToJsonElement(it).toString()
    )
}

fun createArenaTiltakdeltakerEvent(
    operation: ArenaEvent.Operation,
    deltaker: ArenaTiltakdeltaker = DeltakerFixtures.ArenaTiltakdeltaker,
    modify: (deltaker: ArenaTiltakdeltaker) -> ArenaTiltakdeltaker = { it }
): ArenaEvent = modify(deltaker).let {
    createArenaEvent(
        ArenaTable.Deltaker,
        it.TILTAKDELTAKER_ID.toString(),
        operation,
        Json.encodeToJsonElement(it).toString()
    )
}

fun createArenaTiltakEvent(
    operation: ArenaEvent.Operation,
    tiltak: ArenaTiltak = TiltakstypeFixtures.ArenaGruppetiltak,
    modify: (tiltak: ArenaTiltak) -> ArenaTiltak = { it }
): ArenaEvent = modify(tiltak).let {
    createArenaEvent(
        ArenaTable.Tiltakstype,
        it.TILTAKSKODE,
        operation,
        Json.encodeToJsonElement(it).toString()
    )
}

fun createArenaTiltakgjennomforingEvent(
    operation: ArenaEvent.Operation,
    tiltaksgjennomforing: ArenaTiltaksgjennomforing = TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingGruppe,
    status: ArenaEvent.ProcessingStatus = ArenaEvent.ProcessingStatus.Pending,
    modify: (tiltaksgjennomforing: ArenaTiltaksgjennomforing) -> ArenaTiltaksgjennomforing = { it }
): ArenaEvent = modify(tiltaksgjennomforing).let {
    createArenaEvent(
        ArenaTable.Tiltaksgjennomforing,
        it.TILTAKGJENNOMFORING_ID.toString(),
        operation,
        Json.encodeToJsonElement(it).toString(),
        status,
    )
}

private fun createArenaEvent(
    table: ArenaTable,
    id: String,
    operation: ArenaEvent.Operation,
    data: String,
    status: ArenaEvent.ProcessingStatus = ArenaEvent.ProcessingStatus.Pending
): ArenaEvent {
    val before = if (operation == ArenaEvent.Operation.Delete) {
        data
    } else {
        null
    }

    val after = if (operation != ArenaEvent.Operation.Delete) {
        data
    } else {
        null
    }

    return ArenaEvent(
        arenaTable = table,
        arenaId = id,
        operation = operation,
        payload = Json.parseToJsonElement(
            """{
                "table": "${table.table}",
                "op_type": "${operation.opType}",
                "before": $before,
                "after": $after
            }
            """.trimIndent()
        ),
        status = status
    )
}
