import { atom } from "jotai";
import { atomWithHash } from "jotai-location";

export const paginationAtom = atomWithHash(
  "pagination",
  { page: 1, pageSize: 50 },
  {
    setHash: "replaceState",
  },
);

export type FilterAccordionTypes = "apen-for-pamelding" | "innsatsgruppe" | "brukers-enhet";

export const filterAccordionAtom = atom<FilterAccordionTypes[]>([
  "apen-for-pamelding",
  "innsatsgruppe",
  "brukers-enhet",
]);
