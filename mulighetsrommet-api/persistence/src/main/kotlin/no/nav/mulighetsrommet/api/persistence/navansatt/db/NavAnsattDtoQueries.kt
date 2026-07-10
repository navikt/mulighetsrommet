package no.nav.mulighetsrommet.api.persistence.navansatt.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDto
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattRolleDto
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language

private val JsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

class NavAnsattDtoQueries(private val session: Session) : NavAnsattDtoQueryHandler {
    override fun getAll(rollerContainsAll: List<NavAnsattRolle>): List<NavAnsattDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt
            where (:roller::jsonb is null or (roller_json @> :roller::jsonb))
            order by fornavn, etternavn
        """.trimIndent()

        val params = mapOf(
            "roller" to rollerContainsAll.ifEmpty { null }?.let { Json.encodeToString(it) },
        )

        return list(queryOf(query, params)) { it.toNavAnsattDto() }
    }

    override fun getByNavIdent(navIdent: NavIdent): NavAnsattDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt
            where nav_ident = ?
        """.trimIndent()

        return single(queryOf(query, navIdent.value)) { it.toNavAnsattDto() }
    }
}

private fun Row.toNavAnsattDto(): NavAnsattDto {
    val roller = stringOrNull("roller_json")
        ?.let { JsonIgnoreUnknownKeys.decodeFromString<Set<NavAnsattRolle>>(it) }
        ?: setOf()
    return NavAnsattDto(
        entraObjectId = uuid("entra_object_id"),
        navIdent = NavIdent(string("nav_ident")),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = NavAnsattDto.Hovedenhet(
            enhetsnummer = NavEnhetNummer(string("hovedenhet_enhetsnummer")),
            navn = string("hovedenhet_navn"),
        ),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = roller
            .map { NavAnsattRolleDto(rolle = it.rolle, navn = it.rolle.visningsnavn) }
            .sortedBy { it.rolle },
    )
}
