package no.nav.mulighetsrommet.api.gjennomforing.service

import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.model.fromPrismodell
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.api.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingHandling
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDtoMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.DeltakerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArenaKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompakt
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompaktDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.services.ExcelWorkbookBuilder
import no.nav.mulighetsrommet.api.services.buildExcelWorkbook
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.auditlog.AuditLog.auditLogger
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

class GjennomforingDetaljerService(
    private val db: ApiDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
    private val personaliaService: PersonaliaService,
) {
    fun getTiltaksgjennomforingV2Dto(id: UUID): TiltaksgjennomforingV2Dto? = db.session {
        val gjennomforing = getGjennomforing(id) ?: return null
        when (gjennomforing) {
            is GjennomforingAvtale -> {
                val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
                TiltaksgjennomforingV2Mapper.fromGjennomforingAvtale(gjennomforing, detaljer)
            }

            is GjennomforingEnkeltplass -> TiltaksgjennomforingV2Mapper.fromGjennomforingEnkeltplass(gjennomforing)

            is GjennomforingArena -> TiltaksgjennomforingV2Mapper.fromGjennomforingArena(gjennomforing)
        }
    }

    suspend fun getGjennomforingDetaljerDto(
        id: UUID,
        accessType: AccessType.OBO.AzureAd,
        navIdent: NavIdent,
    ): GjennomforingDetaljerDto? {
        val gjennomforing = getGjennomforingTiltaksadministrasjon(id) ?: return null
        return when (gjennomforing) {
            is GjennomforingAvtale -> db.session {
                val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
                GjennomforingDtoMapper.fromGjennomforingAvtale(gjennomforing, detaljer)
            }

            is GjennomforingEnkeltplass -> db.session {
                val okonomi = queries.totrinnskontroll.getDto(
                    gjennomforing.id,
                    TotrinnskontrollType.ENKELTPLASS_OKONOMI,
                )

                val prisendring = queries.enkeltplassPrisendring.getByGjennomforingId(gjennomforing.id)
                    ?.let { queries.prismodell.getOrError(it.prismodellId) }
                    ?.let { prismodell ->
                        val totrinnskontroll = queries.totrinnskontroll.getDtoOrError(
                            gjennomforing.id,
                            TotrinnskontrollType.ENKELTPLASS_PRISENDRING,
                        )
                        GjennomforingDetaljerDto.Prisendring(totrinnskontroll, fromPrismodell(prismodell))
                    }

                val deltaker = getDeltaker(gjennomforing.id)?.let { deltaker ->
                    val personalia = personaliaService
                        .getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.NavAnsatt(accessType))
                    val norskIdent = personalia.norskIdent()
                    if (personalia.harTilgang() && norskIdent != null) {
                        auditLogVisEnkeltplass(navIdent, norskIdent)
                    }
                    val veilederNavn = deltaker.navVeileder?.navIdent?.let {
                        navAnsattService.getNavAnsattNavnFromAzure(it, AccessType.M2M)
                    }
                    DeltakerDto.from(deltaker, personalia, veilederNavn)
                }

                val opplaringKategorisering = context(session) {
                    OpplaringKategoriseringQueries.get(gjennomforing.id)
                }

                GjennomforingDtoMapper.fromEnkeltplass(
                    gjennomforing,
                    okonomi,
                    prisendring,
                    deltaker,
                    opplaringKategorisering,
                )
            }
        }
    }

    fun getGjennomforingTiltaksadministrasjon(id: UUID): GjennomforingTiltaksadministrasjon? = db.session {
        val gjennomforing = getGjennomforing(id) ?: return null
        when (gjennomforing) {
            is GjennomforingTiltaksadministrasjon -> gjennomforing
            is GjennomforingArena -> throw IllegalStateException("Visning av gamle gjennomføringer fra Arena er ikke støttet")
        }
    }

    fun getAllKompaktDto(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<GjennomforingKompaktDto> = db.session {
        val tiltakstyper = tiltakstypeService.getIdsByTiltakskoder(filter.tiltakskoder).ifEmpty {
            tiltakstypeService.getIdsByFeatures(setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON))
        }
        queries.gjennomforing.getAll(
            pagination,
            search = filter.search?.let { NorskIdentHasher.hashIfNorskIdent(it) },
            navEnheter = filter.navEnheter,
            tiltakstyper = tiltakstyper,
            statuser = filter.statuser,
            sortering = filter.sortering,
            avtaleId = filter.avtaleId,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            koordinatorNavIdent = filter.koordinatorNavIdent,
            publisert = filter.publisert,
            typer = filter.gjennomforingTyper.ifEmpty {
                listOf(GjennomforingType.AVTALE, GjennomforingType.ENKELTPLASS)
            },
        ).let { (totalCount, items) ->
            val data = items.map { it.toKompaktDto() }
            PaginatedResponse.of(pagination, totalCount, data)
        }
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker? {
        val deltakelser = queries.deltaker.getByGjennomforingId(gjennomforingId)
        if (deltakelser.size > 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet kun én)")
        }
        return deltakelser.firstOrNull()
    }

    fun exportToExcel(
        filter: AdminTiltaksgjennomforingFilter,
    ): File {
        val result = getAllKompaktDto(Pagination.all(), filter)

        val workbook = buildExcelWorkbook {
            createGjennomforingerSheet(result.data)
        }

        return workbook.use {
            val file = createTempFile("gjennomforinger-", ".xlsx")
            file.outputStream().use(it::write)
            file.toFile()
        }
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<GjennomforingHandling> {
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return setOf()
        val gjennomforing = db.session { getGjennomforing(id) } ?: return setOf()
        return when (gjennomforing) {
            is GjennomforingAvtale -> getHandlingerAvtale(gjennomforing, ansatt)
            is GjennomforingEnkeltplass -> getHandlingerEnkeltplass(gjennomforing, ansatt)
            is GjennomforingArena -> setOf()
        }
    }

    private fun QueryContext.getGjennomforing(id: UUID): Gjennomforing? {
        return queries.gjennomforing.getGjennomforing(id)
    }

    private fun getHandlingerAvtale(
        gjennomforing: GjennomforingAvtale,
        ansatt: NavAnsatt,
    ): Set<GjennomforingHandling> {
        val statusGjennomfores = gjennomforing.status == GjennomforingStatusType.GJENNOMFORES
        return setOfNotNull(
            GjennomforingHandling.DUPLISER.takeIf {
                !tiltakstypeService.erUtfaset(gjennomforing.tiltakstype.tiltakskode)
            },
            GjennomforingHandling.PUBLISER.takeIf { statusGjennomfores },
            GjennomforingHandling.FORHANDSVIS_I_MODIA.takeIf { statusGjennomfores },
            GjennomforingHandling.AVBRYT.takeIf { statusGjennomfores },
            GjennomforingHandling.GJENAPNE.takeIf { gjennomforing.status == GjennomforingStatusType.AVSLUTTET },
            GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING.takeIf { statusGjennomfores },
            GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR.takeIf { statusGjennomfores },
            GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR.takeIf { statusGjennomfores },
            GjennomforingHandling.REGISTRER_ESTIMERT_VENTETID.takeIf { statusGjennomfores },
            GjennomforingHandling.REDIGER.takeIf { statusGjennomfores },
            GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER.takeIf {
                gjennomforing.tiltakstype.tiltakskode.harEgenskap(TiltakstypeEgenskap.STOTTER_TILSKUDD_FOR_INVESTERINGER)
            },
            // FIXME: midlertidig hack for å tillate "Opprett utbetaling" for utenlandske bedifter
            GjennomforingHandling.OPPRETT_UTBETALING.takeIf {
                gjennomforing.arrangor.organisasjonsnummer.value.startsWith("1")
            },
            GjennomforingHandling.OPPRETT_TILSAGN,
            GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
        )
            .filter { tilgangTilHandling(ansatt, it) }
            .toSet()
    }

    private fun getHandlingerEnkeltplass(
        gjennomforing: GjennomforingEnkeltplass,
        ansatt: NavAnsatt,
    ): Set<GjennomforingHandling> {
        val okonomi = db.session {
            queries.totrinnskontroll.get(gjennomforing.id, TotrinnskontrollType.ENKELTPLASS_OKONOMI)
        }
        val prisendring = db.session {
            queries.totrinnskontroll.get(gjennomforing.id, TotrinnskontrollType.ENKELTPLASS_PRISENDRING)
        }
        return setOfNotNull(
            GjennomforingHandling.OPPRETT_TILSAGN,
            GjennomforingHandling.OPPRETT_UTBETALING,
            GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI.takeIf {
                okonomi?.kanSettesPaVent() == true
            },
            GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI.takeIf {
                okonomi?.kanBesluttes() == true
            },
            GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_PRISENDRING.takeIf {
                prisendring?.kanSettesPaVent() == true
            },
            GjennomforingHandling.GODKJENN_ENKELTPLASS_PRISENDRING.takeIf {
                prisendring?.kanBesluttes() == true
            },
        )
            .filter { tilgangTilHandling(ansatt, it, setOf(gjennomforing.ansvarligEnhet.enhetsnummer)) }
            .toSet()
    }

    companion object {
        fun tilgangTilHandling(
            ansatt: NavAnsatt,
            handling: GjennomforingHandling,
            enheter: Set<NavEnhetNummer> = setOf(),
        ): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            val oppfolgerGjennomforing = ansatt.hasGenerellRolle(Rolle.OPPFOLGER_GJENNOMFORING)
            val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                GjennomforingHandling.FORHANDSVIS_I_MODIA -> true

                GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING,
                GjennomforingHandling.ENDRE_TILGJENGELIG_FOR_ARRANGOR,
                GjennomforingHandling.REGISTRER_ESTIMERT_VENTETID,
                -> skrivGjennomforing || oppfolgerGjennomforing

                GjennomforingHandling.PUBLISER,
                GjennomforingHandling.AVBRYT,
                GjennomforingHandling.GJENAPNE,
                GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR,
                GjennomforingHandling.DUPLISER,
                GjennomforingHandling.REDIGER,
                -> skrivGjennomforing

                GjennomforingHandling.OPPRETT_TILSAGN,
                GjennomforingHandling.OPPRETT_EKSTRATILSAGN,
                GjennomforingHandling.OPPRETT_TILSAGN_FOR_INVESTERINGER,
                GjennomforingHandling.OPPRETT_UTBETALING,
                -> saksbehandlerOkonomi

                GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
                GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_PRISENDRING,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_PRISENDRING,
                -> ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, enheter)
            }
        }
    }
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
        type = GjennomforingType.AVTALE,
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
        type = GjennomforingType.ENKELTPLASS,
    )

    is GjennomforingArenaKompakt -> throw IllegalStateException("Visning av gamle gjennomføringer fra Arena er ikke støttet")
}

