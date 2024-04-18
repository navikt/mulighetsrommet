package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

enum class Personopplysning {
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
    IP_ADRESSE,
    UTDANNING_OG_FAGBREV,
    PERSONLIGE_EGENSKAPER_OG_INTERESSER,
    SPRAKKUNNSKAP,
    ADFERD,
    SOSIALE_FORHOLD,
    HELSEOPPLYSNINGER,
    RELIGION,
}

enum class PersonopplysningFrekvens {
    ALLTID,
    OFTE,
    SJELDEN,
}

@Serializable
data class PersonopplysningMedFrekvens(
    val personopplysning: Personopplysning,
    val frekvens: PersonopplysningFrekvens,
    val beskrivelse: String,
) {
    fun toPersonopplysningMedBeskrivelse(): PersonopplysningMedBeskrivelse {
        return PersonopplysningMedBeskrivelse(
            personopplysning = personopplysning,
            beskrivelse = beskrivelse,
        )
    }
}

@Serializable
data class PersonopplysningMedBeskrivelse(
    val personopplysning: Personopplysning,
    val beskrivelse: String,
)

@Serializable
data class PersonopplysningerMedBeskrivelse(
    val alltid: List<PersonopplysningMedBeskrivelse>,
    val ofte: List<PersonopplysningMedBeskrivelse>,
    val sjelden: List<PersonopplysningMedBeskrivelse>,
    val ikkeRelevant: List<PersonopplysningMedBeskrivelse>,
)
