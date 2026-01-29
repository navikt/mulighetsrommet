package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.avstemming.FakturaCsvData
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.oebs.OebsBetalingskanal
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class FakturaQueries(private val session: Session) {

    fun insertFaktura(faktura: Faktura) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into faktura (
                fakturanummer,
                bestillingsnummer,
                kontonummer,
                kid,
                bic,
                iban,
                bank_land_kode,
                bank_navn,
                valuta,
                betalingskanal,
                belop,
                periode,
                status,
                status_sist_oppdatert,
                behandlet_av,
                behandlet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt,
                beskrivelse
            ) values (
                :fakturanummer,
                :bestillingsnummer,
                :kontonummer,
                :kid,
                :bic,
                :iban,
                :bank_land_kode,
                :bank_navn,
                :valuta,
                :betalingskanal,
                :belop,
                :periode::daterange,
                :status,
                :status_sist_oppdatert,
                :behandlet_av,
                :behandlet_tidspunkt,
                :besluttet_av,
                :besluttet_tidspunkt,
                :beskrivelse
            )
            returning id
        """
        val params = mapOf(
            "fakturanummer" to faktura.fakturanummer,
            "bestillingsnummer" to faktura.bestillingsnummer,
            "belop" to faktura.belop,
            "periode" to faktura.periode.toDaterange(),
            "status" to faktura.status.name,
            "status_sist_oppdatert" to faktura.fakturaStatusSistOppdatert,
            "behandlet_av" to faktura.behandletAv.part,
            "behandlet_tidspunkt" to faktura.behandletTidspunkt,
            "besluttet_av" to faktura.besluttetAv.part,
            "besluttet_tidspunkt" to faktura.besluttetTidspunkt,
            "beskrivelse" to faktura.beskrivelse,
            "kontonummer" to faktura.betalingsinformasjon?.kontonummer?.value,
            "kid" to faktura.betalingsinformasjon?.kid?.value,
            "bic" to faktura.betalingsinformasjon?.bic,
            "iban" to faktura.betalingsinformasjon?.iban,
            "land_kode" to faktura.betalingsinformasjon?.bankLandKode,
            "bank_navn" to faktura.betalingsinformasjon?.bankNavn,
            "valuta" to faktura.valuta.name,
            "betalingskanal" to faktura.betalingsinformasjon?.betalingsKanal?.name,
        )

        val fakturaId = single(queryOf(query, params)) { it.int("id") }

        @Language("PostgreSQL")
        val insertLinje = """
            insert into faktura_linje (faktura_id, linjenummer, periode, belop)
            values (:faktura_id, :linjenummer, :periode::daterange, :belop)
        """.trimIndent()
        val linjer = faktura.linjer.map {
            mapOf(
                "faktura_id" to fakturaId,
                "linjenummer" to it.linjenummer,
                "periode" to it.periode.toDaterange(),
                "belop" to it.belop,
            )
        }
        batchPreparedNamedStatement(insertLinje, linjer)
    }

    fun setStatus(fakturanummer: String, status: FakturaStatusType) {
        @Language("PostgreSQL")
        val query = """
            update faktura
            set status = ?,
            status_sist_oppdatert = now()
            where fakturanummer = ?
        """.trimIndent()
        session.execute(queryOf(query, status.name, fakturanummer))
    }

    fun setAvstemtTidspunkt(tidspunkt: LocalDateTime, fakturanummer: List<String>) {
        @Language("PostgreSQL")
        val query = """
            update faktura
            set avstemt_tidspunkt = :tidspunkt
            where fakturanummer = any(:fakturanummer)
        """.trimIndent()
        session.execute(
            queryOf(
                query,
                mapOf(
                    "tidspunkt" to tidspunkt,
                    "fakturanummer" to session.createTextArray(fakturanummer),
                ),
            ),
        )
    }

    fun setFeilmelding(
        fakturanummer: String,
        feilKode: String?,
        feilMelding: String?,
    ) {
        @Language("PostgreSQL")
        val query = """
            update faktura set
                feil_kode = :feil_kode,
                feil_melding = :feil_melding
            where fakturanummer = :fakturanummer
        """.trimIndent()
        val params = mapOf(
            "fakturanummer" to fakturanummer,
            "feil_kode" to feilKode,
            "feil_melding" to feilMelding,
        )
        session.execute(queryOf(query, params))
    }

    fun getNotAvstemt(): List<FakturaCsvData> {
        @Language("PostgreSQL")
        val selectFaktura = """
            select
                faktura.fakturanummer,
                faktura.belop,
                faktura.valuta,
                faktura.besluttet_tidspunkt,
                bestilling.arrangor_hovedenhet,
                bestilling.arrangor_underenhet
            from faktura
                inner join bestilling on bestilling.bestillingsnummer = faktura.bestillingsnummer
            where faktura.avstemt_tidspunkt is null
        """.trimIndent()

        return session.list(queryOf(selectFaktura)) {
            FakturaCsvData(
                fakturanummer = it.string("fakturanummer"),
                belop = it.int("belop"),
                valuta = Valuta.valueOf(it.string("valuta")),
                besluttetTidspunkt = it.localDateTime("besluttet_tidspunkt"),
                arrangorHovedenhet = Organisasjonsnummer(it.string("arrangor_hovedenhet")),
                arrangorUnderenhet = Organisasjonsnummer(it.string("arrangor_underenhet")),
            )
        }
    }

    fun getByFakturanummer(fakturanummer: String): Faktura? {
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
                bic,
                iban,
                bank_land_kode,
                bank_navn,
                valuta,
                betalingskanal,
                belop,
                periode,
                status,
                status_sist_oppdatert,
                behandlet_av,
                behandlet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt,
                beskrivelse
            from faktura
            where fakturanummer = ?
        """.trimIndent()

        return session.single(queryOf(selectFaktura, fakturanummer)) { row ->
            val linjer = session.list(queryOf(selectLinje, row.int("id"))) { linje ->
                Faktura.Linje(
                    linjenummer = linje.int("linjenummer"),
                    periode = linje.periode("periode"),
                    belop = linje.int("belop"),
                )
            }
            val betalingskanal = row.stringOrNull("betalingskanal")?.let { OebsBetalingskanal.valueOf(it) }

            Faktura(
                bestillingsnummer = row.string("bestillingsnummer"),
                fakturanummer = row.string("fakturanummer"),
                betalingsinformasjon = betalingskanal?.let {
                    Faktura.Betalingsinformasjon(
                        kontonummer = row.stringOrNull("kontonummer")?.let { Kontonummer(it) },
                        kid = row.stringOrNull("kid")?.let { kid -> Kid.parseOrThrow(kid) },
                        iban = row.stringOrNull("iban"),
                        bic = row.stringOrNull("bic"),
                        bankLandKode = row.stringOrNull("bank_land_kode"),
                        bankNavn = row.stringOrNull("bank_navn"),
                        betalingsKanal = betalingskanal,
                    )
                },
                belop = row.int("belop"),
                periode = row.periode("periode"),
                status = FakturaStatusType.valueOf(row.string("status")),
                fakturaStatusSistOppdatert = row.localDateTimeOrNull("status_sist_oppdatert"),
                behandletAv = OkonomiPart.fromString(row.string("behandlet_av")),
                behandletTidspunkt = row.localDateTime("behandlet_tidspunkt"),
                besluttetAv = OkonomiPart.fromString(row.string("besluttet_av")),
                besluttetTidspunkt = row.localDateTime("besluttet_tidspunkt"),
                linjer = linjer,
                beskrivelse = row.stringOrNull("beskrivelse"),
                valuta = Valuta.valueOf(row.string("valuta")),
            )
        }
    }

    fun getByBestillingsnummer(bestillingsnummer: String): List<Faktura> {
        @Language("PostgreSQL")
        val sql = """
            select fakturanummer from faktura where bestillingsnummer = ?
        """.trimIndent()

        return session.list(queryOf(sql, bestillingsnummer)) { it.string("fakturanummer") }
            .mapNotNull { getByFakturanummer(it) }
    }
}
