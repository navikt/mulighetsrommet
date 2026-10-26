package no.nav.mulighetsrommet.api.persistence.veilederflate

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakstypeKombinasjon
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakstypeVeilederinfo
import org.intellij.lang.annotations.Language
import java.util.UUID

class VeilederflateTiltakstypeQueries(private val session: Session) : VeilederflateTiltakstypeQueryHandler {
    override fun getVeilederinfo(id: UUID): VeilederflateTiltakstypeVeilederinfo? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select beskrivelse, faneinnhold, faglenker, kan_kombineres_med
            from view_tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toVeilederinfo() }
    }

    private fun Row.toVeilederinfo(): VeilederflateTiltakstypeVeilederinfo {
        val kanKombineresMed = stringOrNull("kan_kombineres_med")
            ?.let { Json.decodeFromString<List<VeilederflateTiltakstypeKombinasjon>>(it) }
            ?: emptyList()

        val faglenker = stringOrNull("faglenker")
            ?.let { Json.decodeFromString<List<RedaksjoneltInnholdLenke>>(it) }
            ?: emptyList()

        return VeilederflateTiltakstypeVeilederinfo(
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            faglenker = faglenker,
            kanKombineresMed = kanKombineresMed,
        )
    }
}
