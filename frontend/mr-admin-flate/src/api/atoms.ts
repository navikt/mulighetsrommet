import {
  Avtalestatus,
  SorteringAvtaler,
  SorteringTiltaksgjennomforinger,
  SorteringTiltakstyper,
  TiltaksgjennomforingStatus,
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { atom, WritableAtom } from "jotai";
import { atomFamily } from "jotai/utils";
import { AVTALE_PAGE_SIZE, PAGE_SIZE } from "../constants";

// Bump version number when localStorage should be cleared
const version = localStorage.getItem("version");
if (version !== "1") {
  localStorage.clear();
  localStorage.setItem("version", "1");
}

/**
 * atomWithStorage fra jotai rendrer først alltid initial value selv om den
 * finnes i storage (https://github.com/pmndrs/jotai/discussions/1879#discussioncomment-5626120)
 * Dette er anbefalt måte og ha en sync versjon av atomWithStorage
 */
function atomWithStorage<Value>(key: string, initialValue: Value, storage = localStorage) {
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

function atomWithHashAndStorage<Value>(
  key: string,
  initialValue: Value,
  storage: Storage = localStorage,
): WritableAtom<Value, Value[], void> {
  const setHash = (hash: string) => {
    const searchParams = new URLSearchParams(window.location.hash.slice(1));
    searchParams.set(key, hash);
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${window.location.search}#${searchParams.toString()}`,
    );
  };
  const innerAtom = atomWithStorage(key, initialValue, storage);

  return atom(
    (get) => {
      const value = get(innerAtom);
      setHash(JSON.stringify(value));
      return value;
    },
    (_get, set, newValue: Value) => {
      set(innerAtom, newValue);
      setHash(JSON.stringify(newValue));
    },
  );
}

export interface TiltakstypeFilter {
  sok?: string;
  status?: Tiltakstypestatus;
  kategori?: Tiltakstypekategori;
  sortering?: SorteringTiltakstyper;
}

export const defaultTiltakstypeFilter: TiltakstypeFilter = {
  sok: "",
  status: Tiltakstypestatus.AKTIV,
  kategori: Tiltakstypekategori.GRUPPE,
  sortering: SorteringTiltakstyper.NAVN_ASCENDING,
};

export const tiltakstypeFilterAtom = atomWithHashAndStorage<TiltakstypeFilter>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
);

export interface TiltaksgjennomforingFilter {
  search: string;
  navEnheter: string[];
  tiltakstyper: string[];
  statuser: TiltaksgjennomforingStatus[];
  sortering: SorteringTiltaksgjennomforinger;
  navRegioner: string[];
  avtale: string;
  arrangorOrgnr: string[];
  visMineGjennomforinger: boolean;
  page: number;
  pageSize: number;
}

export const defaultTiltaksgjennomforingfilter: TiltaksgjennomforingFilter = {
  search: "",
  navEnheter: [],
  tiltakstyper: [],
  statuser: [],
  sortering: SorteringTiltaksgjennomforinger.NAVN_ASCENDING,
  navRegioner: [],
  avtale: "",
  arrangorOrgnr: [],
  visMineGjennomforinger: false,
  page: 1,
  pageSize: PAGE_SIZE,
};

export const tiltaksgjennomforingfilterAtom = atomWithHashAndStorage<TiltaksgjennomforingFilter>(
  "tiltaksgjennomforing-filter",
  defaultTiltaksgjennomforingfilter,
);

export const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  string,
  WritableAtom<TiltaksgjennomforingFilter, [newValue: TiltaksgjennomforingFilter], void>
>((avtaleId: string) => {
  return atomWithHashAndStorage(
    `tiltaksgjennomforing-filter-${avtaleId}`,
    {
      ...defaultTiltaksgjennomforingfilter,
      avtale: avtaleId,
    },
    sessionStorage,
  );
});

export interface AvtaleFilter {
  sok: string;
  statuser: Avtalestatus[];
  navRegioner: string[];
  tiltakstyper: string[];
  sortering: SorteringAvtaler;
  leverandor: string[];
  visMineAvtaler: boolean;
  page: number;
  pageSize: number;
}

export const defaultAvtaleFilter: AvtaleFilter = {
  sok: "",
  statuser: [],
  navRegioner: [],
  tiltakstyper: [],
  sortering: SorteringAvtaler.NAVN_ASCENDING,
  leverandor: [],
  visMineAvtaler: false,
  page: 1,
  pageSize: AVTALE_PAGE_SIZE,
};

export const avtaleFilterAtom = atomWithHashAndStorage<AvtaleFilter>(
  "avtale-filter",
  defaultAvtaleFilter,
);

export const getAvtalerForTiltakstypeFilterAtom = atomFamily<
  string,
  WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>
>((tiltakstypeId: string) => {
  return atomWithHashAndStorage(
    `avtale-filter-${tiltakstypeId}`,
    {
      ...defaultAvtaleFilter,
      tiltakstyper: [tiltakstypeId],
    },
    sessionStorage,
  );
});

export const gjennomforingDetaljerTabAtom = atom<string>("detaljer");

export const avtaleDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["status"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["status"]);
