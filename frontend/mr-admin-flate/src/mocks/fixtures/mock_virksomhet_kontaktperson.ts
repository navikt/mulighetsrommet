import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";

export const mockVirksomhetKontaktperson: VirksomhetKontaktperson[] = [
  {
    epost: "bjarte.berntsen@arrangor.no",
    navn: "Bjarte Berntsen",
    telefon: "12345678",
    id: "de954183-dfbf-4b70-87d2-3ba3b807129e",
    organisasjonsnummer: "123456789",
    beskrivelse: "Sekretær",
  },
  {
    epost: "elvira.johansen@arrangor.no",
    navn: "Elvira Johansen",
    telefon: null,
    id: "20e70e14-2b6a-440d-af5c-5f0a1ee7a416",
    organisasjonsnummer: "123456789",
    beskrivelse: "Direktør",
  },
];
