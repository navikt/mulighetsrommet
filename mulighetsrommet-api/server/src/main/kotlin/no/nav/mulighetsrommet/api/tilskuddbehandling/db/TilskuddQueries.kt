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
        @Language("PostgreSQL")
        val vedtakQuery = """
            select * from view_tilskudd_vedtak
            where tilskudd_id = :id::uuid
            order by lopenummer desc
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

    fun getAll(gjennomforingId: UUID): List<TilskuddKompakt> {
        @Language("PostgreSQL")
        val query = """
            select
                t.id,
                jsonb_build_object(
                   'id', o.id,
                   'navn', o.navn,
                   'kode', o.kode
                ) as tilskudd_opplaering,
                t.gjennomforing_id,
                t.tilskuddsnummer,
                siste_vedtak.vedtak_resultat,
                siste_vedtak.periode,
                siste_vedtak.lopenummer
            from tilskudd t
            inner join tilskudd_opplaering o on o.id = t.tilskudd_opplaering_id
            left join lateral (
                select tv.vedtak_resultat,
                       tv.periode,
                       tv.lopenummer
                from tilskudd_vedtak tv
                where tv.tilskudd_id = t.id
                order by tv.lopenummer desc
                limit 1
            ) siste_vedtak on true
            where t.gjennomforing_id = :gjennomforingId::uuid
            order by t.tilskuddsnummer
        """.trimIndent()

        return session.list(queryOf(query, mapOf("gjennomforingId" to gjennomforingId))) { it.toTilskuddKompakt() }
    }
}

private fun Row.toTilskudd(vedtak: List<Tilskudd.Vedtak>): Tilskudd {
    return Tilskudd(
        id = uuid("id"),
        type = Json.decodeFromString(string("tilskudd_opplaering")),
        gjennomforingId = uuid("gjennomforing_id"),
        tilskuddsnummer = Tilskuddsnummer(string("tilskuddsnummer")),
        vedtak = vedtak,
    )
}

private fun Row.toVedtak(): Tilskudd.Vedtak {
    val mottaker = TilskuddMottaker.valueOf(string("utbetaling_mottaker"))
    val vedtaksResultat = VedtakResultat.valueOf(string("vedtak_resultat"))
    val utbetaling = if (vedtaksResultat == VedtakResultat.INNVILGELSE) toTIlskuddUTbetaling(mottaker) else null
    return Tilskudd.Vedtak(
        id = uuid("id"),
        behandlingId = uuid("tilskudd_behandling_id"),
        lopenummer = int("lopenummer"),
        soknadJournalpostId = string("soknad_journalpost_id"),
        soknadDato = localDate("soknad_dato"),
        periode = periode("periode"),
        kostnadssted = NavEnhetNummer(string("kostnadssted")),
        soknadBelop = Json.decodeFromString<ValutaBelop>(string("soknad_belop")),
        vedtakResultat = vedtaksResultat,
        kommentarVedtaksbrev = stringOrNull("kommentar_vedtaksbrev"),
        utbetalingMottaker = mottaker,
        kommentarIntern = stringOrNull("kommentar_intern"),
        vedtakJournalpostId = stringOrNull("vedtak_journalpost_id"),
        utbetaling = utbetaling,
    )
}

private fun Row.toTIlskuddUTbetaling(mottaker: TilskuddMottaker): Tilskudd.Vedtak.Utbetaling {
    val utbetalingBelop = string("utbetaling_belop").let { Json.decodeFromString<ValutaBelop>(it) }
    return when (mottaker) {
        TilskuddMottaker.BRUKER ->
            Tilskudd.Vedtak.Utbetaling.Bruker(uuidOrNull("bruker_utbetaling_id"), utbetalingBelop)

        TilskuddMottaker.ARRANGOR ->
            Tilskudd.Vedtak.Utbetaling.Arrangor(
                uuidOrNull("arrangor_utbetaling_id"),
                stringOrNull("kid")?.let { Kid.parse(it) },
                utbetalingBelop,
            )
    }
}

private fun Row.toTilskuddKompakt(): TilskuddKompakt {
    return TilskuddKompakt(
        id = uuid("id"),
        type = Json.decodeFromString(string("tilskudd_opplaering")),
        gjennomforingId = uuid("gjennomforing_id"),
        tilskuddsnummer = Tilskuddsnummer(string("tilskuddsnummer")),
        periode = periode("periode"),
        sisteVedtakResultat = stringOrNull("vedtak_resultat")?.let { VedtakResultat.valueOf(it) },
        sisteVedtakLopenummer = int("lopenummer"),
    )
}
