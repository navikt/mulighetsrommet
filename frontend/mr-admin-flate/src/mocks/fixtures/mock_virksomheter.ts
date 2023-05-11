import { Virksomhet } from "mulighetsrommet-api-client";

export const mockVirksomheter: Virksomhet[] = [
  {
    organisasjonsnummer: "123456789",
    navn: "Testbedrift AS",
    underenheter: [
      {
        organisasjonsnummer: "876543987",
        navn: "Underenhet AS",
        overordnetEnhet: "123456789",
      },
    ],
  },
  {
    organisasjonsnummer: "987654321",
    navn: "Ikea AS",
  },
  {
    organisasjonsnummer: "134256789",
    navn: "Røde Kors AS",
  },
];
