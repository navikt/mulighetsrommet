package no.nav.mulighetsrommet.arena.adapter.models.arena

enum class ArenaTable(val table: String) {
    Tiltakstype("SIAMO.TILTAK"),

    Tiltaksgjennomforing("SIAMO.TILTAKGJENNOMFORING"),

    Sak("SIAMO.SAK"),

    Deltaker("SIAMO.TILTAKDELTAKER"),

    HistDeltaker("SIAMO.HIST_TILTAKDELTAKER"),

    AvtaleInfo("SIAMO.AVTALE_INFO"),
    ;

    companion object {
        fun fromTable(table: String): ArenaTable {
            return entries
                .firstOrNull { it.table == table }
                ?: throw IllegalArgumentException("Unsupported Arena table: $table")
        }
    }
}
