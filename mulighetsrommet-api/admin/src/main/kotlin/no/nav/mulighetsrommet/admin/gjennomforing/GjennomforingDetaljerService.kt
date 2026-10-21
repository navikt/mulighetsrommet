package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.either
import arrow.core.right
import arrow.core.toNonEmptySetOrThrow
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.admin.personalia.PersonaliaPort
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.admin.tiltak.toPrismodellDto
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArenaKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.model.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.spreadsheet.ExcelWorkbookBuilder
import no.nav.mulighetsrommet.spreadsheet.buildExcelWorkbook
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

/** Audit-logg ved oppslag på personopplysninger. Implementeres i server. */
interface GjennomforingAuditLog {
    fun visEnkeltplassDeltaker(gjennomforingId: UUID, navIdent: NavIdent, norskIdent: NorskIdent)
}

class GjennomforingDetaljerService(
    private val db: AdminDatabase,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
    private val personaliaPort: PersonaliaPort,
    private val enkeltplassService: GjennomforingEnkeltplassService,
    private val auditLog: GjennomforingAuditLog,
) {
    fun getTiltaksgjennomforingV2Dto(id: UUID): TiltaksgjennomforingV2Dto? = db.session {
        val gjennomforing = getGjennomforing(id) ?: return@session null
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
    ): Either<GjennomforingDetaljerError, GjennomforingDetaljerDto> = either {
        val gjennomforing = getGjennomforingTiltaksadministrasjon(id)
            ?: return GjennomforingDetaljerError.IkkeFunnet("Gjennomføring med id=$id finnes ikke").left()

        return when (gjennomforing) {
            is GjennomforingAvtale -> db.session {
                val detaljer = queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
                GjennomforingDtoMapper.fromGjennomforingAvtale(gjennomforing, detaljer).right()
            }

            is GjennomforingEnkeltplass -> {
                val ansatt = db.session { repository.navAnsatt.getOrError(navIdent) }
                if (!OkonomiAuthorization.kanLeseEnkeltplass(ansatt)) {
                    return GjennomforingDetaljerError.ManglerTilgang("Du mangler lesetilgang til enkeltplass").left()
                }
                val enkeltplass = enkeltplassService.get(gjennomforing.id)
                    ?: error("Fant ikke enkeltplass med id=${gjennomforing.id}")

                val (okonomi, prisendring, deltaker) = db.session {
                    val okonomi = enkeltplass.okonomi?.let { queries.totrinnskontroll.getDtoByIdOrError(it.id) }

                    val prisendring = enkeltplass.prisendring?.let {
                        val totrinnskontroll = queries.totrinnskontroll.getDtoByIdOrError(it.totrinnskontroll.id)
                        GjennomforingEnkeltplassDetaljerDto.Prisendring(
                            totrinnskontroll,
                            it.prismodell.toPrismodellDto(),
                        )
                    }

                    Triple(okonomi, prisendring, getDeltaker(gjennomforing.id))
                }

                val deltakerDto = deltaker?.let { deltaker ->
                    val personalia = personaliaPort.getPersonalia(deltaker.id, accessType)
                    val norskIdent = personalia.norskIdent()
                    val avvistGrunn = personalia.avvistGrunn
                    if (avvistGrunn != null) {
                        return GjennomforingDetaljerError.PersonaliaSjekkAvvist(avvistGrunn).left()
                    } else if (norskIdent != null) {
                        auditLog.visEnkeltplassDeltaker(gjennomforing.id, navIdent, norskIdent)
                    }
                    val veilederNavn = deltaker.navVeileder?.navIdent?.let {
                        navAnsattService.getNavAnsattNavnFromAzure(it, AccessType.M2M)
                    }
                    DeltakerDto.from(deltaker, personalia, veilederNavn, dato = LocalDate.now())
                }

                val opplaringKategorisering = db.session { queries.opplaering.get(gjennomforing.id) }

                GjennomforingDtoMapper.fromEnkeltplass(
                    gjennomforing,
                    okonomi,
                    prisendring,
                    deltakerDto,
                    opplaringKategorisering,
                ).right()
            }
        }
    }

    fun getGjennomforingTiltaksadministrasjon(id: UUID): GjennomforingTiltaksadministrasjon? = db.session {
        val gjennomforing = getGjennomforing(id) ?: return@session null
        when (gjennomforing) {
            is GjennomforingTiltaksadministrasjon -> gjennomforing
            is GjennomforingArena -> throw IllegalStateException("Visning av gamle gjennomføringer fra Arena er ikke støttet")
        }
    }

    suspend fun getAllKompaktDto(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
        accessType: AccessType.OBO.AzureAd,
    ): Either<AvvistGrunn, PaginatedResult<GjennomforingKompaktDto>> {
        val avvistGrunn = filter.search?.let { NorskIdent.parse(it) }?.let {
            personaliaPort.navAnsattTilgangTilPerson(it, accessType)
        }
        if (avvistGrunn != null) {
            return avvistGrunn.left()
        }
        val tiltakstyper = tiltakstypeService.getIdsByTiltakskoder(filter.tiltakskoder).ifEmpty {
            tiltakstypeService.getIdsByFeatures(setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON))
        }
        return db.session {
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
                typer = listOf(GjennomforingType.AVTALE, GjennomforingType.ENKELTPLASS),
            ).let { (totalCount, items) ->
                PaginatedResult(totalCount, items.map { it.toKompaktDto() })
            }
        }.right()
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker? {
        val deltakelser = repository.deltaker.getByGjennomforing(gjennomforingId)
        if (deltakelser.size > 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet kun én)")
        }
        return deltakelser.firstOrNull()
    }

    suspend fun exportToExcel(
        filter: AdminTiltaksgjennomforingFilter,
        accessType: AccessType.OBO.AzureAd,
    ): Either<AvvistGrunn, File> {
        return getAllKompaktDto(Pagination.all(), filter, accessType)
            .map {
                val workbook = buildExcelWorkbook {
                    createGjennomforingerSheet(it.items)
                }

                workbook.use {
                    val file = createTempFile("gjennomforinger-", ".xlsx")
                    file.outputStream().use(it::write)
                    file.toFile()
                }
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
        return repository.gjennomforing.getGjennomforing(id)
    }

    private fun getHandlingerAvtale(
        gjennomforing: GjennomforingAvtale,
        ansatt: NavAnsatt,
    ): Set<GjennomforingHandling> {
        val statusGjennomfores = gjennomforing.status is GjennomforingAvtaleStatus.Gjennomfores
        return setOfNotNull(
            GjennomforingHandling.DUPLISER.takeIf {
                !tiltakstypeService.erUtfaset(gjennomforing.tiltakstype.tiltakskode)
            },
            GjennomforingHandling.PUBLISER.takeIf { statusGjennomfores },
            GjennomforingHandling.FORHANDSVIS_I_MODIA.takeIf { statusGjennomfores },
            GjennomforingHandling.AVBRYT.takeIf { statusGjennomfores },
            GjennomforingHandling.GJENAPNE.takeIf { gjennomforing.status is GjennomforingAvtaleStatus.Avsluttet },
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
            .filter { handling ->
                tilgangTilHandling(ansatt, handling, GjennomforingType.AVTALE)
            }
            .toSet()
    }

    private fun getHandlingerEnkeltplass(
        gjennomforing: GjennomforingEnkeltplass,
        ansatt: NavAnsatt,
    ): Set<GjennomforingHandling> {
        val enkeltplass = enkeltplassService.get(gjennomforing.id)
        val okonomi = enkeltplass?.okonomi
        val prisendring = enkeltplass?.prisendring?.totrinnskontroll

        val okonomiKanBehandlesAvNavAnsatt = okonomi?.kanBehandlesAv(ansatt.navIdent) == true
        val prisendringKanBehandlesAvNavAnsatt = prisendring?.kanBehandlesAv(ansatt.navIdent) == true
        return setOfNotNull(
            GjennomforingHandling.OPPRETT_TILSAGN,
            GjennomforingHandling.OPPRETT_UTBETALING,
            GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI.takeIf {
                okonomiKanBehandlesAvNavAnsatt && okonomi.kanSettesPaVent()
            },
            GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI.takeIf {
                okonomiKanBehandlesAvNavAnsatt && okonomi.kanBesluttes()
            },
            GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_PRISENDRING.takeIf {
                prisendringKanBehandlesAvNavAnsatt && prisendring.kanSettesPaVent()
            },
            GjennomforingHandling.GODKJENN_ENKELTPLASS_PRISENDRING.takeIf {
                prisendringKanBehandlesAvNavAnsatt && prisendring.kanBesluttes()
            },
        )
            .filter { handling ->
                val kostnadssted = setOf(gjennomforing.ansvarligEnhet.enhetsnummer)
                tilgangTilHandling(ansatt, handling, GjennomforingType.ENKELTPLASS, kostnadssted)
            }
            .toSet()
    }

    companion object {
        fun tilgangTilHandling(
            ansatt: NavAnsatt,
            handling: GjennomforingHandling,
            gjennomforingType: GjennomforingType,
            enheter: Set<NavEnhetNummer> = setOf(),
        ): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            val oppfolgerGjennomforing = ansatt.hasGenerellRolle(Rolle.OPPFOLGER_GJENNOMFORING)

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
                -> OkonomiAuthorization.erSaksbehandler(ansatt, gjennomforingType)

                GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
                GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_PRISENDRING,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_PRISENDRING,
                -> OkonomiAuthorization.erOkonomiBeslutter(
                    ansatt,
                    OkonomiBeslutningContext(gjennomforingType, enheter.toNonEmptySetOrThrow()),
                )
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
        status = GjennomforingDtoMapper.fromGjennomforingAvtaleStatus(status),
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
        status = GjennomforingDtoMapper.fromEnkeltplassStatus(status),
        arrangor = arrangor,
        tiltakstype = tiltakstype,
        publisert = false,
        kontorstruktur = kontorstruktur,
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
        "Løpenummer",
        "Tiltaksarrangør",
        "Tiltaksarrangør orgnr",
        "Startdato",
        "Sluttdato",
        "GjennomføringId",
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
                tiltak.id,
            )
        }
    }
}

sealed class GjennomforingDetaljerError {
    data class IkkeFunnet(val message: String) : GjennomforingDetaljerError()
    data class ManglerTilgang(val message: String) : GjennomforingDetaljerError()
    data class PersonaliaSjekkAvvist(val avvistGrunn: AvvistGrunn) : GjennomforingDetaljerError()
}
