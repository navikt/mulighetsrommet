import { Personopplysning, PersonopplysningType } from "@tiltaksadministrasjon/api-client";
import { HttpResponse, PathParams, http } from "msw";

const personopplysninger: Personopplysning[] = [
  {
    type: PersonopplysningType.NAVN,
    title: "Navn",
    helpText: null,
    sortKey: 1,
    beskrivelse: null,
  },
  {
    type: PersonopplysningType.KJONN,
    beskrivelse: null,
    title: "Kjønn",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.ADRESSE,
    beskrivelse: null,
    title: "Adresse",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.TELEFONNUMMER,
    beskrivelse: null,
    title: "Telefonnummer",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.FOLKEREGISTER_IDENTIFIKATOR,
    beskrivelse: null,
    title: "Folkeregisteridentifikator (personnummer/D-nummer)",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.FODSELSDATO,
    beskrivelse: null,
    title: "Fødselsdato",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.BEHOV_FOR_BISTAND_FRA_NAV,
    beskrivelse: null,
    title: "Behov for bistand fra Nav",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.YTELSER_FRA_NAV,
    beskrivelse: null,
    title: "Ytelser fra Nav",
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.BILDE,
    title: "Bilde",
    beskrivelse: null,
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.EPOST,
    title: "E-postadresse",
    beskrivelse: null,
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.BRUKERNAVN,
    title: "Brukernavn",
    beskrivelse: null,
    helpText: null,
    sortKey: 1,
  },
  {
    type: PersonopplysningType.ARBEIDSERFARING_OG_VERV,
    beskrivelse: null,
    title:
      "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
    sortKey: 1,
    helpText: null,
  },
  {
    type: PersonopplysningType.SERTIFIKATER_OG_KURS,
    beskrivelse: null,
    sortKey: 1,
    title: "Sertifikater og kurs, eks. førerkort, vekterkurs",
    helpText: null,
  },
  {
    type: PersonopplysningType.UTDANNING_OG_FAGBREV,
    beskrivelse: null,
    sortKey: 1,
    title: "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.",
    helpText: null,
  },
  {
    sortKey: 1,
    beskrivelse: null,
    type: PersonopplysningType.IP_ADRESSE,
    title: "IP-adresse",
    helpText: null,
  },
  {
    sortKey: 1,
    type: PersonopplysningType.PERSONLIGE_EGENSKAPER_OG_INTERESSER,
    beskrivelse: null,
    title: "Opplysninger om personlige egenskaper og interesser",
    helpText: null,
  },
  {
    sortKey: 1,
    beskrivelse: null,
    type: PersonopplysningType.SPRAKKUNNSKAP,
    title: "Opplysninger om språkkunnskap",
    helpText: null,
  },
  {
    sortKey: 1,
    beskrivelse: null,
    type: PersonopplysningType.ADFERD,
    title: "Opplysninger om atferd som kan ha betydning for tiltaket og jobbmuligheter",
    helpText:
      "For eksempel truende adferd, vanskelig å samarbeide med osv. Det kan for eksempel være tilfeller hvor det er nødvendig å informere tiltaksarrangør om at bruker har et sikkerhetstiltak hos Nav.",
  },
  {
    sortKey: 1,
    beskrivelse: null,
    type: PersonopplysningType.SOSIALE_FORHOLD,
    title: "Sosiale eller personlige forhold som kan ha betydning for tiltaket og jobbmuligheter",
    helpText:
      "For eksempel aleneomsorg for barn og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går utover tiltaket.",
  },
  {
    type: PersonopplysningType.HELSEOPPLYSNINGER,
    beskrivelse: null,
    sortKey: 1,
    title: "Helseopplysninger (særlige kategorier av personopplysninger)",
    helpText:
      "Kan være nødvendig dersom deltaker har helseutfordringer som påvirker hvilke jobber han/hun kan ta, og dersom det er behov for tilrettelegging hos leverandør/arbeidsplass på grunn av helse.",
  },
  {
    type: PersonopplysningType.RELIGION,
    beskrivelse: null,
    sortKey: 1,
    title: "Religion (særlige kategorier av personopplysninger)",
    helpText:
      "Dersom det påvirker hvilke arbeidsoppgaver deltaker kan ha, behov for tilrettelegging, eller f.eks. dersom vedkommende ikke kan håndtere kjøtt.",
  },
  {
    type: PersonopplysningType.NASJONALITET,
    beskrivelse: null,
    sortKey: 1,
    title: "Nasjonalitet/landbakgrunn",
    helpText: null,
  },
  {
    type: PersonopplysningType.ADRESSESPERRE,
    beskrivelse: null,
    sortKey: 1,
    title: "Adressesperre (kode 6/7)",
    helpText: null,
  },
];

export const personopplysningerHandlers = [
  http.get<PathParams>("*/api/tiltaksadministrasjon/personopplysninger", () =>
    HttpResponse.json(personopplysninger),
  ),
];
