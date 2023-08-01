import { NavAnsatt, NavAnsattRolle } from "mulighetsrommet-api-client";

const bertil = {
  azureId: "8c133e5e-fd93-4226-8567-41d699a3efee",
  navIdent: "B123456",
  fornavn: "Bertil",
  etternavn: "Betabruker",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  mobilnummer: null,
  epost: "bertil.betabruker@nav.no",
  roller: [NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON],
  skalSlettesDato: null,
};

const pelle = {
  azureId: "db0d3a34-1071-42f5-aeec-38d37055271d",
  fornavn: "Pelle",
  etternavn: "Pilotbruker",
  navIdent: "P987655",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  mobilnummer: null,
  epost: "pelle.pilotbruker@nav.no",
  roller: [NavAnsattRolle.KONTAKTPERSON],
  skalSlettesDato: null,
};

export const mockBetabruker: NavAnsatt = bertil;

export const mockKontaktpersoner: NavAnsatt[] = [bertil, pelle];
