import { atom } from "jotai";
import { atomWithHash } from "jotai-location";
import { NavEnhet } from "mulighetsrommet-api-client";

interface AppContextData {
  fnr: string;
  enhet: string;
  overordnetEnhet?: string | null;
}

export const appContext = atom<Partial<AppContextData>>({});

// Bump version number when localStorage should be cleared
const version = localStorage.getItem("version");
if (version !== "0.2.0") {
  localStorage.clear();
  sessionStorage.clear();
  localStorage.setItem("version", "0.2.0");
}

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
