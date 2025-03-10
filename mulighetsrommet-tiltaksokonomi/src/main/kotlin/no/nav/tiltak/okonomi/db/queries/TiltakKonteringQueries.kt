package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.db.periode
import no.nav.tiltak.okonomi.db.toDaterange
import no.nav.tiltak.okonomi.model.OebsKontering
import org.intellij.lang.annotations.Language

class TiltakKonteringQueries(private val session: Session) {

    fun insertKontering(kontering: OebsKontering) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltak_kontering_oebs (tilskuddstype, tiltakskode, periode, statlig_regnskapskonto, statlig_artskonto)
            values (:tilskuddstype::tilskuddstype, :tiltakskode, :periode::daterange, :statlig_regnskapskonto, :statlig_artskonto)
        """.trimIndent()

        val params = mapOf(
            "tilskuddstype" to kontering.tilskuddstype.name,
            "tiltakskode" to kontering.tiltakskode.name,
            "periode" to kontering.periode.toDaterange(),
            "statlig_regnskapskonto" to kontering.statligRegnskapskonto,
            "statlig_artskonto" to kontering.statligArtskonto,
        )

        session.execute(queryOf(query, params))
    }

    fun getOebsKontering(
        tilskuddstype: Tilskuddstype,
        tiltakskode: Tiltakskode,
        periode: Periode,
    ): OebsKontering? {
        @Language("PostgreSQL")
        val query = """
            select tilskuddstype, tiltakskode, periode, statlig_regnskapskonto, statlig_artskonto
            from tiltak_kontering_oebs
            where tilskuddstype = :tilskuddstype
              and tiltakskode = :tiltakskode
              and periode @> :periode::daterange
        """.trimIndent()

        val params = mapOf(
            "tilskuddstype" to tilskuddstype.name,
            "tiltakskode" to tiltakskode.name,
            "periode" to periode.toDaterange(),
        )

        return session.single(queryOf(query, params)) {
            OebsKontering(
                tilskuddstype = Tilskuddstype.valueOf(it.string("tilskuddstype")),
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                periode = it.periode("periode"),
                statligRegnskapskonto = it.string("statlig_regnskapskonto"),
                statligArtskonto = it.string("statlig_artskonto"),
            )
        }
    }
}
