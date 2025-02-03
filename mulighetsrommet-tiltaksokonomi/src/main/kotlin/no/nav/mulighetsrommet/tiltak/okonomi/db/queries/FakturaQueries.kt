package no.nav.mulighetsrommet.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.tiltak.okonomi.api.OkonomiPart
import no.nav.mulighetsrommet.tiltak.okonomi.db.Faktura
import no.nav.mulighetsrommet.tiltak.okonomi.db.FakturaStatusType
import no.nav.mulighetsrommet.tiltak.okonomi.db.LinjeDbo
import no.nav.mulighetsrommet.tiltak.okonomi.db.periode
import org.intellij.lang.annotations.Language

class FakturaQueries(private val session: Session) {

    fun opprettFaktura(faktura: Faktura) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into faktura (
                fakturanummer,
                bestillingsnummer,
                kontonummer,
                kid,
                belop,
                periode,
                status,
                opprettet_av,
                opprettet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt
            ) values (
                :fakturanummer,
                :bestillingsnummer,
                :kontonummer,
                :kid,
                :belop,
                daterange(:periode_start, :periode_slutt),
                :status,
                :opprettet_av,
                :opprettet_tidspunkt,
                :besluttet_av,
                :besluttet_tidspunkt
            )
            returning id
        """
        val params = mapOf(
            "fakturanummer" to faktura.fakturanummer,
            "bestillingsnummer" to faktura.bestillingsnummer,
            "kontonummer" to faktura.kontonummer.value,
            "kid" to faktura.kid?.value,
            "belop" to faktura.belop,
            "periode_start" to faktura.periode.start,
            "periode_slutt" to faktura.periode.slutt,
            "status" to faktura.status.name,
            "opprettet_av" to faktura.opprettetAv.part,
            "opprettet_tidspunkt" to faktura.opprettetTidspunkt,
            "besluttet_av" to faktura.besluttetAv.part,
            "besluttet_tidspunkt" to faktura.besluttetTidspunkt,
        )
        val fakturaId = single(queryOf(query, params)) { it.int("id") }

        @Language("PostgreSQL")
        val insertLinje = """
            insert into faktura_linje (faktura_id, linjenummer, periode, belop)
            values (:faktura_id, :linjenummer, daterange(:periode_start, :periode_slutt), :belop)
        """.trimIndent()
        val linjer = faktura.linjer.map {
            mapOf(
                "faktura_id" to fakturaId,
                "linjenummer" to it.linjenummer,
                "periode_start" to it.periode.start,
                "periode_slutt" to it.periode.slutt,
                "belop" to it.belop,
            )
        }
        batchPreparedNamedStatement(insertLinje, linjer)
    }

    fun getFaktura(fakturanummer: String): Faktura? {
        @Language("PostgreSQL")
        val selectLinje = """
            select linjenummer, periode, belop
            from faktura_linje
            where faktura_id = ?
            order by linjenummer
        """.trimIndent()

        @Language("PostgreSQL")
        val selectFaktura = """
            select
                id,
                bestillingsnummer,
                fakturanummer,
                kontonummer,
                kid,
                belop,
                periode,
                status,
                opprettet_av,
                opprettet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt
            from faktura
            where fakturanummer = ?
        """.trimIndent()

        return session.single(queryOf(selectFaktura, fakturanummer)) { faktura ->
            val linjer = session.list(queryOf(selectLinje, faktura.int("id"))) { linje ->
                LinjeDbo(
                    linjenummer = linje.int("linjenummer"),
                    periode = linje.periode("periode"),
                    belop = linje.int("belop"),
                )
            }

            Faktura(
                bestillingsnummer = faktura.string("bestillingsnummer"),
                fakturanummer = faktura.string("fakturanummer"),
                kontonummer = Kontonummer(faktura.string("kontonummer")),
                kid = faktura.stringOrNull("kid")?.let { kid -> Kid(kid) },
                belop = faktura.int("belop"),
                periode = faktura.periode("periode"),
                status = FakturaStatusType.valueOf(faktura.string("status")),
                opprettetAv = OkonomiPart.fromString(faktura.string("opprettet_av")),
                opprettetTidspunkt = faktura.localDateTime("opprettet_tidspunkt"),
                besluttetAv = OkonomiPart.fromString(faktura.string("besluttet_av")),
                besluttetTidspunkt = faktura.localDateTime("besluttet_tidspunkt"),
                linjer = linjer,
            )
        }
    }
}
