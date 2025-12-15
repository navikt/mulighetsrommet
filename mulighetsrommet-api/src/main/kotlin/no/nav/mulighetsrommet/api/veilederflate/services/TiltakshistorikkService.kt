package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseEierskap
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelsePamelding
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseStatus
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseTilstand
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.ArbeidsgiverAvtaleStatus
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.TiltakshistorikkClient
import no.nav.tiltak.historikk.TiltakshistorikkMelding
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakshistorikkService(
    private val historiskeIdenterQuery: HentHistoriskeIdenterPdlQuery,
    private val tiltakstypeService: TiltakstypeService,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tiltakshistorikkClient: TiltakshistorikkClient,
    private val features: FeatureToggleService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun hentHistorikk(norskIdent: NorskIdent, obo: AccessType.OBO): Deltakelser = coroutineScope {
        val tiltakshistorikk = async { getTiltakshistorikk(norskIdent, obo) }
        val deltakelserFraKomet = async { getDeltakelserKomet(norskIdent, obo) }
        deltakelserFraKomet.await()
            .mergeWith(tiltakshistorikk.await())
            .filter(::isEnabled)
    }

    private suspend fun getTiltakshistorikk(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Deltakelser = coroutineScope {
        val identer = hentHistoriskeNorskIdent(norskIdent, obo)
        tiltakshistorikkClient.getHistorikk(identer).fold(
            { error ->
                log.warn("Feil oppstå ved henting av tiltakshistorikken: $error")
                Deltakelser(
                    meldinger = setOf(DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TILTAKSHISTORIKKEN),
                    aktive = listOf(),
                    historiske = listOf(),
                )
            },
            { response ->
                val meldinger = response.meldinger
                    .map {
                        when (it) {
                            TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK -> DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK
                        }
                    }
                    .toSet()

                val (aktive, historiske) = response.historikk
                    .map { async { toDeltakelse(it) } }
                    .awaitAll()
                    .partition { erAktiv(it.tilstand) }

                Deltakelser(
                    meldinger = meldinger,
                    aktive = aktive,
                    historiske = historiske,
                )
            },
        )
    }

    suspend fun getDeltakelserKomet(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Deltakelser {
        return amtDeltakerClient.hentDeltakelser(DeltakelserRequest(norskIdent), obo).fold({ error ->
            log.warn("Klarte ikke hente deltakelser fra Komet: $error")
            Deltakelser(
                meldinger = setOf(DeltakelserMelding.MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET),
                aktive = emptyList(),
                historiske = emptyList(),
            )
        }, { response ->
            Deltakelser(
                meldinger = setOf(),
                aktive = response.aktive.map { toDeltakelse(it) },
                historiske = response.historikk.map { toDeltakelse(it) },
            )
        })
    }

    private fun toDeltakelse(it: TiltakshistorikkV1Dto): Deltakelse = when (it) {
        is TiltakshistorikkV1Dto.ArenaDeltakelse -> toDeltakelse(it)
        is TiltakshistorikkV1Dto.TeamKometDeltakelse -> toDeltakelse(it)
        is TiltakshistorikkV1Dto.TeamTiltakAvtale -> toDeltakelse(it)
    }

    private fun toDeltakelse(deltakelse: TiltakshistorikkV1Dto.ArenaDeltakelse): Deltakelse {
        // TODO: fjerne ekstra sjekk mot tiltakstypeService etter at feature toggle for enkeltplasser er borte
        //  `deltakelse.tiltakstype` er egentlig nok info, men foreløpig må vi gjøre et oppslag for å tiltakskoden
        //  som feature toggle er definert for
        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(deltakelse.tiltakstype.tiltakskode)
            ?.let { DeltakelseTiltakstype(it.navn, it.tiltakskode) }
            ?: DeltakelseTiltakstype(deltakelse.tiltakstype.navn, null)
        return Deltakelse(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseStatus(
                type = deltakelse.status.toDataElement(),
                aarsak = null,
            ),
            tittel = deltakelse.tittel,
            tiltakstype = tiltakstype,
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakelseEierskap.ARENA,
            tilstand = getTilstand(deltakelse.status),
            pamelding = null,
        )
    }

    private fun toDeltakelse(deltakelse: TiltakshistorikkV1Dto.TeamKometDeltakelse): Deltakelse {
        return Deltakelse(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseStatus(
                type = deltakelse.status.type.toDataElement(),
                aarsak = deltakelse.status.aarsak?.description,
            ),
            tittel = deltakelse.tittel,
            tiltakstype = DeltakelseTiltakstype(deltakelse.tiltakstype.navn, deltakelse.tiltakstype.tiltakskode),
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakelseEierskap.TEAM_KOMET,
            tilstand = getTilstand(deltakelse.status.type),
            // Vi inkluderer ikke info om påmelding før deltakelsen er tilgjengelig fra [AmtDeltakerClient.hentDeltakelser]
            pamelding = null,
        )
    }

    private fun toDeltakelse(deltakelse: TiltakshistorikkV1Dto.TeamTiltakAvtale): Deltakelse {
        return Deltakelse(
            id = deltakelse.id,
            periode = DeltakelsePeriode(
                startDato = deltakelse.startDato,
                sluttDato = deltakelse.sluttDato,
            ),
            status = DeltakelseStatus(
                type = deltakelse.status.toDataElement(),
                aarsak = null,
            ),
            tittel = deltakelse.tittel,
            tiltakstype = DeltakelseTiltakstype(deltakelse.tiltakstype.navn, null),
            innsoktDato = null,
            sistEndretDato = null,
            eierskap = DeltakelseEierskap.TEAM_TILTAK,
            tilstand = getTilstand(deltakelse.status),
            pamelding = null,
        )
    }

    private fun toDeltakelse(deltakelse: DeltakelseFraKomet): Deltakelse {
        // TODO: ideelt sett hadde vi fått tiltakskode i stedet for arenakode fra komet
        //  (og enda mer ideelt sett hadde vi ikke trengt å kalle på dette endepunktet i det hele tatt, men kun benyttet
        //  `tiltakshistorikk`-appen som kilde)
        val tiltakstype = tiltakstypeService.getByArenaTiltakskode(deltakelse.tiltakstype.tiltakskode)
            ?.let { DeltakelseTiltakstype(it.navn, it.tiltakskode) }
            ?: throw IllegalStateException("Arena-tiltakskode finnes ikke i db=${deltakelse.tiltakstype.tiltakskode}")

        val tilstand = getTilstand(deltakelse.status.type)
        val pamelding = if (erAktiv(tilstand) && Tiltakskoder.isGruppetiltak(deltakelse.tiltakstype.tiltakskode)) {
            DeltakelsePamelding(deltakelse.deltakerlisteId, deltakelse.status.type)
        } else {
            null
        }
        return Deltakelse(
            id = deltakelse.deltakerId,
            periode = DeltakelsePeriode(
                startDato = deltakelse.periode?.startdato,
                sluttDato = deltakelse.periode?.sluttdato,
            ),
            eierskap = DeltakelseEierskap.TEAM_KOMET,
            tilstand = tilstand,
            tittel = deltakelse.tittel,
            tiltakstype = tiltakstype,
            status = DeltakelseStatus(
                type = deltakelse.status.type.toDataElement(),
                aarsak = deltakelse.status.aarsak,
            ),
            innsoktDato = deltakelse.innsoktDato,
            sistEndretDato = deltakelse.sistEndretDato,
            pamelding = pamelding,
        )
    }

    private fun isEnabled(deltakelse: Deltakelse): Boolean {
        val tiltakskode = deltakelse.tiltakstype.tiltakskode ?: return true

        if (!Tiltakskoder.isEnkeltplassTiltak(tiltakskode)) {
            return true
        }

        return when (deltakelse.eierskap) {
            DeltakelseEierskap.TEAM_TILTAK -> true

            DeltakelseEierskap.ARENA -> !features.isEnabledForTiltakstype(
                FeatureToggle.TILTAKSHISTORIKK_VIS_KOMET_ENKELTPLASSER,
                tiltakskode,
            )

            DeltakelseEierskap.TEAM_KOMET -> features.isEnabledForTiltakstype(
                FeatureToggle.TILTAKSHISTORIKK_VIS_KOMET_ENKELTPLASSER,
                tiltakskode,
            )
        }
    }

    private fun erAktiv(tilstand: DeltakelseTilstand): Boolean = when (tilstand) {
        DeltakelseTilstand.KLADD,
        DeltakelseTilstand.UTKAST,
        DeltakelseTilstand.AKTIV,
        -> true

        DeltakelseTilstand.AVSLUTTET,
        -> false
    }

    private suspend fun hentHistoriskeNorskIdent(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): List<NorskIdent> {
        val request = GraphqlRequest.HentHistoriskeIdenter(
            ident = PdlIdent(norskIdent.value),
            grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
        )
        return historiskeIdenterQuery.hentHistoriskeIdenter(request, obo)
            .map { identer -> identer.map { NorskIdent(it.ident.value) } }
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw Exception("Feil mot pdl!")
                    PdlError.NotFound -> listOf(norskIdent)
                }
            }
    }
}

