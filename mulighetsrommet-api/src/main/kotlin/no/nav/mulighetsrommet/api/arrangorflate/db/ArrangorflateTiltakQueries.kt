package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.avtale.db.toPrismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.intellij.lang.annotations.Language
import java.util.UUID

class ArrangorflateTiltakQueries(private val session: Session) {

    fun getOrError(id: UUID): ArrangorflateTiltak {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_arrangorflate_tiltak
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toArrangorflateTiltak() }
    }

    fun getAll(
        tiltakstyper: List<UUID>,
        organisasjonsnummer: List<Organisasjonsnummer>,
        statuser: List<GjennomforingStatusType>,
        prismodeller: List<PrismodellType>,
    ): List<ArrangorflateTiltak> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_arrangorflate_tiltak
            where tiltakstype_id = any(:tiltakstype_ids)
              and arrangor_organisasjonsnummer = any(:arrangor_orgnrs)
              and status = any(:statuser)
              and prismodell_type = any(:prismodeller::prismodell_type[])
        """.trimIndent()

        val parameters = mapOf(
            "tiltakstype_ids" to createUuidArray(tiltakstyper),
            "arrangor_orgnrs" to createArrayOfValue(organisasjonsnummer) { it.value },
            "statuser" to createArrayOf("gjennomforing_status", statuser),
            "prismodeller" to createArrayOf("prismodell_type", prismodeller),
        )

        return list(queryOf(query, parameters)) { it.toArrangorflateTiltak() }
    }
}

private fun Row.toArrangorflateTiltak(): ArrangorflateTiltak {
    val tiltakstype = ArrangorflateTiltak.Tiltakstype(
        id = uuid("tiltakstype_id"),
        navn = string("tiltakstype_navn"),
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
    )
    val arrangor = ArrangorflateTiltak.ArrangorUnderenhet(
        id = uuid("arrangor_id"),
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        navn = string("arrangor_navn"),
    )
    return ArrangorflateTiltak(
        id = uuid("id"),
        navn = string("navn"),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.GJENNOMFORES,
        arrangor = arrangor,
        tiltakstype = tiltakstype,
        prismodell = toPrismodell(),
    )
}
