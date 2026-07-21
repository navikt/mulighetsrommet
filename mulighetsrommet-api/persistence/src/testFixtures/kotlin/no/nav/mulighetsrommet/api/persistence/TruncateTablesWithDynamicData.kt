package no.nav.mulighetsrommet.api.persistence

import kotliquery.Session
import kotliquery.queryOf

/**
 * Sletter innholdet i alle standard databasetabell som _ikke_ inneholder hardkodet applikasjonsdata.
 * Tabeller som beholdes av denne rutinen inkluderer
 *   - Databasemigrasjoner (alle tester går mot samme database)
 *   - Kodeverk som eies av applikasjonen og er definert som repeterbare migrasjoner
 *   - Enum-tabeller
 */
fun truncateTablesWithDynamicData(session: Session) {
    val excludedTables = setOf(
        "flyway_schema_history",
        "opplaring_kategorisering_bransje",
        "opplaring_kategorisering_kurstype",
        "opplaring_forerkort",
        "opplaring_innhold_element",
        "deltaker_registrering_innholdselement",
        "personopplysning",
        "kostnadssted",
        "endringshistorikk_type",
        "nav_ansatt_rolle_type",
        "utbetaling_blokkering_type",
        "utbetaling_status_type",
        "utbetaling_linje_status_type",
        "tilsagn_type",
        "tilsagn_status_type",
        "vedtak_resultat",
        "tilskudd_opplaering",
        "tilskudd_behandling_status",
        "totrinnskontroll_type",
        "totrinnskontroll_status_type",
    )

    val tableNames = session.list(
        queryOf("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'"),
    ) {
        it.string("table_name")
    }

    tableNames.filter { it !in excludedTables }.forEach {
        session.execute(queryOf("truncate table $it restart identity cascade"))
    }
}
