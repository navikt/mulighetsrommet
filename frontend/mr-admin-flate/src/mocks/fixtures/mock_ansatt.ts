import { GjennomforingKontaktperson, NavAnsatt, Rolle } from "@mr/api-client-v2";

const bertil: NavAnsatt = {
  entraObjectId: "0bab029e-e84e-4842-8a27-d153b29782cf",
  navIdent: "B123456",
  fornavn: "Bertil",
  etternavn: "Bengtson",
  hovedenhet: { enhetsnummer: "2990", navn: "IT-avdelingen" },
  mobilnummer: null,
  epost: "bertil.bengtson@nav.no",
  roller: [
    { rolle: Rolle.TILTAKADMINISTRASJON_GENERELL, navn: "Tiltaksadministrasjon generell" },
    { rolle: Rolle.TILTAKSGJENNOMFORINGER_SKRIV, navn: "Skrivetilgang - Gjennomføring" },
    { rolle: Rolle.AVTALER_SKRIV, navn: "Skrivetilgang - Avtale" },
    { rolle: Rolle.SAKSBEHANDLER_OKONOMI, navn: "Saksbehandler - Økonomi" },
    { rolle: Rolle.BESLUTTER_TILSAGN, navn: "Beslutter - Tilsagn" },
    { rolle: Rolle.ATTESTANT_UTBETALING, navn: "Attestant - Utbetaling" },
    { rolle: Rolle.KONTAKTPERSON, navn: "Kontaktperson" },
  ],
  skalSlettesDato: null,
};

const pelle: NavAnsatt = {
  entraObjectId: "db0d3a34-1071-42f5-aeec-38d37055271d",
  fornavn: "Pelle",
  etternavn: "Pilotbruker",
  navIdent: "P987655",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  mobilnummer: null,
  epost: "pelle.pilotbruker@nav.no",
  roller: [{ rolle: Rolle.KONTAKTPERSON, navn: "Kontaktperson" }],
  skalSlettesDato: null,
};

const perRichard: NavAnsatt = {
  entraObjectId: "uu3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "per.richard.olsen@nav.no",
  mobilnummer: "90567894",
  navIdent: "O123456",
  fornavn: "Per Richard",
  etternavn: "Olsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [{ rolle: Rolle.KONTAKTPERSON, navn: "Kontaktperson" }],
  skalSlettesDato: null,
};

const nikoline: NavAnsatt = {
  entraObjectId: "zz3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "nikoline.madsen@nav.no",
  mobilnummer: "90764321",
  navIdent: "M654378",
  fornavn: "Nikoline",
  etternavn: "Madsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [{ rolle: Rolle.KONTAKTPERSON, navn: "Kontaktperson" }],
  skalSlettesDato: null,
};

const petrus: NavAnsatt = {
  entraObjectId: "ab3d3a34-1071-42f5-aeec-38d37055271d",
  epost: "petrus.pilsen@nav.no",
  mobilnummer: "78654323",
  navIdent: "M887654",
  fornavn: "Petrus",
  etternavn: "Pilsen",
  hovedenhet: {
    enhetsnummer: "2990",
    navn: "IT Drift",
  },
  roller: [{ rolle: Rolle.KONTAKTPERSON, navn: "Kontaktperson" }],
  skalSlettesDato: null,
};

export const petrusKontaktperson: GjennomforingKontaktperson = {
  navIdent: petrus.navIdent,
  navn: petrus.fornavn + " " + petrus.etternavn,
  epost: petrus.epost,
  mobilnummer: petrus.mobilnummer,
  beskrivelse: "Beskrivelse til Petrus",
};

export const nikolineKontaktperson: GjennomforingKontaktperson = {
  navIdent: nikoline.navIdent,
  navn: nikoline.fornavn + " " + nikoline.etternavn,
  epost: nikoline.epost,
  mobilnummer: nikoline.mobilnummer,
  beskrivelse: null,
};

export const mockRedaktor: NavAnsatt = bertil;

export const mockKontaktpersoner: NavAnsatt[] = [bertil, pelle, perRichard, nikoline, petrus];
