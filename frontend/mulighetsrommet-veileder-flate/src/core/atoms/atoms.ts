import { atom } from "jotai";
import { atomWithHash } from "jotai-location";

// Bump version number when sessionStorage should be cleared
const version = sessionStorage.getItem("version");
if (version !== "0.1") {
  sessionStorage.clear();
  sessionStorage.setItem("version", "0.1");
}

export const paginationAtom = atomWithHash(
  "pagination",
  { page: 1, pageSize: 50 },
  {
    setHash: "replaceState",
  },
);

export const filterAccordionAtom = atom<string[]>([
  "apen-for-innsok",
  "innsatsgruppe",
  "brukers-enhet",
]);
