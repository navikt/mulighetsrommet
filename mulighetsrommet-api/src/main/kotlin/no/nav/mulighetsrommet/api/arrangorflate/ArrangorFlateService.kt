package no.nav.mulighetsrommet.api.arrangorflate

import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateUtbetalingKompakt
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.api.utbetaling.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.*

class ArrangorFlateService(
    val arrangorService: ArrangorService,
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    val db: ApiDatabase,
) {

    fun getUtbetalinger(orgnr: Organisasjonsnummer): List<ArrFlateUtbetalingKompakt> {
        return db.session {
            queries.utbetaling.getByArrangorIds(orgnr).map {
                if (it.status == UtbetalingStatus.UTBETALT) {
                    return@map ArrFlateUtbetalingKompakt.fromUtbetalingDto(it)
                }
                val forslag = getDeltakerforslagByGjennomforing(it.gjennomforing.id)
                val antallRelevanteForslag = getAntallRelevanteForslag(forslag, it)
                val utbetaling = it.copy(status = if (antallRelevanteForslag > 0) UtbetalingStatus.VENTER_PA_ENDRING else it.status)
                ArrFlateUtbetalingKompakt.fromUtbetalingDto(utbetaling)
            }
        }
    }

    private fun getAntallRelevanteForslag(forslag: Map<UUID, List<DeltakerForslag>>, utbetaling: UtbetalingDto): Int {
        val relevanteForslag = getRelevanteForslag(forslag, utbetaling)
        return relevanteForslag.sumOf { it.antallRelevanteForslag }
    }

    fun getUtbetaling(id: UUID): UtbetalingDto {
        return db.session {
            queries.utbetaling.get(id) ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
        }
    }

    fun getDeltakerforslagByGjennomforing(gjennomforingId: UUID): Map<UUID, List<DeltakerForslag>> {
        return db.session {
            queries.deltakerForslag.getForslagByGjennomforing(gjennomforingId)
        }
    }

    fun getTilsagn(id: UUID): ArrangorflateTilsagn {
        return db.session {
            queries.tilsagn.getArrangorflateTilsagn(id) ?: throw NotFoundException("Fant ikke tilsagn")
        }
    }

    fun getAlleTilsagnForOrganisasjon(orgnr: Organisasjonsnummer): List<ArrangorflateTilsagn> {
        return db.session {
            queries.tilsagn.getAllArrangorflateTilsagn(orgnr)
        }
    }

    fun getRelevanteForslag(forslagByDeltakerId: Map<UUID, List<DeltakerForslag>>, utbetaling: UtbetalingDto): List<RelevanteForslag> {
        return forslagByDeltakerId
            .map { (deltakerId, forslag) ->
                RelevanteForslag(
                    deltakerId = deltakerId,
                    antallRelevanteForslag = forslag.count { it.relevantForDeltakelse(utbetaling) },
                )
            }
    }
}
