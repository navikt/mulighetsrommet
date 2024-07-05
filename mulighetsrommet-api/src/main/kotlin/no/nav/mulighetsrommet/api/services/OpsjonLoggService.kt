package no.nav.mulighetsrommet.api.services

import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtaler.OpsjonLoggValidator
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.routes.v1.OpsjonLoggRequest
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class OpsjonLoggService(
    private val db: Database,
    private val opsjonLoggValidator: OpsjonLoggValidator,
    private val avtaleRepository: AvtaleRepository,
    private val endringshistorikkService: EndringshistorikkService,
) {
    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, status, registrert_av)
            values (:avtaleId, :sluttdato, :status::opsjonstatus, :registrertAv)
        """.trimIndent()

        val avtale =
            avtaleRepository.get(entry.avtaleId) ?: throw NotFoundException("Fant ikke avtale med id ${entry.avtaleId}")

        opsjonLoggValidator.validate(entry, avtale.opsjonsmodellData).map {
            db.transaction { tx ->
                if (entry.sluttdato != null) {
                    avtaleRepository.oppdaterSluttdato(entry.avtaleId, entry.sluttdato)
                }
                queryOf(
                    query,
                    mapOf(
                        "avtaleId" to entry.avtaleId,
                        "sluttdato" to entry.sluttdato,
                        "status" to entry.status.name,
                        "registrertAv" to entry.registrertAv.value,
                    ),
                ).asExecute.let { tx.run(it) }

                endringshistorikkService.logEndring(
                    documentClass = DocumentClass.AVTALE,
                    operation = getEndringsmeldingstekst(entry),
                    userId = entry.registrertAv.value,
                    documentId = entry.avtaleId,
                ) {
                    Json.encodeToJsonElement(entry)
                }
            }
        }
    }

    private fun getEndringsmeldingstekst(entry: OpsjonLoggEntry): String {
        return when (entry.status) {
            OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST -> "Opsjon utløst"
            OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            OpsjonLoggRequest.OpsjonsLoggStatus.PÅGÅENDE_OPSJONSPROSESS -> "Registrert at det er en pågående opsjonsprosess"
        }
    }
}
