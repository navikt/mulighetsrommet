import { Innsatsgruppe, VeilederflateInnsatsgruppe } from "mulighetsrommet-api-client";

export const mockInnsatsgrupper: VeilederflateInnsatsgruppe[] = [
  {
    tittel: "Standard innsats",
    nokkel: Innsatsgruppe.STANDARD_INNSATS,
    order: 0,
  },
  {
    tittel: "Situasjonsbestemt innsats",
    nokkel: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
    order: 1,
  },
  {
    tittel: "Spesielt tilpasset innsats ",
    nokkel: Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
    order: 2,
  },
  {
    tittel: "Varig tilpasset innsats",
    nokkel: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
    order: 3,
  },
];
