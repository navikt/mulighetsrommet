import { http, HttpResponse } from "msw";
import { Personopplysning, PersonopplysningData } from "mulighetsrommet-api-client";

export const avtaleHandlers = [
  http.get("*/api/v1/intern/avtaler/:id/behandle-personopplysninger", async () => {
    return HttpResponse.json<PersonopplysningData[]>([
      {
        personopplysning: Personopplysning.NAVN,
        tittel: "Navn",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.KJONN,
        tittel: "Kjønn",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.ADRESSE,
        tittel: "Adresse",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.TELEFONNUMMER,
        tittel: "Telefonnummer",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.FOLKEREGISTER_IDENTIFIKATOR,
        tittel: "Folkeregisteridentifikator",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.FODSELSDATO,
        tittel: "Fødselsdato",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.BEHOV_FOR_BISTAND_FRA_NAV,
        tittel: "Behov for bistand fra NAV",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.YTELSER_FRA_NAV,
        tittel: "Ytelser fra NAV",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.BILDE,
        tittel: "Bilde",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.EPOST,
        tittel: "E-postadresse",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.BRUKERNAVN,
        tittel: "Brukernavn",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.ARBEIDSERFARING_OG_VERV,
        tittel:
          "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.SERTIFIKATER_OG_KURS,
        tittel: "Sertifikater og kurs, eks. Førerkort, vekterkurs",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.UTDANNING_OG_FAGBREV,
        tittel: "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.PERSONLIGE_EGENSKAPER_OG_INTERESSER,
        tittel: "Opplysninger om personlige egenskaper og interesser",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.SPRAKKUNNSKAP,
        tittel: "Opplysninger om språkkunnskap",
        hjelpetekst: null,
      },

      {
        personopplysning: Personopplysning.IP_ADRESSE,
        tittel: "IP-adresse",
        hjelpetekst: null,
      },
      {
        personopplysning: Personopplysning.SOSIALE_FORHOLD,
        tittel:
          "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. Aleneforsørger og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går ut over tiltaksgjennomføringen)",
        hjelpetekst:
          "For eksempel aleneomsorg for barn og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går utover tiltaksgjennomføringen.",
      },
      {
        personopplysning: Personopplysning.HELSEOPPLYSNINGER,
        tittel: "Helseopplysninger",
        hjelpetekst:
          "Kan være nødvendig dersom deltaker har helseutfordringer som påvirker hvilke jobber han/hun kan ta, og dersom det er behov for tilrettelegging hos leverandør/arbeidsplass på grunn av helse.",
      },
      {
        personopplysning: Personopplysning.RELIGION,
        tittel: "Religion",
        hjelpetekst: null,
      },
    ]);
  }),
];
