package no.nav.mulighetsrommet.api.navenhet

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class NavEnhetService(
    private val db: ApiDatabase,
) {
    val cache: Cache<NavEnhetNummer, NavEnhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    fun hentEnhet(enhetsnummer: NavEnhetNummer): NavEnhetDto? = db.session {
        CacheUtils.tryCacheFirstNullable(cache, enhetsnummer) {
            getNavEnhetDto(enhetsnummer)
        }
    }

    fun hentOverordnetFylkesenhet(enhetsnummer: NavEnhetNummer): NavEnhetDto? {
        val enhet = CacheUtils.tryCacheFirstNullable(cache, enhetsnummer) {
            hentEnhet(enhetsnummer)
        }

        return if (enhet?.type == NavEnhetType.FYLKE) {
            enhet
        } else if (enhet?.overordnetEnhet != null) {
            hentOverordnetFylkesenhet(enhet.overordnetEnhet)
        } else {
            enhet
        }
    }

    fun hentAlleEnheter(filter: EnhetFilter): List<NavEnhetDto> = db.session {
        val typer = filter.typer?.map { toNorg2Type(it) }

        queries.enhet
            .getAll(filter.statuser, typer, filter.overordnetEnhet)
            .map { toNavEnhetDto(it) }
    }

    fun hentRegioner(): List<NavRegionDto> {
        val relevanteEnheter = EnhetFilter(
            statuser = listOf(NavEnhetStatus.AKTIV),
            typer = listOf(NavEnhetType.KO, NavEnhetType.LOKAL, NavEnhetType.FYLKE, NavEnhetType.ARK),
        )

        val alleEnheter = hentAlleEnheter(relevanteEnheter)
            .filter { NavEnhetHelpers.erGeografiskEnhet(it.type) || NavEnhetHelpers.erSpesialenhetSomKanVelgesIModia(it.enhetsnummer) }

        return buildRegionList(alleEnheter)
    }

    fun hentKostnadssted(regioner: List<NavEnhetNummer>): List<NavEnhetDto> = db.session {
        queries.enhet.getKostnadssted(regioner).map { toNavEnhetDto(it) }
    }

    private fun QueryContext.getNavEnhetDto(enhetsnummer: NavEnhetNummer): NavEnhetDto? {
        return queries.enhet.get(enhetsnummer)?.let { toNavEnhetDto(it) }
    }
}

fun buildRegionList(enheter: List<NavEnhetDto>): List<NavRegionDto> {
    return enheter
        .filter { it.type == NavEnhetType.FYLKE }
        .toSet()
        .map { region ->
            NavRegionDto(
                enhetsnummer = region.enhetsnummer,
                navn = region.navn,
                type = region.type,
                enheter = enheter
                    .filter { it.overordnetEnhet == region.enhetsnummer }
                    .toSet()
                    // K er før L så egne ansatte (som er KO) legger seg nederst (med desc)
                    .sortedByDescending { it.type },
            )
        }
}

private fun toNavEnhetDto(dbo: NavEnhetDbo) = NavEnhetDto(
    navn = dbo.navn,
    enhetsnummer = dbo.enhetsnummer,
    type = toNavEnhetType(dbo.type),
    overordnetEnhet = dbo.overordnetEnhet,
)

private fun toNorg2Type(type: NavEnhetType): Norg2Type = when (type) {
    NavEnhetType.FYLKE -> Norg2Type.FYLKE
    NavEnhetType.LOKAL -> Norg2Type.LOKAL
    NavEnhetType.TILTAK -> Norg2Type.TILTAK
    NavEnhetType.ALS -> Norg2Type.ALS
    NavEnhetType.KO -> Norg2Type.KO
    NavEnhetType.ARK -> Norg2Type.ARK
}

private fun toNavEnhetType(type: Norg2Type): NavEnhetType = when (type) {
    Norg2Type.FYLKE -> NavEnhetType.FYLKE
    Norg2Type.LOKAL -> NavEnhetType.LOKAL
    Norg2Type.TILTAK -> NavEnhetType.TILTAK
    Norg2Type.ALS -> NavEnhetType.ALS
    Norg2Type.KO -> NavEnhetType.KO
    Norg2Type.ARK -> NavEnhetType.ARK
    else -> throw IllegalArgumentException("Norg2Type er er ikke en støttet NavEnhetType: $type")
}
