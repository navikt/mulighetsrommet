import { LagretVirksomhet } from "mulighetsrommet-api-client";

export const mockVirksomheter: {
  [navn: string]: LagretVirksomhet;
} = {
  fretex: {
    id: "c95e836f-a381-4d82-b8e3-74257b14f26c",
    organisasjonsnummer: "123456789",
    navn: "Fretex AS",
    underenheter: [
      {
        id: "d9d4db51-3564-4493-b897-4fc38dc48965",
        organisasjonsnummer: "876543987",
        navn: "Fretex Oslo AS",
        overordnetEnhet: "123456789",
      },
    ],
  },
  ikea: {
    id: "2dd86798-7a93-4d51-80ce-3e63f8e2daf3",
    organisasjonsnummer: "987654321",
    navn: "Ikea AS",
  },
  rode_kors: {
    id: "a1a5191f-1478-4ad8-9a82-47182a28a19d",
    organisasjonsnummer: "134256789",
    navn: "Røde Kors AS",
  },
};
