package no.nav.mulighetsrommet.utdanning.db

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramMedUtdanninger
import org.intellij.lang.annotations.Language
import java.util.*

object UtdanningQueries {

    fun getUtdanningsprogrammer(session: Session): List<UtdanningsprogramMedUtdanninger> {
        @Language("PostgreSQL")
        val utdanningsprogrammerQuery = """
            select *
            from utdanningsprogram
            where utdanningsprogram_type = 'YRKESFAGLIG' and array_length(nus_koder, 1) > 0
            order by navn
        """.trimIndent()

        @Language("PostgreSQL")
        val utdanningerQuery = """
            select
                utdanning.id,
                utdanning.navn,
                utdanning.programlop_start,
                nus_koder as nusKoder
            from utdanning
            where nus_koder <> '{}'
            group by utdanning.id
            order by utdanning.navn;
        """.trimIndent()

        val utdanningsprogrammer = queryOf(utdanningsprogrammerQuery).map { row ->
            UtdanningsprogramMedUtdanninger.Utdanningsprogram(
                id = row.uuid("id"),
                navn = row.string("navn"),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }.asList.runWithSession(session)

        val utdanninger = queryOf(utdanningerQuery).map { row ->
            UtdanningsprogramMedUtdanninger.Utdanning(
                id = row.uuid("id"),
                navn = row.string("navn"),
                programlopStart = row.uuid("programlop_start"),
                nusKoder = row.array<String>("nusKoder").toList(),
            )
        }.asList.runWithSession(session)

        return utdanningsprogrammer.map { utdanningsprogram ->
            UtdanningsprogramMedUtdanninger(
                utdanningsprogram = utdanningsprogram,
                utdanninger = utdanninger.filter { it.programlopStart == utdanningsprogram.id },
            )
        }
    }

    fun upsertUtdanningsprogram(session: TransactionalSession, utdanningsprogram: Utdanningsprogram) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanningsprogram (navn, programomradekode, utdanningsprogram_type, nus_koder)
            values (:navn, :programomradekode, :utdanningsprogram_type::utdanningsprogram_type, :nus_koder)
            on conflict (programomradekode) do update set
                navn = excluded.navn,
                utdanningsprogram_type = excluded.utdanningsprogram_type,
                nus_koder = excluded.nus_koder
        """.trimIndent()

        val params = mapOf(
            "navn" to utdanningsprogram.navn,
            "programomradekode" to utdanningsprogram.programomradekode,
            "utdanningsprogram_type" to utdanningsprogram.type?.name,
            "nus_koder" to session.createTextArray(utdanningsprogram.nusKoder),
        )

        queryOf(query, params).asExecute.runWithSession(session)
    }

    fun upsertUtdanning(session: TransactionalSession, utdanning: Utdanning) {
        val programomradeId = getIdForUtdanningsprogram(session, utdanning.utdanningslop.first())

        @Language("PostgreSQL")
        val upsertUtdanning = """
            insert into utdanning (utdanning_id, programomradekode, navn, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start, nus_koder)
            values (:utdanning_id, :programomradekode, :navn, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start::uuid, :nus_koder)
            on conflict (utdanning_id) do update set
                programomradekode = excluded.programomradekode,
                navn = excluded.navn,
                sluttkompetanse = excluded.sluttkompetanse,
                aktiv = excluded.aktiv,
                utdanningstatus = excluded.utdanningstatus,
                utdanningslop = excluded.utdanningslop,
                programlop_start = excluded.programlop_start,
                nus_koder = excluded.nus_koder
        """.trimIndent()

        queryOf(
            upsertUtdanning,
            mapOf(
                "utdanning_id" to utdanning.utdanningId,
                "programomradekode" to utdanning.programomradekode,
                "navn" to utdanning.navn,
                "sluttkompetanse" to utdanning.sluttkompetanse?.name,
                "aktiv" to utdanning.aktiv,
                "utdanningstatus" to utdanning.utdanningstatus.name,
                "utdanningslop" to session.createTextArray(utdanning.utdanningslop),
                "programlop_start" to programomradeId,
                "nus_koder" to session.createTextArray(utdanning.nusKoder),
            ),
        ).asExecute.runWithSession(session)
    }

    fun getIdForUtdanningsprogram(session: Session, programomradekode: String): UUID {
        @Language("PostgreSQL")
        val query = """
            select id from utdanningsprogram where programomradekode = ?
        """.trimIndent()

        return queryOf(query, programomradekode)
            .map { it.uuid("id") }
            .asSingle
            .runWithSession(session)
            .let { requireNotNull(it) { "Fant ingen utdanningsprogram med kode=$programomradekode" } }
    }

    fun getIdForUtdanning(session: Session, utdanningId: String): UUID {
        @Language("PostgreSQL")
        val query = """
            select id from utdanning where utdanning_id = ?
        """.trimIndent()

        return queryOf(query, utdanningId)
            .map { it.uuid("id") }
            .asSingle
            .runWithSession(session)
            .let { requireNotNull(it) { "Fant ingen utdanning med id=$utdanningId" } }
    }
}
