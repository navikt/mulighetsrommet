package no.nav.mulighetsrommet.api.application.tiltak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.application.AdminDatabase
import no.nav.mulighetsrommet.api.application.QueryContext
import no.nav.mulighetsrommet.api.application.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import java.time.LocalDateTime
import java.util.UUID

data class UpsertVeilederinfoCommand(
    val id: UUID,
    val veilederinfo: Tiltakstype.Veilederinfo,
    val endretAv: NavIdent,
)

data class UpsertDeltakerinfoCommand(
    val id: UUID,
    val deltakerinfo: Tiltakstype.Deltakerinfo,
    val endretAv: NavIdent,
)

class TiltakstypeUseCase(
    private val db: AdminDatabase,
) {
    fun execute(command: UpsertVeilederinfoCommand): Either<TiltakstypeUseCaseError, Unit> = db.transaction {
        val tiltakstype = repository.tiltakstype.get(command.id)
            ?: return@transaction TiltakstypeUseCaseError.NotFound(command.id).left()

        val updated = tiltakstype.copy(veilederinfo = command.veilederinfo)
        repository.tiltakstype.save(updated)

        logEndring("Redigerte informasjon for veiledere", updated, command.endretAv)
        publishToKafka(updated)
        Unit.right()
    }

    fun execute(command: UpsertDeltakerinfoCommand): Either<TiltakstypeUseCaseError, Unit> = db.transaction {
        val tiltakstype = repository.tiltakstype.get(command.id)
            ?: return@transaction TiltakstypeUseCaseError.NotFound(command.id).left()

        val updated = tiltakstype.copy(deltakerinfo = command.deltakerinfo)
        repository.tiltakstype.save(updated)

        logEndring("Redigerte informasjon for deltakere", tiltakstype, command.endretAv)
        publishToKafka(updated)
        Unit.right()
    }

    internal fun QueryContext.publishToKafka(tiltakstype: Tiltakstype) {
        if (tiltakstype.tiltakskode.system == TiltakstypeSystem.TILTAKSADMINISTRASJON) {
            val ekstern = checkNotNull(queries.tiltakstype.getEksternTiltakstype(tiltakstype.id))
            outbox.publish(ekstern)
        }
    }

    internal fun QueryContext.logEndring(tekst: String, tiltakstype: Tiltakstype, endretAv: NavIdent) {
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILTAKSTYPE,
            tekst,
            endretAv,
            tiltakstype.id,
            LocalDateTime.now(),
        ) { Json.encodeToJsonElement(tiltakstype) }
    }
}
