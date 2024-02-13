import { atom } from "jotai";
import { atomWithHash } from "jotai-location";

export const paginationAtom = atomWithHash(
  "pagination",
  { page: 1, pageSize: 15 },
  {
    setHash: "replaceState",
  },
);

export const filterAccordionAtom = atom<string[]>([
  "apen-for-innsok",
  "innsatsgruppe",
  "brukers-enhet",
]);
