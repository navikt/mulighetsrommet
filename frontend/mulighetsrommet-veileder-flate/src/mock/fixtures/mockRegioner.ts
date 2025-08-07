import { NavRegionDto } from "@api-client";

export const mockRegioner: NavRegionDto[] = [
  {
    navn: "Nav Oslo",
    enhetsnummer: "0300",
    enheter: [
      {
        navn: "Nav St. Hanshaugen",
        enhetsnummer: "0313",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Bjerke",
        enhetsnummer: "0330",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Nordstrand",
        enhetsnummer: "0318",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Vestre Aker",
        enhetsnummer: "0334",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
      {
        navn: "Nav Grünerløkka",
        enhetsnummer: "0315",
        overordnetEnhet: "0300",
        erStandardvalg: true,
      },
    ],
  },
  {
    navn: "Nav Innlandet",
    enhetsnummer: "0400",
    enheter: [
      {
        navn: "Nav Solør",
        enhetsnummer: "0425",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Kongsvinger",
        enhetsnummer: "0402",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Løten",
        enhetsnummer: "0415",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Trysil",
        enhetsnummer: "0428",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Lesja - Dovre",
        enhetsnummer: "0511",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
      {
        navn: "Nav Eidskog",
        enhetsnummer: "0420",
        overordnetEnhet: "0400",
        erStandardvalg: true,
      },
    ],
  },
  {
    navn: "Nav Øst-Viken",
    enhetsnummer: "0200",
    enheter: [
      {
        navn: "Nav Fredrikstad",
        enhetsnummer: "0106",
        overordnetEnhet: "0200",
        erStandardvalg: true,
      },
      {
        navn: "Nav Sarpsborg",
        enhetsnummer: "0105",
        overordnetEnhet: "0200",
        erStandardvalg: true,
      },
    ],
  },
];
