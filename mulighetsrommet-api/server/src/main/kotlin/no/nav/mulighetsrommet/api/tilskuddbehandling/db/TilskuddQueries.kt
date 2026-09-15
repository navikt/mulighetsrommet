package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ValutaBelop
import org.intellij.lang.annotations.Language
import java.util.UUID

class TilskuddQueries(private val session: Session) {
    fun get(id: UUID): Tilskudd? {
        val vedtakQuery = """
            select * from view_tilskudd_vedtak
            where tilskudd_id = :id::uuid
        """.trimIndent()

        val vedtak = session.list(queryOf(vedtakQuery, mapOf("id" to id))) { it.toVedtak() }

        @Language("PostgreSQL")
        val tilskuddQuery = """
            select
                t.id,
                jsonb_build_object(
                   'id', o.id,
                   'navn', o.navn,
                   'kode', o.kode
                ) as tilskudd_opplaering,
                t.gjennomforing_id,
                t.tilskuddsnummer
            from tilskudd t
            inner join tilskudd_opplaering o on o.id = t.tilskudd_opplaering_id
            where t.id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(tilskuddQuery, mapOf("id" to id))) { it.toTilskudd(vedtak) }
    }
}

private fun Row.toTilskudd(vedtak: List<Tilskudd.Vedtak>): Tilskudd {
    return Tilskudd(
        id = uuid("id"),
        type = Json.decodeFromString(string("tilskudd_opplaering")),
        gjennomforingId = uuid("gjennomforing_id"),
        tilskuddsnummer = string("tilskuddsnummer"),
        vedtak = vedtak,
    )
}

private fun Row.toVedtak(): Tilskudd.Vedtak {
    return Tilskudd.Vedtak(
        id = uuid("id"),
        behandlingId = uuid("tilskudd_behandling_id"),
        soknadJournalpostId = string("soknad_journalpost_id"),
        soknadDato = localDate("soknad_dato"),
        periode = periode("periode"),
        kostnadssted = NavEnhetNummer(string("kostnadssted")),
        soknadBelop = Json.decodeFromString<ValutaBelop>(string("soknad_belop")),
        utbetalingBelop = stringOrNull("utbetaling_belop")?.let { Json.decodeFromString<ValutaBelop>(it) },
        vedtakResultat = VedtakResultat.valueOf(string("vedtak_resultat")),
        kommentarVedtaksbrev = stringOrNull("kommentar_vedtaksbrev"),
        utbetalingMottaker = TilskuddMottaker.valueOf(string("utbetaling_mottaker")),
        kid = stringOrNull("kid")?.let { Kid.parse(it) },
        kommentarIntern = stringOrNull("kommentar_intern"),
        vedtakJournalpostId = stringOrNull("vedtak_journalpost_id"),
    )
}