private fun ExcelWorkbookBuilder.createGjennomforingerSheet(
    result: List<GjennomforingKompaktDto>,
) = table("Gjennomforinger") {
    header(
        "Tiltaksnavn",
        "Tiltakstype",
        "Tiltaksnummer",
        "Tiltaksarrangør",
        "Tiltaksarrangør orgnr",
        "Startdato",
        "Sluttdato",
    )

    result.forEach { tiltak ->
        row {
            listOf(
                tiltak.navn,
                tiltak.tiltakstype.navn,
                tiltak.lopenummer.value,
                tiltak.arrangor.navn,
                tiltak.arrangor.organisasjonsnummer.value,
                tiltak.startDato?.formaterDatoTilEuropeiskDatoformat(),
                tiltak.sluttDato?.formaterDatoTilEuropeiskDatoformat(),
            )
        }
    }
}

private fun auditLogVisEnkeltplass(navIdent: NavIdent, norskIdent: NorskIdent) {
    val message = CefMessage.builder()
        .applicationName("Tiltaksadministrasjon")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.ACCESS)
        .name("Tiltaksadministrasjon - Se enkeltplassdeltaker")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navIdent.value)
        .destinationUserId(norskIdent.value)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", "Nav-ansatt med ident: '$navIdent' har sett på enkeltplassdeltaker med ident: '$norskIdent'.")
        .build()
    auditLogger.log(message)
}
