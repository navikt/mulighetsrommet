import { Ansatt, NavKontaktperson } from "mulighetsrommet-api-client";

export const mockBetabruker: Ansatt = {
  etternavn: "Betabruker",
  fornavn: "Bertil",
  ident: "B99876",
  navn: "Betabruker, Bertil",
  tilganger: ["BETABRUKER"],
  hovedenhet: "2990",
  hovedenhetNavn: "IT Drift",
};

export const mockKontaktpersoner: NavKontaktperson[] = [
  {
    etternavn: "Betabruker",
    fornavn: "Bertil",
    navident: "B99876",
    navn: "Betabruker, Bertil",
    hovedenhet: "2990",
    epost: "bertil.betabruker@nav.no",
  },
  {
    etternavn: "Pilotbruker",
    fornavn: "Pelle",
    navident: "P987655",
    navn: "Pilotbruker, ∏elle",
    hovedenhet: "2990",
    epost: "pelle.pilotbruker@nav.no",
  },
];
