import { Virksomhet } from "mulighetsrommet-api-client";

export const mockVirksomheter: { [navn: string]: Virksomhet } = {
  fretex: {
    organisasjonsnummer: "123456789",
    navn: "Fretex AS",
    underenheter: [
      {
        organisasjonsnummer: "876543987",
        navn: "Fretex Oslo AS",
        overordnetEnhet: "123456789",
      },
    ],
  },
  ikea: {
    organisasjonsnummer: "987654321",
    navn: "Ikea AS",
  },
  rode_kors: {
    organisasjonsnummer: "134256789",
    navn: "Røde Kors AS",
  },
};
