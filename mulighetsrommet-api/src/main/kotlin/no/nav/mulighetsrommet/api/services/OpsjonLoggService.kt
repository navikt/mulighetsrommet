package no.nav.mulighetsrommet.api.services

import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtaler.OpsjonLoggValidator
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.routes.v1.OpsjonLoggRequest
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class OpsjonLoggService(
    private val db: Database,
    private val opsjonLoggValidator: OpsjonLoggValidator,
    private val avtaleRepository: AvtaleRepository,
    private val endringshistorikkService: EndringshistorikkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry) {
        val avtale = getAvtaleOrThrow(entry.avtaleId)
        opsjonLoggValidator.validate(entry, avtale.opsjonsmodellData).map {
            logger.info("Lagrer opsjon og setter ny sluttdato for avtale med id: '${entry.avtaleId}'. Opsjonsdata: $entry")
            db.transaction { tx ->
                if (entry.sluttdato != null) {
                    avtaleRepository.oppdaterSluttdato(entry.avtaleId, entry.sluttdato, tx)
                }

                queryOf(
                    getInsertOpsjonQuery(),
                    mapOf(
                        "avtaleId" to entry.avtaleId,
                        "sluttdato" to entry.sluttdato,
                        "status" to entry.status.name,
                        "registrertAv" to entry.registrertAv.value,
                    ),
                ).asExecute.let { tx.run(it) }

                loggEndring(tx, entry.registrertAv, getEndringsmeldingstekst(entry), entry.avtaleId, entry)
            }
        }.mapLeft {
            logger.debug("Klarte ikke å lagre opsjon: {})", it)
        }
    }

    fun delete(opsjonLoggEntryId: UUID, avtaleId: UUID, slettesAv: NavIdent) {
        val opsjoner = getOpsjoner(avtaleId)
        val avtale = getAvtaleOrThrow(avtaleId)
        validateAvtale(avtale)

        db.transaction { tx ->
            logger.info("Fjerner opsjons med id: '$opsjonLoggEntryId' for avtale med id: '$avtaleId'")
            val forrigeSluttdato = kalkulerNySluttdato(opsjoner, avtale)

            forrigeSluttdato?.let {
                avtaleRepository.oppdaterSluttdato(avtaleId, it, tx)
            }

            slettOpsjon(opsjonLoggEntryId, tx)
            loggEndring(tx, slettesAv, "Opsjon slettet", avtaleId, opsjoner.first())
        }
    }

    private fun getInsertOpsjonQuery(): String {
        @Language("PostgreSQL")
        val insertOpsjonQuery = """
            insert into avtale_opsjon_logg(avtale_id, sluttdato, status, registrert_av)
            values (:avtaleId, :sluttdato, :status::opsjonstatus, :registrertAv)
        """.trimIndent()
        return insertOpsjonQuery
    }

    private fun kalkulerNySluttdato(opsjoner: List<OpsjonLoggEntry>, avtale: AvtaleAdminDto): LocalDate? {
        return if (opsjoner.size > 1) opsjoner[1].sluttdato else avtale.opprinneligSluttDato
    }

    private fun validateAvtale(avtale: AvtaleAdminDto) {
        if (avtale.opprinneligSluttDato == null) {
            throw NotFoundException("Fant ingen opprinnelig sluttdato for avtale med id '${avtale.id}'")
        }
    }

    private fun getOpsjoner(avtaleId: UUID): List<OpsjonLoggEntry> {
        @Language("PostgreSQL")
        val getSisteOpsjonerQuery = """
            select * from avtale_opsjon_logg
            where avtale_id = :avtaleId::uuid and status = 'OPSJON_UTLØST'
            order by registrert_dato desc
        """.trimIndent()

        val opsjoner = queryOf(getSisteOpsjonerQuery, mapOf("avtaleId" to avtaleId)).map {
            it.toOpsjonLoggEntry()
        }.asList.let { db.run(it) }

        if (opsjoner.isEmpty()) {
            throw NotFoundException("Fant ingen utløste opsjoner for avtale med id '$avtaleId'")
        } else {
            return opsjoner
        }
    }

    private fun Row.toOpsjonLoggEntry(): OpsjonLoggEntry {
        return OpsjonLoggEntry(
            avtaleId = this.uuid("avtale_id"),
            sluttdato = this.localDate("sluttdato"),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.valueOf(this.string("status")),
            registrertAv = NavIdent(this.string("registrert_av")),
        )
    }

    private fun loggEndring(
        tx: TransactionalSession,
        slettesAv: NavIdent,
        operation: String,
        avtaleId: UUID,
        opsjon: OpsjonLoggEntry,
    ) {
        endringshistorikkService.logEndring(
            tx = tx,
            documentClass = DocumentClass.AVTALE,
            operation = operation,
            userId = slettesAv.value,
            documentId = avtaleId,
        ) {
            Json.encodeToJsonElement(opsjon)
        }
    }

    private fun slettOpsjon(opsjonLoggEntryId: UUID, tx: TransactionalSession) {
        @Language("PostgreSQL")
        val deleteOpsjonLoggEntryQuery = """
            delete from avtale_opsjon_logg where id = :id
        """.trimIndent()
        queryOf(deleteOpsjonLoggEntryQuery, mapOf("id" to opsjonLoggEntryId)).asExecute.let { tx.run(it) }
    }

    private fun getAvtaleOrThrow(avtaleId: UUID): AvtaleAdminDto {
        return avtaleRepository.get(avtaleId) ?: throw NotFoundException("Fant ingen avtale med id '$avtaleId'")
    }

    private fun getEndringsmeldingstekst(entry: OpsjonLoggEntry): String {
        return when (entry.status) {
            OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST -> "Opsjon utløst"
            OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            OpsjonLoggRequest.OpsjonsLoggStatus.PÅGÅENDE_OPSJONSPROSESS -> "Registrert at det er en pågående opsjonsprosess"
        }
    }
}
