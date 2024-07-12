package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.utils.CacheUtils
import java.util.concurrent.TimeUnit

class NavEnhetService(private val enhetRepository: NavEnhetRepository) {

    val cache: Cache<String, NavEnhetDbo> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(500)
        .recordStats()
        .build()

    fun hentEnhet(enhetsnummer: String): NavEnhetDbo? {
        return CacheUtils.tryCacheFirstNullable(cache, enhetsnummer) {
            enhetRepository.get(enhetsnummer)
        }
    }

    fun hentOverordnetFylkesenhet(enhetsnummer: String): NavEnhetDbo? {
        val enhet = CacheUtils.tryCacheFirstNullable(cache, enhetsnummer) {
            hentEnhet(enhetsnummer)
        }

        return if (enhet?.type == Norg2Type.FYLKE) {
            enhet
        } else if (enhet?.overordnetEnhet != null) {
            hentOverordnetFylkesenhet(enhet.overordnetEnhet)
        } else {
            enhet
        }
    }

    fun hentAlleEnheter(filter: EnhetFilter): List<NavEnhetDbo> {
        return enhetRepository.getAll(filter.statuser, filter.typer, filter.overordnetEnhet)
    }

    fun hentRegioner(): List<NavRegionDto> {
        val alleEnheter = hentAlleEnheter(
            EnhetFilter(
                statuser = listOf(NavEnhetStatus.AKTIV),
                typer = listOf(Norg2Type.KO, Norg2Type.LOKAL, Norg2Type.FYLKE),
            ),
        )
        return alleEnheter
            .filter { it.type == Norg2Type.FYLKE }
            .map { region ->
                NavRegionDto(
                    enhetsnummer = region.enhetsnummer,
                    navn = region.navn,
                    status = region.status,
                    type = region.type,
                    enheter = alleEnheter
                        .filter {
                            it.overordnetEnhet == region.enhetsnummer &&
                                (it.type == Norg2Type.LOKAL || NAV_EGNE_ANSATTE_TIL_FYLKE_MAP.keys.contains(it.enhetsnummer))
                        }
                        // K er før L så egne ansatte (som er KO) legger seg nederst (med desc)
                        .sortedByDescending { it.type },
                )
            }
    }

    @Serializable
    data class NavRegionDto(
        val enhetsnummer: String,
        val navn: String,
        val status: NavEnhetStatus,
        val type: Norg2Type,
        val enheter: List<NavEnhetDbo>,
    )
}
