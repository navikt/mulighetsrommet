package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.models.TiltakshistorikkDTO
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HistorikkService(
    private val db: Database,
    private val arrangorService: ArrangorService
) {
    val log: Logger = LoggerFactory.getLogger(HistorikkService::class.java)

    suspend fun hentHistorikkForBruker(norskIdent: String): List<TiltakshistorikkDTO> {
        return getHistorikkForBrukerFromDb(norskIdent).map {
            val arrangor = it.virksomhetsnummer?.let { virksomhetsnummer -> hentArrangorNavn(virksomhetsnummer) }
            TiltakshistorikkDTO(
                id = it.id,
                fraDato = it.fraDato,
                tilDato = it.tilDato,
                status = it.status,
                tiltaksnavn = it.tiltaksnavn,
                tiltakstype = it.tiltakstype,
                arrangor = arrangor
            )
        }
    }

    private suspend fun hentArrangorNavn(virksomhetsnummer: String): String? {
        return try {
            arrangorService.hentArrangornavn(virksomhetsnummer)
        } catch (e: Throwable) {
            log.error("Feil oppstod ved henting arrangørnavn, sjekk securelogs")
            SecureLog.logger.error("Feil oppstod ved henting arrangørnavn", e)
            null
        }
    }

    private fun getHistorikkForBrukerFromDb(norskIdent: String): List<Tiltakshistorikk> {
        @Language("PostgreSQL")
        val query = """
            select tiltakshistorikk.id,
                   tiltakshistorikk.fra_dato,
                   tiltakshistorikk.til_dato,
                   tiltakshistorikk.status,
                   coalesce(gjennomforing.navn, tiltakshistorikk.beskrivelse) as navn,
                   coalesce(gjennomforing.virksomhetsnummer, tiltakshistorikk.virksomhetsnummer) as virksomhetsnummer,
                   t.navn as tiltakstype
            from tiltakshistorikk
                     left join tiltaksgjennomforing gjennomforing on gjennomforing.id = tiltakshistorikk.tiltaksgjennomforing_id
                     left join tiltakstype t on t.id = coalesce(gjennomforing.tiltakstype_id, tiltakshistorikk.tiltakstypeid)  
            where norsk_ident = ?
            order by tiltakshistorikk.fra_dato desc nulls last;
        """.trimIndent()
        val queryResult = queryOf(query, norskIdent).map { it.toTiltakshistorikk() }.asList
        return db.run(queryResult)
    }

    private fun Row.toTiltakshistorikk(): Tiltakshistorikk = Tiltakshistorikk(
        id = uuid("id"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(string("status")),
        tiltaksnavn = stringOrNull("navn"),
        tiltakstype = string("tiltakstype"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer")
    )
}
