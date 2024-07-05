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
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class OpsjonLoggService(
    private val db: Database,
    private val opsjonLoggValidator: OpsjonLoggValidator,
    private val avtaleRepository: AvtaleRepository,
    private val endringshistorikkService: EndringshistorikkService,
) {
    val logger = LoggerFactory.getLogger(this::class.java)
    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, status, registrert_av)
            values (:avtaleId, :sluttdato, :status::opsjonstatus, :registrertAv)
        """.trimIndent()

        val avtale =
            avtaleRepository.get(entry.avtaleId) ?: throw NotFoundException("Fant ikke avtale med id ${entry.avtaleId}")

        opsjonLoggValidator.validate(entry, avtale.opsjonsmodellData).map {
            logger.info("Lagrer opsjon og setter ny sluttdato for avtale med id: '${entry.avtaleId}'. Opsjonsdata: $entry")
            db.transaction { tx ->
                if (entry.sluttdato != null) {
                    avtaleRepository.oppdaterSluttdato(entry.avtaleId, entry.sluttdato, tx)
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
        }.mapLeft {
            logger.debug("Klarte ikke å lagre opsjon: {})", it)
        }
    }

    fun delete(opsjonLoggEntryId: UUID, avtaleId: UUID, slettesAv: NavIdent) {
        @Language("PostgreSQL")
        val getSisteOpsjonerQuery = """
            select * from avtale_opsjon_logg
            where avtale_id = :avtaleId::uuid and status = 'OPSJON_UTLØST'
            order by registrert_dato desc
        """.trimIndent()

        val opsjoner = queryOf(getSisteOpsjonerQuery, mapOf("avtaleId" to avtaleId)).map { row ->
            OpsjonLoggEntry(
                avtaleId = row.uuid("avtale_id"),
                sluttdato = row.localDate("sluttdato"),
                status = OpsjonLoggRequest.OpsjonsLoggStatus.valueOf(row.string("status")),
                registrertAv = NavIdent(row.string("registrert_av")),
            )
        }.asList.let { db.run(it) }

        if (opsjoner.isEmpty()) {
            throw NotFoundException("Fant ingen utløst opsjon for avtale med id '$opsjonLoggEntryId'")
        }

        val avtale = avtaleRepository.get(avtaleId) ?: throw NotFoundException("Fant ingen avtale med id '$avtaleId'")

        if (avtale.opprinneligSluttDato == null) {
            throw NotFoundException("Fant ingen opprinnelig sluttdato for avtale med id '$avtaleId'")
        }

        @Language("PostgreSQL")
        val deleteOpsjonLoggEntryQuery = """
            delete from avtale_opsjon_logg where id = :id
        """.trimIndent()

        db.transaction { tx ->
            logger.info("Fjerner opsjons med id: '$opsjonLoggEntryId' for avtale med id: '$avtaleId'")
            val forrigeSluttdato =
                if (opsjoner.size > 1) opsjoner[1].sluttdato else avtale.opprinneligSluttDato

            forrigeSluttdato?.let {
                avtaleRepository.oppdaterSluttdato(avtaleId, it, tx)
            }

            queryOf(deleteOpsjonLoggEntryQuery, mapOf("id" to opsjonLoggEntryId)).asExecute.let { tx.run(it) }

            endringshistorikkService.logEndring(
                tx = tx,
                documentClass = DocumentClass.AVTALE,
                operation = "Opsjon slettet",
                userId = slettesAv.value,
                documentId = avtaleId,
            ) {
                Json.encodeToJsonElement(opsjoner.first())
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
