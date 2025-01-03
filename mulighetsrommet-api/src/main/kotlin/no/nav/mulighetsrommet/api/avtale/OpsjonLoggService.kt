package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.responses.ValidationError
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
    private val endringshistorikkService: EndringshistorikkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagreOpsjonLoggEntry(entry: OpsjonLoggEntry): Either<List<ValidationError>, Unit> = db.tx {
        val avtale = requireNotNull(Queries.avtale.get(entry.avtaleId))
        OpsjonLoggValidator.validate(entry, avtale).map {
            if (entry.sluttdato != null) {
                Queries.avtale.oppdaterSluttdato(entry.avtaleId, entry.sluttdato)
            }
            Queries.opsjoner.insert(entry)
            loggEndring(
                EndretAv.NavAnsatt(entry.registrertAv),
                getEndringsmeldingstekst(entry),
                entry.avtaleId,
                entry,
            )
        }
    }

    fun delete(opsjonLoggEntryId: UUID, avtaleId: UUID, slettesAv: NavIdent): Unit = db.tx {
        val opsjoner = Queries.opsjoner.get(avtaleId)
        val avtale = requireNotNull(Queries.avtale.get(avtaleId))

        logger.info("Fjerner opsjon med id: '$opsjonLoggEntryId' for avtale med id: '$avtaleId'")
        kalkulerNySluttdato(opsjoner, avtale)?.let {
            Queries.avtale.oppdaterSluttdato(avtaleId, it)
        }

        Queries.opsjoner.delete(opsjonLoggEntryId)
        loggEndring(EndretAv.NavAnsatt(slettesAv), "Opsjon slettet", avtaleId, opsjoner.first())
    }

    private fun kalkulerNySluttdato(opsjoner: List<OpsjonLoggEntry>, avtale: AvtaleDto): LocalDate? {
        val utlosteOpsjoner = opsjoner.filter { it.status == OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST }
            .sortedByDescending { it.forrigeSluttdato }

        if (utlosteOpsjoner.isNotEmpty()) {
            return utlosteOpsjoner[0].forrigeSluttdato ?: avtale.sluttDato
        }

        return avtale.sluttDato
    }

    private fun TransactionalSession.loggEndring(
        endretAv: EndretAv.NavAnsatt,
        operation: String,
        avtaleId: UUID,
        opsjon: OpsjonLoggEntry,
    ) {
        endringshistorikkService.logEndring(
            tx = this@TransactionalSession,
            documentClass = DocumentClass.AVTALE,
            operation = operation,
            user = endretAv,
            documentId = avtaleId,
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
