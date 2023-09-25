import {
  Avtalestatus,
  SorteringAvtaler,
  SorteringTiltaksgjennomforinger,
  SorteringTiltakstyper,
  TiltaksgjennomforingStatus,
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { atom } from "jotai";
import { AVTALE_PAGE_SIZE, PAGE_SIZE } from "../constants";

// Bump version number when localStorage should be cleared
const version = localStorage.getItem("version");
if (version !== "0.1.0") {
  localStorage.clear();
  localStorage.setItem("version", "0.1.0");
}

/**
 * atomWithStorage fra jotai rendrer først alltid initial value selv om den
 * finnes i storage (https://github.com/pmndrs/jotai/discussions/1879#discussioncomment-5626120)
 * Dette er anbefalt måte og ha en sync versjon av atomWithStorage
 */
function atomWithStorage<Value>(key: string, initialValue: Value, storage = localStorage) {
  const baseAtom = atom(storage.getItem(key) ?? JSON.stringify(initialValue));
  return atom(
    (get) => JSON.parse(get(baseAtom)),
    (get, set, nextValue: Value) => {
      const str = JSON.stringify(nextValue);
      set(baseAtom, str);
      storage.setItem(key, str);
    },
  );
}

function atomWithHashAndStorage<Value>(key: string, initialValue: Value) {
  const setHash = (hash: string) => {
    const searchParams = new URLSearchParams(window.location.hash.slice(1));
    searchParams.set(key, hash);
    window.history.replaceState(
      null,
      "",
      `${window.location.pathname}${window.location.search}#${searchParams.toString()}`,
    );
  };
  const innerAtom = atomWithStorage(key, initialValue);

  return atom(
    (get) => {
      const value = get(innerAtom);
      setHash(JSON.stringify(value));
      return value;
    },
    (get, set, newValue: Value) => {
      set(innerAtom, newValue);
      setHash(JSON.stringify(newValue));
    },
  );
}

export const paginationAtom = atomWithHashAndStorage("page", 1);

export const avtalePaginationAtom = atomWithHashAndStorage("avtalePage", 1);

export interface TiltakstypeFilter {
  sok?: string;
  status: Tiltakstypestatus | "";
  kategori?: Tiltakstypekategori | "";
  sortering?: SorteringTiltakstyper;
}

export const defaultTiltakstypeFilter: TiltakstypeFilter = {
  sok: "",
  status: Tiltakstypestatus.AKTIV,
  kategori: Tiltakstypekategori.GRUPPE,
  sortering: SorteringTiltakstyper.NAVN_ASCENDING,
};

export const tiltakstypeFilter = atomWithHashAndStorage<TiltakstypeFilter>(
  "tiltakstypefilter",
  defaultTiltakstypeFilter,
);

export interface Tiltaksgjennomforingfilter {
  search: string;
  navEnhet: string;
  tiltakstype: string;
  status: TiltaksgjennomforingStatus | "";
  sortering: SorteringTiltaksgjennomforinger;
  navRegion: string;
  avtale: string;
  arrangorOrgnr: string;
  antallGjennomforingerVises: number;
  visMineGjennomforinger: boolean;
}

export const defaultTiltaksgjennomforingfilter: Tiltaksgjennomforingfilter = {
  search: "",
  navEnhet: "",
  tiltakstype: "",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  sortering: SorteringTiltaksgjennomforinger.NAVN_ASCENDING,
  navRegion: "",
  avtale: "",
  arrangorOrgnr: "",
  antallGjennomforingerVises: PAGE_SIZE,
  visMineGjennomforinger: false,
};

export const tiltaksgjennomforingfilter = atomWithHashAndStorage<Tiltaksgjennomforingfilter>(
  "tiltaksgjennomforingFilter",
  defaultTiltaksgjennomforingfilter,
);

export const tiltaksgjennomforingTilAvtaleFilter = atom<Pick<Tiltaksgjennomforingfilter, "search">>(
  { search: "" },
);

export interface AvtaleFilterProps {
  sok: string;
  status: Avtalestatus | "";
  navRegion: string;
  tiltakstype: string;
  sortering: SorteringAvtaler;
  leverandor_orgnr: string;
  antallAvtalerVises: number;
  visMineAvtaler: boolean;
}

export const defaultAvtaleFilter: AvtaleFilterProps = {
  sok: "",
  status: Avtalestatus.AKTIV,
  navRegion: "",
  tiltakstype: "",
  sortering: SorteringAvtaler.NAVN_ASCENDING,
  leverandor_orgnr: "",
  antallAvtalerVises: AVTALE_PAGE_SIZE,
  visMineAvtaler: false,
};

export const avtaleFilter = atomWithHashAndStorage<AvtaleFilterProps>(
  "avtalefilter",
  defaultAvtaleFilter,
);