private fun getTilstand(type: ArenaDeltakerStatus): DeltakelseTilstand = when (type) {
    ArenaDeltakerStatus.AKTUELL,
    ArenaDeltakerStatus.VENTELISTE,
    ArenaDeltakerStatus.TILBUD,
    ArenaDeltakerStatus.GJENNOMFORES,
    ArenaDeltakerStatus.INFORMASJONSMOTE,
    ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD,
    -> DeltakelseTilstand.AKTIV

    ArenaDeltakerStatus.AVSLAG,
    ArenaDeltakerStatus.DELTAKELSE_AVBRUTT,
    ArenaDeltakerStatus.FEILREGISTRERT,
    ArenaDeltakerStatus.FULLFORT,
    ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT,
    ArenaDeltakerStatus.GJENNOMFORING_AVLYST,
    ArenaDeltakerStatus.IKKE_AKTUELL,
    ArenaDeltakerStatus.IKKE_MOTT,
    ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD,
    -> DeltakelseTilstand.AVSLUTTET
}

private fun getTilstand(type: DeltakerStatusType): DeltakelseTilstand = when (type) {
    DeltakerStatusType.KLADD,
    -> DeltakelseTilstand.KLADD

    DeltakerStatusType.UTKAST_TIL_PAMELDING,
    DeltakerStatusType.PABEGYNT_REGISTRERING,
    -> DeltakelseTilstand.UTKAST

    DeltakerStatusType.VENTER_PA_OPPSTART,
    DeltakerStatusType.DELTAR,
    DeltakerStatusType.VURDERES,
    DeltakerStatusType.VENTELISTE,
    DeltakerStatusType.SOKT_INN,
    -> DeltakelseTilstand.AKTIV

    DeltakerStatusType.AVBRUTT,
    DeltakerStatusType.AVBRUTT_UTKAST,
    DeltakerStatusType.FEILREGISTRERT,
    DeltakerStatusType.FULLFORT,
    DeltakerStatusType.HAR_SLUTTET,
    DeltakerStatusType.IKKE_AKTUELL,
    -> DeltakelseTilstand.AVSLUTTET
}

