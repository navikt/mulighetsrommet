import { atom } from "jotai";
import { atomWithHash } from "jotai-location";
import { ApentForInnsok, Innsatsgruppe, NavEnhet } from "mulighetsrommet-api-client";

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

export const defaultTiltaksgjennomforingfilter: Tiltaksgjennomforingsfilter = {
  search: "",
  innsatsgruppe: undefined,
  tiltakstyper: [],
  apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
};

export const tiltaksgjennomforingsfilter = atomWithStorage(
  "filter",
  defaultTiltaksgjennomforingfilter,
  sessionStorage,
);

export const paginationAtom = atomWithHash("page", 1, { setHash: "replaceState" });
export const faneAtom = atomWithHash("fane", "tab1", {
  setHash: "replaceState",
});

export const geografiskEnhetForPreviewAtom = atom<NavEnhet | undefined>(undefined);

export const filterAccordionAtom = atom<string[]>(["apen-for-innsok", "innsatsgruppe"]);

/**
 * atomWithStorage fra jotai rendrer først alltid initial value selv om den
 * finnes i storage (https://github.com/pmndrs/jotai/discussions/1879#discussioncomment-5626120)
 * Dette er anbefalt måte og ha en sync versjon av atomWithStorage
 */
function atomWithStorage<Value>(key: string, initialValue: Value, storage: Storage) {
  const baseAtom = atom(storage.getItem(key) ?? JSON.stringify(initialValue));
  return atom(
    (get) => JSON.parse(get(baseAtom)) as Value,
    (_, set, nextValue: Value) => {
      const str = JSON.stringify(nextValue);
      set(baseAtom, str);
      storage.setItem(key, str);
    },
  );
}
