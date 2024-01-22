import { atom } from "jotai";
import { atomWithHash } from "jotai-location";
import { NavEnhet } from "mulighetsrommet-api-client";

export const paginationAtom = atomWithHash(
  "pagination",
  { page: 1, pageSize: 15 },
  {
    setHash: "replaceState",
  },
);

export const faneAtom = atomWithHash("fane", "tab1", {
  setHash: "replaceState",
});

export const geografiskEnhetForPreviewAtom = atom<NavEnhet | undefined>(undefined);

export const filterAccordionAtom = atom<string[]>(["apen-for-innsok", "innsatsgruppe"]);
