package no.nav.mulighetsrommet.arena.adapter.models.arena

enum class ArenaTable(val table: String) {
    Tiltakstype("SIAMO.TILTAK"),

    Tiltaksgjennomforing("SIAMO.TILTAKGJENNOMFORING"),

    Sak("SIAMO.SAK"),

    Deltaker("SIAMO.TILTAKDELTAKER"),

    AvtaleInfo("SIAMO.AVTALE_INFO"),
    ;

    companion object {
        fun fromTable(table: String): ArenaTable {
            return values()
                .firstOrNull { it.table == table }
                ?: throw IllegalArgumentException("Unsupported Arena table: $table")
        }
    }
}