private fun getTilstand(status: ArbeidsgiverAvtaleStatus): DeltakelseTilstand = when (status) {
    ArbeidsgiverAvtaleStatus.PAABEGYNT,
    -> DeltakelseTilstand.KLADD

    ArbeidsgiverAvtaleStatus.MANGLER_GODKJENNING,
    -> DeltakelseTilstand.UTKAST

    ArbeidsgiverAvtaleStatus.KLAR_FOR_OPPSTART,
    ArbeidsgiverAvtaleStatus.GJENNOMFORES,
    -> DeltakelseTilstand.AKTIV

    ArbeidsgiverAvtaleStatus.AVSLUTTET,
    ArbeidsgiverAvtaleStatus.AVBRUTT,
    ArbeidsgiverAvtaleStatus.ANNULLERT,
    -> DeltakelseTilstand.AVSLUTTET
}

enum class DeltakelserMelding {
    MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET,
    MANGLER_DELTAKELSER_FRA_TEAM_TILTAK,
    MANGLER_DELTAKELSER_FRA_TILTAKSHISTORIKKEN,
}

data class Deltakelser(
    val meldinger: Set<DeltakelserMelding>,
    val aktive: List<Deltakelse>,
    val historiske: List<Deltakelse>,
) {
    /**
     * Kombinerer deltakelser. Ved konflikter på id så kastes deltakelse fra [other].
     */
    fun mergeWith(other: Deltakelser) = Deltakelser(
        meldinger = meldinger + other.meldinger,
        aktive = (aktive + other.aktive).distinctBy { it.id }.sortedWith(deltakelseComparator),
        historiske = (historiske + other.historiske).distinctBy { it.id }.sortedWith(deltakelseComparator),
    )

    fun filter(predicate: (deltakelse: Deltakelse) -> Boolean): Deltakelser {
        return Deltakelser(
            meldinger = meldinger,
            aktive = aktive.filter(predicate),
            historiske = historiske.filter(predicate),
        )
    }
}

/**
 * Sorterer deltakelser basert på nyeste startdato først
 */
private val deltakelseComparator: Comparator<Deltakelse> = Comparator { a, b ->
    val startDatoA = a.periode.startDato
    val startDatoB = b.periode.startDato

    when {
        startDatoA == null && startDatoB == null -> 0
        startDatoA == null -> -1
        startDatoB == null -> 1
        else -> startDatoB.compareTo(startDatoA)
    }
}
