import { ArrangorKontaktperson } from "mulighetsrommet-api-client";

export const mockArrangorKontaktpersoner: ArrangorKontaktperson[] = [
  {
    epost: "bjarte.berntsen@arrangor.no",
    navn: "Bjarte Berntsen",
    telefon: "12345678",
    id: "de954183-dfbf-4b70-87d2-3ba3b807129e",
    arrangorId: "c95e836f-a381-4d82-b8e3-74257b14f26c",
    beskrivelse: "Sekretær",
  },
  {
    epost: "elvira.johansen@arrangor.no",
    navn: "Elvira Johansen",
    telefon: null,
    id: "20e70e14-2b6a-440d-af5c-5f0a1ee7a416",
    arrangorId: "c95e836f-a381-4d82-b8e3-74257b14f26c",
    beskrivelse: "Direktør",
  },
];
