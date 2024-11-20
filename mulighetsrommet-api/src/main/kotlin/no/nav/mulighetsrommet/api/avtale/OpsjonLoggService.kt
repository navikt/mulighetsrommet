package no.nav.mulighetsrommet.api.avtale

import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggRepository
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class OpsjonLoggService(
    private val db: Database,
    private val opsjonLoggValidator: OpsjonLoggValidator,
    private val avtaleRepository: AvtaleRepository,
    private val opsjonLoggRepository: OpsjonLoggRepository,
    private val endringshistorikkService: EndringshistorikkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry) {
        val avtale = getAvtaleOrThrow(entry.avtaleId)
        opsjonLoggValidator.validate(entry, avtale).map {
            logger.info("Lagrer opsjon og setter ny sluttdato for avtale med id: '${entry.avtaleId}'. Opsjonsdata: $entry")
            db.transaction { tx ->
                if (entry.sluttdato != null) {
                    avtaleRepository.oppdaterSluttdato(entry.avtaleId, entry.sluttdato, tx)
                }
                opsjonLoggRepository.insert(entry, tx)
                loggEndring(
                    tx,
                    EndretAv.NavAnsatt(entry.registrertAv),
                    getEndringsmeldingstekst(entry),
                    entry.avtaleId,
                    entry,
                )
            }
        }.mapLeft {
            logger.debug("Klarte ikke å lagre opsjon: {})", it)
        }
    }

    fun delete(opsjonLoggEntryId: UUID, avtaleId: UUID, slettesAv: NavIdent) {
        val opsjoner = opsjonLoggRepository.getOpsjoner(avtaleId)
        val avtale = getAvtaleOrThrow(avtaleId)

        db.transaction { tx ->
            logger.info("Fjerner opsjon med id: '$opsjonLoggEntryId' for avtale med id: '$avtaleId'")
            val forrigeSluttdato = kalkulerNySluttdato(opsjoner, avtale)

            forrigeSluttdato?.let {
                avtaleRepository.oppdaterSluttdato(avtaleId, it, tx)
            }

            opsjonLoggRepository.delete(opsjonLoggEntryId, tx)
            loggEndring(tx, EndretAv.NavAnsatt(slettesAv), "Opsjon slettet", avtaleId, opsjoner.first())
        }
    }

    private fun kalkulerNySluttdato(opsjoner: List<OpsjonLoggEntry>, avtale: AvtaleDto): LocalDate? {
        val utlosteOpsjoner = opsjoner.filter { it.status == OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST }
            .sortedByDescending { it.forrigeSluttdato }

        if (utlosteOpsjoner.isNotEmpty()) {
            return utlosteOpsjoner[0].forrigeSluttdato ?: avtale.sluttDato
        }

        return avtale.sluttDato
    }

    private fun loggEndring(
        tx: TransactionalSession,
        endretAv: EndretAv.NavAnsatt,
        operation: String,
        avtaleId: UUID,
        opsjon: OpsjonLoggEntry,
    ) {
        endringshistorikkService.logEndring(
            tx = tx,
            documentClass = DocumentClass.AVTALE,
            operation = operation,
            user = endretAv,
            documentId = avtaleId,
        ) {
            Json.encodeToJsonElement(opsjon)
        }
    }

    private fun getAvtaleOrThrow(avtaleId: UUID): AvtaleDto {
        return avtaleRepository.get(avtaleId) ?: throw NotFoundException("Fant ingen avtale med id '$avtaleId'")
    }

    private fun getEndringsmeldingstekst(entry: OpsjonLoggEntry): String {
        return when (entry.status) {
            OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST -> "Opsjon registrert"
            OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            OpsjonLoggRequest.OpsjonsLoggStatus.PÅGÅENDE_OPSJONSPROSESS -> "Registrert at det er en pågående opsjonsprosess"
        }
    }
}
