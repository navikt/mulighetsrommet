import { NusDataResponse, NusElement, Tiltakskode } from "mulighetsrommet-api-client";

const vgsNivaa: NusElement[] = [
  {
    code: "30",
    name: "Allmenne fag",
    parent: "3",
    tiltakskode: Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    version: "2437",
  },
  {
    code: "31",
    name: "Humanistiske og estetiske fag",
    parent: "3",
    tiltakskode: Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    version: "2437",
  },
  {
    code: "32",
    name: "Lærerutdanninger og utdanninger i pedagogikk",
    parent: "3",
    tiltakskode: Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    version: "2437",
  },
];

export const mockNusData: NusDataResponse = {
  data: [
    {
      nivaa: "Videregående, grunnutdanning",
      kategorier: vgsNivaa,
    },
    {
      nivaa: "Videregående, avsluttende utdanning",
      kategorier: vgsNivaa,
    },
  ],
};
