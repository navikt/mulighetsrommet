import {
  NavAnsatt,
  NavAnsattRolle,
  TiltaksgjennomforingKontaktperson,
} from "mulighetsrommet-api-client";

const bertil: NavAnsatt = {
  azureId: "8c133e5e-fd93-4226-8567-41d699a3efee",
  navIdent: "B123456",
  fornavn: "Bertil",
  etternavn: "Bengtson",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  mobilnummer: null,
  epost: "bertil.Bengtson@nav.no",
  roller: [
    NavAnsattRolle.KONTAKTPERSON,
    NavAnsattRolle.AVTALER_SKRIV,
    NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
  ],
  skalSlettesDato: null,
};

const pelle: NavAnsatt = {
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

const perRichard: NavAnsatt = {
  azureId: "uu3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "per.richard.olsen@nav.no",
  mobilnummer: "90567894",
  navIdent: "O123456",
  fornavn: "Per Richard",
  etternavn: "Olsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [NavAnsattRolle.KONTAKTPERSON],
  skalSlettesDato: null,
};

const nikoline: NavAnsatt = {
  azureId: "zz3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "nikoline.madsen@nav.no",
  mobilnummer: "90764321",
  navIdent: "M654378",
  fornavn: "Nikoline",
  etternavn: "Madsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [NavAnsattRolle.KONTAKTPERSON],
  skalSlettesDato: null,
};

const petrus: NavAnsatt = {
  azureId: "ab3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "petrus.pilsen@nav.no",
  mobilnummer: "78654323",
  navIdent: "M887654",
  fornavn: "Petrus",
  etternavn: "Pilsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [NavAnsattRolle.KONTAKTPERSON],
  skalSlettesDato: null,
};

export const petrusKontaktperson: TiltaksgjennomforingKontaktperson = {
  navIdent: petrus.navIdent,
  navn: petrus.fornavn + " " + petrus.etternavn,
  epost: petrus.epost,
  mobilnummer: petrus.epost,
  navEnheter: ["0315", "0330"],
  beskrivelse: "Beskrivelse til Petrus",
};

export const nikolineKontaktperson: TiltaksgjennomforingKontaktperson = {
  navIdent: nikoline.navIdent,
  navn: nikoline.fornavn + " " + nikoline.etternavn,
  epost: nikoline.epost,
  mobilnummer: nikoline.epost,
  navEnheter: ["0313"],
  beskrivelse: null,
};

export const mockRedaktor: NavAnsatt = bertil;

export const mockKontaktpersoner: NavAnsatt[] = [bertil, pelle, perRichard, nikoline, petrus];
