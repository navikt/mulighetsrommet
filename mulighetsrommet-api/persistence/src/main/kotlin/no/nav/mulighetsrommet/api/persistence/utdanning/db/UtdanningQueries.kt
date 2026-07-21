package no.nav.mulighetsrommet.api.persistence.utdanning.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramRepository
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language
import java.util.UUID

class UtdanningQueries(private val session: Session) : UtdanningsprogramRepository {

    override fun save(utdanningsprogram: Utdanningsprogram) {
        val programomradeId = upsertUtdanningsprogram(utdanningsprogram)
        utdanningsprogram.utdanninger.forEach { utdanning ->
            upsertUtdanning(programomradeId, utdanning)
        }
    }

    override fun findByProgramomradekode(programomradekode: String): Utdanningsprogram? = with(session) {
        @Language("PostgreSQL")
        val programomradeQuery = """
            select * from utdanningsprogram where programomradekode = ?
        """.trimIndent()

        data class Programomrade(
            val id: UUID,
            val navn: String,
            val type: UtdanningsprogramType?,
            val nusKoder: List<String>,
        )

        val programomrade = single(queryOf(programomradeQuery, programomradekode)) { row ->
            Programomrade(
                id = row.uuid("id"),
                navn = row.string("navn"),
                type = row.stringOrNull("utdanningsprogram_type")?.let { UtdanningsprogramType.valueOf(it) },
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        } ?: return null

        @Language("PostgreSQL")
        val utdanningerQuery = """
            select utdanning.*
            from utdanning
            join utdanningsprogram on utdanningsprogram.id = utdanning.programlop_start
            where utdanningsprogram.programomradekode = ?
        """.trimIndent()

        val utdanninger = list(queryOf(utdanningerQuery, programomradekode)) { row ->
            Utdanning(
                id = row.uuid("id"),
                programomradekode = row.string("programomradekode"),
                utdanningId = row.string("utdanning_id"),
                navn = row.string("navn"),
                sluttkompetanse = row.stringOrNull("sluttkompetanse")?.let { Utdanning.Sluttkompetanse.valueOf(it) },
                aktiv = row.boolean("aktiv"),
                utdanningstatus = Utdanning.Status.valueOf(row.string("utdanningstatus")),
                utdanningslop = row.array<String>("utdanningslop").toList(),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }

        return Utdanningsprogram.fromStorage(
            id = programomrade.id,
            programomradekode = programomradekode,
            navn = programomrade.navn,
            type = programomrade.type,
            nusKoder = programomrade.nusKoder,
            utdanninger = utdanninger,
        )
    }

    private fun upsertUtdanningsprogram(utdanningsprogram: Utdanningsprogram): UUID = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanningsprogram (id, navn, programomradekode, utdanningsprogram_type, nus_koder)
            values (:id::uuid, :navn, :programomradekode, :utdanningsprogram_type::utdanningsprogram_type, :nus_koder)
            on conflict (programomradekode) do update set
                navn = excluded.navn,
                utdanningsprogram_type = excluded.utdanningsprogram_type,
                nus_koder = excluded.nus_koder
            returning id
        """.trimIndent()

        val params = mapOf(
            "id" to utdanningsprogram.id,
            "navn" to utdanningsprogram.navn,
            "programomradekode" to utdanningsprogram.programomradekode,
            "utdanningsprogram_type" to utdanningsprogram.type?.name,
            "nus_koder" to createTextArray(utdanningsprogram.nusKoder),
        )

        return requireSingle(queryOf(query, params)) { it.uuid("id") }
    }

    private fun upsertUtdanning(programomradeId: UUID, utdanning: Utdanning) = with(session) {
        @Language("PostgreSQL")
        val upsertUtdanning = """
            insert into utdanning (id, utdanning_id, programomradekode, navn, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start, nus_koder)
            values (:id::uuid, :utdanning_id, :programomradekode, :navn, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start::uuid, :nus_koder)
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

        val params = mapOf(
            "id" to utdanning.id,
            "utdanning_id" to utdanning.utdanningId,
            "programomradekode" to utdanning.programomradekode,
            "navn" to utdanning.navn,
            "sluttkompetanse" to utdanning.sluttkompetanse?.name,
            "aktiv" to utdanning.aktiv,
            "utdanningstatus" to utdanning.utdanningstatus.name,
            "utdanningslop" to createTextArray(utdanning.utdanningslop),
            "programlop_start" to programomradeId,
            "nus_koder" to createTextArray(utdanning.nusKoder),
        )

        execute(queryOf(upsertUtdanning, params))
    }
}
