package no.nav.mulighetsrommet.api.gjennomforing.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.api.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDtoMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompaktDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeFilter
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import java.util.UUID

class GjennomforingDetaljerService(
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
) {
    fun get(id: UUID): GjennomforingDetaljerDto? = db.session {
        getGjennomforing(id)?.let { GjennomforingDtoMapper.fromGjennomforing(it) }
    }

    fun getAll(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<GjennomforingKompaktDto> = db.session {
        val tiltakstyper = filter.tiltakstypeIder.ifEmpty {
            tiltakstypeService
                .getAll(TiltakstypeFilter(features = setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON)))
                .map { it.id }
        }
        queries.gjennomforing.getAll(
            pagination,
            search = filter.search,
            navEnheter = filter.navEnheter,
            tiltakstypeIder = tiltakstyper,
            statuser = filter.statuser,
            sortering = filter.sortering,
            avtaleId = filter.avtaleId,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            koordinatorNavIdent = filter.koordinatorNavIdent,
            publisert = filter.publisert,
            sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
        ).let { (totalCount, items) ->
            val data = items.map { it.toKompaktDto() }
            PaginatedResponse.of(pagination, totalCount, data)
        }
    }

    fun handlinger(id: UUID, navIdent: NavIdent): Set<GjennomforingHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        val gjennomforing = getGjennomforing(id) ?: return setOf()
        return when (gjennomforing) {
            is GjennomforingAvtale -> getHandlingerGruppetiltak(gjennomforing, ansatt)
            is GjennomforingEnkeltplass -> setOf()
        }
    }

    private fun getGjennomforing(id: UUID): Gjennomforing? = db.session {
        queries.gjennomforing.getGjennomforingAvtale(id) ?: queries.gjennomforing.getGjennomforingEnkeltplass(id)
    }

    companion object {
        fun tilgangTilHandling(handling: GjennomforingHandling, ansatt: NavAnsatt): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            val oppfolgerGjennomforing = ansatt.hasGenerellRolle(Rolle.OPPFOLGER_GJENNOMFORING)
            val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                GjennomforingHandling.PUBLISER -> skrivGjennomforing
                GjennomforingHandling.AVBRYT -> skrivGjennomforing
                GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING -> skrivGjennomforing || oppfolgerGjennomforing
                GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR -> skrivGjennomforing || oppfolgerGjennomforing
                GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR -> skrivGjennomforing
                GjennomforingHandling.DUPLISER -> skrivGjennomforing
                GjennomforingHandling.REDIGER -> skrivGjennomforing
                GjennomforingHandling.OPPRETT_TILSAGN -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_EKSTRATILSAGN -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER -> saksbehandlerOkonomi
                GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING -> saksbehandlerOkonomi
                GjennomforingHandling.FORHANDSVIS_I_MODIA -> true
            }
        }
    }
}

private fun getHandlingerGruppetiltak(
    gjennomforing: GjennomforingAvtale,
    ansatt: NavAnsatt,
): Set<GjennomforingHandling> {
    val statusGjennomfores = gjennomforing.status is GjennomforingStatus.Gjennomfores
    return setOfNotNull(
        GjennomforingHandling.PUBLISER.takeIf { statusGjennomfores },
        GjennomforingHandling.FORHANDSVIS_I_MODIA.takeIf { statusGjennomfores },
        GjennomforingHandling.AVBRYT.takeIf { statusGjennomfores },
        GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING.takeIf { statusGjennomfores },
        GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR.takeIf { statusGjennomfores },
        GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR.takeIf { statusGjennomfores },
        GjennomforingHandling.REDIGER.takeIf { statusGjennomfores },
        GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER.takeIf {
            gjennomforing.tiltakstype.tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER)
        },

        GjennomforingHandling.DUPLISER,
        GjennomforingHandling.OPPRETT_KORREKSJON_PA_UTBETALING,
        GjennomforingHandling.OPPRETT_TILSAGN,
        GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
    )
        .filter { GjennomforingDetaljerService.tilgangTilHandling(it, ansatt) }
        .toSet()
}

private fun GjennomforingKompakt.toKompaktDto(): GjennomforingKompaktDto = when (this) {
    is GjennomforingAvtaleKompakt -> GjennomforingKompaktDto(
        id = id,
        navn = navn,
        lopenummer = lopenummer,
        startDato = startDato,
        sluttDato = sluttDato,
        status = GjennomforingDtoMapper.fromGjennomforingStatus(status),
        arrangor = arrangor,
        tiltakstype = tiltakstype,
        publisert = publisert,
        kontorstruktur = kontorstruktur,
    )

    is GjennomforingEnkeltplassKompakt -> GjennomforingKompaktDto(
        id = id,
        navn = tiltakstype.navn,
        lopenummer = lopenummer,
        startDato = startDato,
        sluttDato = sluttDato,
        status = GjennomforingDtoMapper.fromGjennomforingStatus(status),
        arrangor = arrangor,
        tiltakstype = tiltakstype,
        publisert = false,
        kontorstruktur = listOf(),
    )
}
