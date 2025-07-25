import { Innsatsgruppe, VeilederflateInnsatsgruppe } from "@api-client";

export const mockInnsatsgrupper: VeilederflateInnsatsgruppe[] = [
  {
    tittel: "Gode muligheter (standard)",
    nokkel: Innsatsgruppe.GODE_MULIGHETER,
    order: 0,
  },
  {
    tittel: "Trenger veiledning (situasjonsbestemt)",
    nokkel: Innsatsgruppe.TRENGER_VEILEDNING,
    order: 1,
  },
  {
    tittel: "Trenger veiledning, nedsatt arbeidsevne (spesielt tilpasset)",
    nokkel: Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    order: 2,
  },
  {
    tittel: "Jobbe delvis (delvis varig tilpasset, kun ny løsning)",
    nokkel: Innsatsgruppe.JOBBE_DELVIS,
    order: 3,
  },
  {
    tittel: "Liten mulighet til å jobbe (varig tilpasset)",
    nokkel: Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
    order: 4,
  },
];
