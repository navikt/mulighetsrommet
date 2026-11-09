package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Artskonto
import no.nav.tiltak.okonomi.Regnskapskonto
import no.nav.tiltak.okonomi.oebs.OebsKontering
import org.intellij.lang.annotations.Language

class TiltakKonteringQueries(private val session: Session) {

    fun insertKontering(kontering: OebsKontering) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltak_kontering_oebs (regnskapskonto, artskonto, periode, statlig_regnskapskonto, statlig_artskonto)
            values (:regnskapskonto::regnskapskonto, :artskonto, :periode::daterange, :statlig_regnskapskonto, :statlig_artskonto)
        """.trimIndent()

        val params = mapOf(
            "regnskapskonto" to kontering.regnskapskonto.name,
            "artskonto" to kontering.artskonto.name,
            "periode" to kontering.periode.toDaterange(),
            "statlig_regnskapskonto" to kontering.statligRegnskapskonto,
            "statlig_artskonto" to kontering.statligArtskonto,
        )

        session.execute(queryOf(query, params))
    }

    fun getOebsKontering(
        regnskapskonto: Regnskapskonto,
        artskonto: Artskonto,
        periode: Periode,
    ): OebsKontering? {
        @Language("PostgreSQL")
        val query = """
            select regnskapskonto, artskonto, periode, statlig_regnskapskonto, statlig_artskonto
            from tiltak_kontering_oebs
            where regnskapskonto = :regnskapskonto
              and artskonto = :artskonto
              and periode @> :periode::daterange
        """.trimIndent()

        val params = mapOf(
            "regnskapskonto" to regnskapskonto.name,
            "artskonto" to artskonto.name,
            "periode" to periode.toDaterange(),
        )

        return session.single(queryOf(query, params)) {
            OebsKontering(
                regnskapskonto = Regnskapskonto.valueOf(it.string("regnskapskonto")),
                artskonto = Artskonto.valueOf(it.string("artskonto")),
                periode = it.periode("periode"),
                statligRegnskapskonto = it.string("statlig_regnskapskonto"),
                statligArtskonto = it.string("statlig_artskonto"),
            )
        }
    }
}
