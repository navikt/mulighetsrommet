import {
  GjennomforingKontaktpersonDto,
  NavAnsattDto,
  Rolle,
} from "@tiltaksadministrasjon/api-client";

const bertil: NavAnsattDto = {
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
};

const pelle: NavAnsattDto = {
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
};

const perRichard: NavAnsattDto = {
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
};

const nikoline: NavAnsattDto = {
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
};

const petrus: NavAnsattDto = {
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
};

export const petrusKontaktperson: GjennomforingKontaktpersonDto = {
  navIdent: petrus.navIdent,
  navn: petrus.fornavn + " " + petrus.etternavn,
  hovedenhet: "0400",
  epost: petrus.epost,
  mobilnummer: petrus.mobilnummer,
  beskrivelse: "Beskrivelse til Petrus",
};

export const nikolineKontaktperson: GjennomforingKontaktpersonDto = {
  navIdent: nikoline.navIdent,
  navn: nikoline.fornavn + " " + nikoline.etternavn,
  hovedenhet: "0400",
  epost: nikoline.epost,
  mobilnummer: nikoline.mobilnummer,
  beskrivelse: null,
};

export const mockRedaktor: NavAnsattDto = bertil;

export const mockKontaktpersoner: NavAnsattDto[] = [bertil, pelle, perRichard, nikoline, petrus];
