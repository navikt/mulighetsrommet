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
    ;

    fun toPersonopplysningMedBeskrivelse(hjelpetekst: String?) =
        PersonopplysningMedBeskrivelse(
            personopplysning = this,
            beskrivelse = this.toBeskrivelse(),
            hjelpetekst = hjelpetekst,
        )

    private fun toBeskrivelse(): String {
        return when (this) {
            NAVN -> "Navn"
            KJONN -> "Kjønn"
            ADRESSE -> "Adresse"
            TELEFONNUMMER -> "Telefonnummer"
            FOLKEREGISTER_IDENTIFIKATOR -> "Folkeregisteridentifikator (personnummer og D-nummer)"
            FODSELSDATO -> "Fødselsdato"
            BEHOV_FOR_BISTAND_FRA_NAV -> "Behov for bistand fra NAV"
            YTELSER_FRA_NAV -> "Ytelser fra NAV"
            BILDE -> "Bilde"
            EPOST -> "E-postadresse"
            BRUKERNAVN -> "Brukernavn"
            ARBEIDSERFARING_OG_VERV -> "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet"
            SERTIFIKATER_OG_KURS -> "Sertifikater og kurs, eks. førerkort, vekterkurs"
            IP_ADRESSE -> "IP-adresse"
            UTDANNING_OG_FAGBREV -> "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv."
            PERSONLIGE_EGENSKAPER_OG_INTERESSER -> "Opplysninger om personlige egenskaper og interesser"
            SPRAKKUNNSKAP -> "Opplysninger om språkkunnskap"
            ADFERD -> "Opplysninger om atferd som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. truende adferd, vanskelig å samarbeide med osv.)"
            SOSIALE_FORHOLD -> "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. aleneforsørger og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går ut over tiltaksgjennomføringen)"
            HELSEOPPLYSNINGER -> "Helseopplysninger"
            RELIGION -> "Religion"
        }
    }
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
    val hjelpetekst: String? = null,
)

@Serializable
data class PersonopplysningMedBeskrivelse(
    val personopplysning: Personopplysning,
    val beskrivelse: String,
    val hjelpetekst: String?,
)
