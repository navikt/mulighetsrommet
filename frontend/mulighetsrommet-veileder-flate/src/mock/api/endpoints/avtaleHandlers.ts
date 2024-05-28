import { http, HttpResponse } from "msw";
import { Personopplysning, PersonopplysningerPerFrekvens } from "mulighetsrommet-api-client";

export const avtaleHandlers = [
  http.get("*/api/v1/intern/avtaler/:id/behandle-personopplysninger", async () => {
    return HttpResponse.json<PersonopplysningerPerFrekvens>({
      ALLTID: [
        {
          personopplysning: Personopplysning.NAVN,
          beskrivelse: "Navn",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.KJONN,
          beskrivelse: "Kjønn",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.ADRESSE,
          beskrivelse: "Adresse",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.TELEFONNUMMER,
          beskrivelse: "Telefonnummer",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.FOLKEREGISTER_IDENTIFIKATOR,
          beskrivelse: "Folkeregisteridentifikator",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.FODSELSDATO,
          beskrivelse: "Fødselsdato",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.BEHOV_FOR_BISTAND_FRA_NAV,
          beskrivelse: "Behov for bistand fra NAV",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.YTELSER_FRA_NAV,
          beskrivelse: "Ytelser fra NAV",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.BILDE,
          beskrivelse: "Bilde",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.EPOST,
          beskrivelse: "E-postadresse",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.BRUKERNAVN,
          beskrivelse: "Brukernavn",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.ARBEIDSERFARING_OG_VERV,
          beskrivelse:
            "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.SERTIFIKATER_OG_KURS,
          beskrivelse: "Sertifikater og kurs, eks. Førerkort, vekterkurs",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.UTDANNING_OG_FAGBREV,
          beskrivelse: "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.PERSONLIGE_EGENSKAPER_OG_INTERESSER,
          beskrivelse: "Opplysninger om personlige egenskaper og interesser",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.SPRAKKUNNSKAP,
          beskrivelse: "Opplysninger om språkkunnskap",
          hjelpetekst: null,
        },
      ],
      OFTE: [
        {
          personopplysning: Personopplysning.IP_ADRESSE,
          beskrivelse: "IP-adresse",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.SOSIALE_FORHOLD,
          beskrivelse:
            "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. Aleneforsørger og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går ut over tiltaksgjennomføringen)",
          hjelpetekst: null,
        },
        {
          personopplysning: Personopplysning.HELSEOPPLYSNINGER,
          beskrivelse: "Helseopplysninger",
          hjelpetekst: null,
        },
      ],
      SJELDEN: [
        {
          personopplysning: Personopplysning.RELIGION,
          beskrivelse: "Religion",
          hjelpetekst: null,
        },
      ],
    });
  }),
];
