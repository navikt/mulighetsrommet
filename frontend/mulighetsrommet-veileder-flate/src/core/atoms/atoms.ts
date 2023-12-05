import { atomWithHash } from "jotai-location";
import { ApentForInnsok, Innsatsgruppe, NavEnhet } from "mulighetsrommet-api-client";
import { atomWithStorage } from "jotai/utils";
import { atom } from "jotai";

// Bump version number when localStorage should be cleared
const version = localStorage.getItem("version");
if (version !== "0.1.0") {
  localStorage.clear();
  localStorage.setItem("version", "0.1.0");
}

export interface Tiltaksgjennomforingsfilter {
  search: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<Innsatsgruppe>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
  apentForInnsok: ApentForInnsok;
}

export interface Tiltaksgjennomforingsfiltergruppe<T> {
  id: string;
  tittel: string;
  nokkel?: T;
}

export const tiltaksgjennomforingsfilter = atomWithStorage<Tiltaksgjennomforingsfilter>("filter", {
  search: "",
  innsatsgruppe: undefined,
  tiltakstyper: [],
  apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
});

export const paginationAtom = atomWithHash("page", 1, { setHash: "replaceState" });
export const faneAtom = atomWithHash("fane", "tab1", {
  setHash: "replaceState",
});

export const geografiskEnhetForPreviewAtom = atom<NavEnhet | undefined>(undefined);
