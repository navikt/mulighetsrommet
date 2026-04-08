package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class Personopplysning(
    val type: Type,
    val title: String,
    val helpText: String?,
    val sortKey: Int,
) {
    enum class Type {
        NAVN,
        KJONN,
        ADRESSE,
        TELEFONNUMMER,
        FOLKEREGISTER_IDENTIFIKATOR,
        FODSELSDATO,
        BEHOV_FOR_BISTAND_FRA_NAV,
        YTELSER_FRA_NAV,
        BILDE,
        EPOST,
        BRUKERNAVN,
        ARBEIDSERFARING_OG_VERV,
        SERTIFIKATER_OG_KURS,
        UTDANNING_OG_FAGBREV,
        IP_ADRESSE,
        PERSONLIGE_EGENSKAPER_OG_INTERESSER,
        SPRAKKUNNSKAP,
        ADFERD,
        SOSIALE_FORHOLD,
        HELSEOPPLYSNINGER,
        RELIGION,
        NASJONALITET,
        ADRESSESPERRE,
        VERGEMAL,
        STEMME,
    }
}
