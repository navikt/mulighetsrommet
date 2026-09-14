package no.nav.mulighetsrommet.admin.arrangor

/**
 * Klassifiserer en [no.nav.mulighetsrommet.api.domain.arrangor.Arrangor] etter hva slags
 * virksomhet den representerer. Speiler skillet mellom [no.nav.mulighetsrommet.api.domain.arrangor.Arrangor.Norsk]
 * (med eller uten overordnet enhet) og [no.nav.mulighetsrommet.api.domain.arrangor.Arrangor.Utenlandsk], slik at
 * det kan filtreres på i søk.
 */
enum class ArrangorType {
    NORSK_HOVEDENHET,
    NORSK_UNDERENHET,
    UTENLANDSK,
}
