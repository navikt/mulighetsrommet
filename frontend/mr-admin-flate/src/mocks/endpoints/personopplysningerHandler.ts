import { HttpResponse, PathParams, http } from "msw";

export const personopplysningerHandlers = [
  http.get<PathParams>("*/api/v1/intern/personopplysninger", () =>
    HttpResponse.json([
      {
        personopplysning: "NAVN",
        tittel: "Navn",
        hjelpetekst: null,
      },
      {
        personopplysning: "KJONN",
        tittel: "Kjønn",
        hjelpetekst: null,
      },
      {
        personopplysning: "ADRESSE",
        tittel: "Adresse",
        hjelpetekst: null,
      },
      {
        personopplysning: "TELEFONNUMMER",
        tittel: "Telefonnummer",
        hjelpetekst: null,
      },
      {
        personopplysning: "FOLKEREGISTER_IDENTIFIKATOR",
        tittel: "Folkeregisteridentifikator (personnummer/D-nummer)",
        hjelpetekst: null,
      },
      {
        personopplysning: "FODSELSDATO",
        tittel: "Fødselsdato",
        hjelpetekst: null,
      },
      {
        personopplysning: "BEHOV_FOR_BISTAND_FRA_NAV",
        tittel: "Behov for bistand fra Nav",
        hjelpetekst: null,
      },
      {
        personopplysning: "YTELSER_FRA_NAV",
        tittel: "Ytelser fra Nav",
        hjelpetekst: null,
      },
      {
        personopplysning: "BILDE",
        tittel: "Bilde",
        hjelpetekst: null,
      },
      {
        personopplysning: "EPOST",
        tittel: "E-postadresse",
        hjelpetekst: null,
      },
      {
        personopplysning: "BRUKERNAVN",
        tittel: "Brukernavn",
        hjelpetekst: null,
      },
      {
        personopplysning: "ARBEIDSERFARING_OG_VERV",
        tittel:
          "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
        hjelpetekst: null,
      },
      {
        personopplysning: "SERTIFIKATER_OG_KURS",
        tittel: "Sertifikater og kurs, eks. førerkort, vekterkurs",
        hjelpetekst: null,
      },
      {
        personopplysning: "UTDANNING_OG_FAGBREV",
        tittel: "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.",
        hjelpetekst: null,
      },
      {
        personopplysning: "IP_ADRESSE",
        tittel: "IP-adresse",
        hjelpetekst: null,
      },
      {
        personopplysning: "PERSONLIGE_EGENSKAPER_OG_INTERESSER",
        tittel: "Opplysninger om personlige egenskaper og interesser",
        hjelpetekst: null,
      },
      {
        personopplysning: "SPRAKKUNNSKAP",
        tittel: "Opplysninger om språkkunnskap",
        hjelpetekst: null,
      },
      {
        personopplysning: "ADFERD",
        tittel: "Opplysninger om atferd som kan ha betydning for tiltaket og jobbmuligheter",
        hjelpetekst:
          "For eksempel truende adferd, vanskelig å samarbeide med osv. Det kan for eksempel være tilfeller hvor det er nødvendig å informere tiltaksarrangør om at bruker har et sikkerhetstiltak hos Nav.",
      },
      {
        personopplysning: "SOSIALE_FORHOLD",
        tittel:
          "Sosiale eller personlige forhold som kan ha betydning for tiltaket og jobbmuligheter",
        hjelpetekst:
          "For eksempel aleneomsorg for barn og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går utover tiltaket.",
      },
      {
        personopplysning: "HELSEOPPLYSNINGER",
        tittel: "Helseopplysninger (særlige kategorier av personopplysninger)",
        hjelpetekst:
          "Kan være nødvendig dersom deltaker har helseutfordringer som påvirker hvilke jobber han/hun kan ta, og dersom det er behov for tilrettelegging hos leverandør/arbeidsplass på grunn av helse.",
      },
      {
        personopplysning: "RELIGION",
        tittel: "Religion (særlige kategorier av personopplysninger)",
        hjelpetekst:
          "Dersom det påvirker hvilke arbeidsoppgaver deltaker kan ha, behov for tilrettelegging, eller f.eks. dersom vedkommende ikke kan håndtere kjøtt.",
      },
      {
        personopplysning: "NASJONALITET",
        tittel: "Nasjonalitet/landbakgrunn",
        hjelpetekst: null,
      },
      {
        personopplysning: "ADRESSESPERRE",
        tittel: "Adressesperre (kode 6/7)",
        hjelpetekst: null,
      },
    ]),
  ),
];
