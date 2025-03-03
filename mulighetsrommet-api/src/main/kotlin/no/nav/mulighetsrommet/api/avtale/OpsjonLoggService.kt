package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.NavIdent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OpsjonLoggService(
    private val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry): Either<List<FieldError>, Unit> = db.transaction {
        val avtale = requireNotNull(queries.avtale.get(entry.avtaleId))
        OpsjonLoggValidator.validate(entry, avtale).map {
            if (entry.sluttdato != null) {
                queries.avtale.oppdaterSluttdato(entry.avtaleId, entry.sluttdato)
            }
            queries.opsjoner.insert(entry)
            loggEndring(
                entry.registrertAv,
                getEndringsmeldingstekst(entry),
                entry.avtaleId,
                entry,
            )
        }
    }

    fun delete(opsjonLoggEntryId: UUID, avtaleId: UUID, slettesAv: NavIdent): Unit = db.transaction {
        val opsjoner = queries.opsjoner.get(avtaleId)
        val avtale = requireNotNull(queries.avtale.get(avtaleId))

        logger.info("Fjerner opsjon med id: '$opsjonLoggEntryId' for avtale med id: '$avtaleId'")
        kalkulerNySluttdato(opsjoner, avtale)?.let {
            queries.avtale.oppdaterSluttdato(avtaleId, it)
        }

        queries.opsjoner.delete(opsjonLoggEntryId)
        loggEndring(slettesAv, "Opsjon slettet", avtaleId, opsjoner.first())
    }

    private fun kalkulerNySluttdato(opsjoner: List<OpsjonLoggEntry>, avtale: AvtaleDto): LocalDate? {
        val utlosteOpsjoner = opsjoner.filter { it.status == OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST }
            .sortedByDescending { it.forrigeSluttdato }

        if (utlosteOpsjoner.isNotEmpty()) {
            return utlosteOpsjoner[0].forrigeSluttdato ?: avtale.sluttDato
        }

        return avtale.sluttDato
    }

    private fun QueryContext.loggEndring(
        endretAv: NavIdent,
        operation: String,
        avtaleId: UUID,
        opsjon: OpsjonLoggEntry,
    ) {
        queries.endringshistorikk.logEndring(
            documentClass = DocumentClass.AVTALE,
            operation = operation,
            agent = endretAv,
            documentId = avtaleId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(opsjon)
        }
    }

    private fun getEndringsmeldingstekst(entry: OpsjonLoggEntry): String {
        return when (entry.status) {
            OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST -> "Opsjon registrert"
            OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            OpsjonLoggRequest.OpsjonsLoggStatus.PÅGÅENDE_OPSJONSPROSESS -> "Registrert at det er en pågående opsjonsprosess"
        }
    }
}
