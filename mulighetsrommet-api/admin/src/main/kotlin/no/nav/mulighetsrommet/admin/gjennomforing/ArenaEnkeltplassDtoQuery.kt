package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.Either
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserError
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserPort
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattContext
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.validation.FieldError
import java.util.UUID

data class FinnArenaEnkeltplassInput(val search: String)

sealed interface FinnArenaEnkeltplassError {
    data class Valideringsfeil(val errors: List<FieldError>) : FinnArenaEnkeltplassError
    data class ManglerTilgang(val avvistGrunn: AvvistGrunn? = null) : FinnArenaEnkeltplassError
    data object OppslagFeilet : FinnArenaEnkeltplassError
}

class ArenaEnkeltplassDtoQuery(
    private val db: AdminDatabase,
    private val tiltakstyper: TiltakstypeService,
    private val detaljer: GjennomforingDetaljerService,
    private val deltakelser: DeltakelserPort,
) {
    suspend fun execute(
        input: FinnArenaEnkeltplassInput,
        context: NavAnsattContext,
    ): Either<FinnArenaEnkeltplassError, List<ArenaEnkeltplassDto>> = either {
        val ident = NorskIdent.parse(input.search)
        val kandidater = if (ident != null) {
            finnMigreringskandidaterForBruker(ident, context)
        } else {
            finnMigreringskandidaterForTiltaksnummer(input)
        }
        kandidater.mapNotNull { (gjennomforing, deltaker, ansvarligEnhet) ->
            val deltakerDto = detaljer.findDeltakerDto(gjennomforing.id, context.accessType, context.navIdent)
                .mapLeft { FinnArenaEnkeltplassError.ManglerTilgang(it.avvistGrunn) }.bind()
                ?: return@mapNotNull null
            ArenaEnkeltplassDto(
                id = gjennomforing.id,
                tiltaksnummer = requireNotNull(gjennomforing.arena).tiltaksnummer,
                navn = gjennomforing.navn,
                tiltakstype = gjennomforing.tiltakstype,
                arrangor = gjennomforing.arrangor,
                ansvarligEnhet = ansvarligEnhet,
                deltakerStatus = deltaker.status.type,
                startDato = deltaker.startDato,
                sluttDato = deltaker.sluttDato,
                deltaker = deltakerDto,
            )
        }
    }

    private suspend fun Raise<FinnArenaEnkeltplassError>.finnMigreringskandidaterForBruker(
        ident: NorskIdent,
        context: NavAnsattContext,
    ): List<Migreringskandidat> {
        val deltakerIds = deltakelser.finnDeltakerIds(ident, context.accessType).mapLeft { error ->
            when (error) {
                is DeltakelserError.ManglerTilgang -> FinnArenaEnkeltplassError.ManglerTilgang(error.avvistGrunn)
                DeltakelserError.OppslagFeilet -> FinnArenaEnkeltplassError.OppslagFeilet
            }
        }.bind()
        return db.session {
            deltakerIds.mapNotNull { repository.deltaker.get(it)?.gjennomforingId }
                .mapNotNull { finnMigreringskandidat(it) }
        }
    }

    private fun Raise<FinnArenaEnkeltplassError>.finnMigreringskandidaterForTiltaksnummer(
        input: FinnArenaEnkeltplassInput,
    ): List<Migreringskandidat> {
        val tiltaksnummer = runCatching { Tiltaksnummer(input.search.trim()) }.getOrNull()
            ?: raise(
                FinnArenaEnkeltplassError.Valideringsfeil(
                    FieldError
                        .of("Oppgi et tiltaksnummer (for eksempel 2026/1234) eller et fødselsnummer på 11 siffer")
                        .nel(),
                ),
            )
        return db.session {
            queries.gjennomforing.findIdsByArenaTiltaksnummer(tiltaksnummer)
                .mapNotNull { finnMigreringskandidat(it) }
        }
    }

    private fun QueryContext.finnMigreringskandidat(id: UUID): Migreringskandidat? {
        val gjennomforing = repository.gjennomforing.getGjennomforing(id) as? GjennomforingArena
            ?: return null
        return validerMigreringskandidat(gjennomforing, tiltakstyper).getOrNull()
    }
}
