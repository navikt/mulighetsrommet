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
import { atomWithStorage } from 'jotai/vanilla/utils';

export function atomWithHashInLocalStorage<Value>(
  key: string,
  initialValue: Value,
) {
  const setHash = (hash: string) => {
    const searchParams = new URLSearchParams(window.location.hash.slice(1));
    searchParams.set(key, hash)
    window.history.replaceState(
      null,
      '',
      `${window.location.pathname}${window.location.search}#${searchParams.toString()}`,
    );
  };
  const innerAtom = atomWithStorage<Value>(
    key,
    initialValue,
    {
      getItem: (key, initialValue) => {
        const hash = localStorage.getItem(key)
        if (hash !== null && hash !== "undefined") {
          setHash(hash);
          return JSON.parse(hash);
        } else {
          return initialValue;
        }
      },
      setItem: (key, value) => {
        const hash = JSON.stringify(value);
        setHash(hash);
        localStorage.setItem(key, hash)
      },
      removeItem: (key) => {
        localStorage.removeItem(key)
      }
    }
  );
  return innerAtom;
}

export const paginationAtom = atomWithHashInLocalStorage("page", 1);

export const avtalePaginationAtom = atomWithHashInLocalStorage("avtalePage", 1);

export const faneAtom = atomWithHashInLocalStorage("fane", "tab_notifikasjoner_1");

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

export const tiltakstypeFilter = atomWithHashInLocalStorage<TiltakstypeFilter>(
  "tiltakstypefilter",
  defaultTiltakstypeFilter,
);

export interface Tiltaksgjennomforingfilter {
  search: string;
  enhet: string;
  tiltakstype: string;
  status: TiltaksgjennomforingStatus | "";
  sortering: SorteringTiltaksgjennomforinger;
  navRegion: string;
  avtale: string;
  arrangorOrgnr: string;
  antallGjennomforingerVises: number;
}

export const defaultTiltaksgjennomforingfilter: Tiltaksgjennomforingfilter = {
  search: "",
  enhet: "",
  tiltakstype: "",
  status: TiltaksgjennomforingStatus.GJENNOMFORES,
  sortering: SorteringTiltaksgjennomforinger.NAVN_ASCENDING,
  navRegion: "",
  avtale: "",
  arrangorOrgnr: "",
  antallGjennomforingerVises: PAGE_SIZE,
};

export const tiltaksgjennomforingfilter =
  atomWithHashInLocalStorage<Tiltaksgjennomforingfilter>(
    "tiltaksgjennomforingFilter",
    defaultTiltaksgjennomforingfilter,
  );

export const tiltaksgjennomforingTilAvtaleFilter = atom<
  Pick<Tiltaksgjennomforingfilter, "search">
>({ search: "" });

export type AvtaleTabs = "avtaleinfo" | "tiltaksgjennomforinger" | "nokkeltall";

export interface AvtaleFilterProps {
  sok: string;
  status: Avtalestatus | "";
  navRegion: string;
  tiltakstype: string;
  sortering: SorteringAvtaler;
  leverandor_orgnr: string;
  antallAvtalerVises: number;
  avtaleTab: AvtaleTabs;
}

export const defaultAvtaleFilter: AvtaleFilterProps = {
  sok: "",
  status: Avtalestatus.AKTIV,
  navRegion: "",
  tiltakstype: "",
  sortering: SorteringAvtaler.NAVN_ASCENDING,
  leverandor_orgnr: "",
  antallAvtalerVises: AVTALE_PAGE_SIZE,
  avtaleTab: "avtaleinfo",
};

const avtaleFilter = atomWithHashInLocalStorage<AvtaleFilterProps>(
  "avtalefilter",
  defaultAvtaleFilter,
);

export type TiltakstypeAvtaleTabs = "arenaInfo" | "avtaler";

export const avtaleTabAtom = atomWithHashInLocalStorage<TiltakstypeAvtaleTabs>(
  "avtaleTab",
  "arenaInfo",
);

export { avtaleFilter };

export type TiltaksgjennomforingerTabs = "detaljer" | "nokkeltall";

export const tiltaksgjennomforingTabAtom =
  atomWithHashInLocalStorage<TiltaksgjennomforingerTabs>(
    "tiltaksgjennomforingTab",
    "detaljer",
  );
