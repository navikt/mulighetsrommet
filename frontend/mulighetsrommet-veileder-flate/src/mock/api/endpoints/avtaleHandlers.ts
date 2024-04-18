import { http, HttpResponse } from "msw";
import { BehandlingAvPersonopplysninger, Personopplysning } from "mulighetsrommet-api-client";

export const avtaleHandlers = [
  http.get("*/api/v1/internal/avtaler/:id/behandle-personopplysninger", async () => {
    return HttpResponse.json<BehandlingAvPersonopplysninger>({
      alltid: [
        {
          personopplysning: Personopplysning.NAVN,
          beskrivelse: "Navn",
        },
        {
          personopplysning: Personopplysning.KJONN,
          beskrivelse: "Kjønn",
        },
        {
          personopplysning: Personopplysning.ADRESSE,
          beskrivelse: "Adresse",
        },
        {
          personopplysning: Personopplysning.TELEFONNUMMER,
          beskrivelse: "Telefonnummer",
        },
        {
          personopplysning: Personopplysning.FOLKEREGISTER_IDENTIFIKATOR,
          beskrivelse: "Folkeregisteridentifikator",
        },
        {
          personopplysning: Personopplysning.FODSELSDATO,
          beskrivelse: "Fødselsdato",
        },
        {
          personopplysning: Personopplysning.BEHOV_FOR_BISTAND_FRA_NAV,
          beskrivelse: "Behov for bistand fra NAV",
        },
        {
          personopplysning: Personopplysning.YTELSER_FRA_NAV,
          beskrivelse: "Ytelser fra NAV",
        },
        {
          personopplysning: Personopplysning.BILDE,
          beskrivelse: "Bilde",
        },
        {
          personopplysning: Personopplysning.EPOST,
          beskrivelse: "E-postadresse",
        },
        {
          personopplysning: Personopplysning.BRUKERNAVN,
          beskrivelse: "Brukernavn",
        },
        {
          personopplysning: Personopplysning.ARBEIDSERFARING_OG_VERV,
          beskrivelse:
            "Opplysninger knyttet til arbeidserfaring og verv som normalt fremkommer av en CV, herunder arbeidsgiver og hvor lenge man har jobbet",
        },
        {
          personopplysning: Personopplysning.SERTIFIKATER_OG_KURS,
          beskrivelse: "Sertifikater og kurs, eks. Førerkort, vekterkurs",
        },
        {
          personopplysning: Personopplysning.UTDANNING_OG_FAGBREV,
          beskrivelse: "Utdanning, herunder fagbrev, høyere utdanning, grunnskoleopplæring osv.",
        },
        {
          personopplysning: Personopplysning.PERSONLIGE_EGENSKAPER_OG_INTERESSER,
          beskrivelse: "Opplysninger om personlige egenskaper og interesser",
        },
        {
          personopplysning: Personopplysning.SPRAKKUNNSKAP,
          beskrivelse: "Opplysninger om språkkunnskap",
        },
      ],
      ofte: [
        {
          personopplysning: Personopplysning.IP_ADRESSE,
          beskrivelse: "IP-adresse",
        },
        {
          personopplysning: Personopplysning.SOSIALE_FORHOLD,
          beskrivelse:
            "Sosiale eller personlige forhold som kan ha betydning for tiltaksgjennomføring og jobbmuligheter (eks. Aleneforsørger og kan derfor ikke jobbe kveldstid, eller økonomiske forhold som går ut over tiltaksgjennomføringen)",
        },
        {
          personopplysning: Personopplysning.HELSEOPPLYSNINGER,
          beskrivelse: "Helseopplysninger",
        },
      ],
      sjelden: [
        {
          personopplysning: Personopplysning.RELIGION,
          beskrivelse: "Religion",
        },
      ],
    });
  }),
];
