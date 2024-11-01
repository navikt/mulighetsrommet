package no.nav.mulighetsrommet.utdanning.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramMedUtdanninger
import org.intellij.lang.annotations.Language

class UtdanningRepository(private val db: Database) {

    fun getUtdanningsprogrammer(): List<UtdanningsprogramMedUtdanninger> {
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
                coalesce(array_agg(nus_kode_innhold.nus_kode) filter (where nus_kode_innhold.nus_kode is not null), '{}') as nuskoder
            from
                utdanning
                    left join
                utdanning_nus_kode on utdanning.utdanning_id = utdanning_nus_kode.utdanning_id
                    left join
                utdanning_nus_kode_innhold nus_kode_innhold on utdanning_nus_kode.nus_kode = nus_kode_innhold.nus_kode
            group by
                utdanning.id, utdanning.navn
            having
                coalesce(array_agg(nus_kode_innhold.nus_kode) filter (where nus_kode_innhold.nus_kode is not null), '{}') <> '{}'
            order by utdanning.navn;
        """.trimIndent()

        val utdanningsprogrammer = queryOf(utdanningsprogrammerQuery).map { row ->
            UtdanningsprogramMedUtdanninger.Utdanningsprogram(
                id = row.uuid("id"),
                navn = row.string("navn"),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }.asList.let { db.run(it) }

        val utdanninger = queryOf(utdanningerQuery).map { row ->
            UtdanningsprogramMedUtdanninger.Utdanning(
                id = row.uuid("id"),
                navn = row.string("navn"),
                programlopStart = row.uuid("programlop_start"),
                nusKoder = row.array<String>("nusKoder").toList(),
            )
        }.asList.let { db.run(it) }

        return utdanningsprogrammer.map { utdanningsprogram ->
            UtdanningsprogramMedUtdanninger(
                utdanningsprogram = utdanningsprogram,
                utdanninger = utdanninger.filter { it.programlopStart == utdanningsprogram.id },
            )
        }
    }

    fun upsertPrograomomrade(session: TransactionalSession, utdanningsprogram: Utdanningsprogram) {
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
            "nus_koder" to session.createArrayOf("text", utdanningsprogram.nusKoder),
        )

        queryOf(query, params).asExecute.runWithSession(session)
    }

    fun upsertUtdanning(session: TransactionalSession, utdanning: Utdanning) {
        @Language("PostgreSQL")
        val getIdForProgramomradeQuery = """
            select id from utdanningsprogram where programomradekode = :programomradekode
        """.trimIndent()

        val programomradeId =
            queryOf(getIdForProgramomradeQuery, mapOf("programomradekode" to utdanning.utdanningslop.first()))
                .map { it.uuid("id") }
                .asSingle
                .runWithSession(session)

        @Language("PostgreSQL")
        val upsertUtdanning = """
            insert into utdanning (utdanning_id, programomradekode, navn, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start)
            values (:utdanning_id, :programomradekode, :navn, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start::uuid)
            on conflict (utdanning_id) do update set
                programomradekode = excluded.programomradekode,
                navn = excluded.navn,
                sluttkompetanse = excluded.sluttkompetanse,
                aktiv = excluded.aktiv,
                utdanningstatus = excluded.utdanningstatus,
                utdanningslop = excluded.utdanningslop,
                programlop_start = excluded.programlop_start
        """.trimIndent()

        @Language("PostgreSQL")
        val nuskodeInnholdInsertQuery = """
            insert into utdanning_nus_kode_innhold(title, nus_kode)
            values(:title, :nus_kode)
            on conflict (nus_kode) do update set
                title = excluded.title
        """.trimIndent()

        @Language("PostgreSQL")
        val nusKodeKoblingforUtdanningQuery = """
            insert into utdanning_nus_kode(utdanning_id, nus_kode)
            values (:utdanning_id, :nus_kode_id)
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
                "utdanningslop" to db.createTextArray(utdanning.utdanningslop),
                "programlop_start" to programomradeId,
            ),
        ).asExecute.runWithSession(session)

        utdanning.nusKodeverk.forEach { nus ->
            queryOf(
                nuskodeInnholdInsertQuery,
                mapOf("title" to nus.navn, "nus_kode" to nus.kode),
            ).asExecute.runWithSession(session)

            queryOf(
                nusKodeKoblingforUtdanningQuery,
                mapOf("utdanning_id" to utdanning.utdanningId, "nus_kode_id" to nus.kode),
            ).asExecute.runWithSession(session)
        }
    }
}
