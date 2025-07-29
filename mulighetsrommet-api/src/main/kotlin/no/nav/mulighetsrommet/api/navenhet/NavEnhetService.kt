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

        return NavEnhetHelpers.buildNavRegioner(alleEnheter)
    }

    fun hentKostnadssted(regioner: List<NavEnhetNummer>): List<NavEnhetDto> = db.session {
        queries.enhet.getKostnadssted(regioner).map { toNavEnhetDto(it) }
    }

    private fun QueryContext.getNavEnhetDto(enhetsnummer: NavEnhetNummer): NavEnhetDto? {
        return queries.enhet.get(enhetsnummer)?.let { toNavEnhetDto(it) }
    }
}

private fun toNavEnhetDto(dbo: NavEnhetDbo) = NavEnhetDto(
    navn = dbo.navn,
    enhetsnummer = dbo.enhetsnummer,
    type = toNavEnhetType(dbo.type),
    overordnetEnhet = dbo.overordnetEnhet,
)

private fun toNorg2Type(type: NavEnhetType) = Norg2Type.valueOf(type.name)

private fun toNavEnhetType(type: Norg2Type) = NavEnhetType.valueOf(type.name)
