package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

enum class Personopplysning(private val tittel: String, private val hjelpetekst: String?, val sortKey: Int) {
    NAVN("Navn", null, 1),
    KJONN("Kjønn", null, 2),
    ADRESSE("Adresse", null, 3),
    TELEFONNUMMER("Telefonnummer", null, 4),
    FOLKEREGISTER_IDENTIFIKATOR("Folkeregisteridentifikator (personnummer/D-nummer)", null, 5),
    FODSELSDATO("Fødselsdato", null, 6),
    BEHOV_FOR_BISTAND_FRA_NAV("Behov for bistand fra NAV", null, 7),
    YTELSER_FRA_NAV("Ytelser fra NAV", null, 8),
    BILDE("Bilde", null, 9),
    EPOST("E-postadresse", null, 10),
    BRUKERNAVN("Brukernavn", null, 11),
    ARBEIDSERFARING_OG_VERV(
        "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
        null,
        12,
    ),
    SERTIFIKATER_OG_KURS("Sertifikater og kurs, eks. førerkort, vekterkurs", null, 13),
    UTDANNING_OG_FAGBREV("Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.", null, 14),
    IP_ADRESSE("IP-adresse", null, 15),
    PERSONLIGE_EGENSKAPER_OG_INTERESSER("Opplysninger om personlige egenskaper og interesser", null, 16),
    SPRAKKUNNSKAP("Opplysninger om språkkunnskap", null, 17),
    ADFERD(
        "Opplysninger om atferd som kan ha betydning for tiltaksgjennomføring og jobbmuligheter",
        "For eksempel truende adferd, vanskelig å samarbeide med osv. Det kan for eksempel være tilfeller hvor det er nødvendig å informere tiltaksarrangør om at bruker har et sikkerhetstiltak hos NAV.",
        18,
    ),
    SOSIALE_FORHOLD(
        "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter",
        "For eksempel aleneomsorg for barn og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går utover tiltaksgjennomføringen.",
        19,
    ),
    HELSEOPPLYSNINGER(
        "Helseopplysninger (særlige kategorier av personopplysninger)",
        "Kan være nødvendig dersom deltaker har helseutfordringer som påvirker hvilke jobber han/hun kan ta, og dersom det er behov for tilrettelegging hos leverandør/arbeidsplass på grunn av helse.",
        20,
    ),
    RELIGION(
        "Religion (særlige kategorier av personopplysninger)",
        "Dersom det påvirker hvilke arbeidsoppgaver deltaker kan ha, behov for tilrettelegging, eller f.eks. dersom vedkommende ikke kan håndtere kjøtt.",
        21,
    ),
    NASJONALITET("Nasjonalitet", null, 22),
    ADRESSESPERRE("Adressesperre", null, 23),
    ;

    fun toPersonopplysningData() = PersonopplysningData(
        this,
        this.tittel,
        this.hjelpetekst,
    )
}

@Serializable
data class PersonopplysningData(
    val personopplysning: Personopplysning,
    val tittel: String,
    val hjelpetekst: String?,
)
