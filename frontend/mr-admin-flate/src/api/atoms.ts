import { atomWithHash } from "jotai-location";
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

export const paginationAtom = atomWithHash("page", 1, {
  setHash: "replaceState",
});

export const avtalePaginationAtom = atomWithHash("avtalePage", 1, {
  setHash: "replaceState",
});

export const faneAtom = atomWithHash("fane", "tab_notifikasjoner_1", {
  setHash: "replaceState",
});

export interface TiltakstypeFilter {
  sok?: string;
  status?: Tiltakstypestatus;
  kategori?: Tiltakstypekategori;
  sortering?: SorteringTiltakstyper;
}

export const tiltakstypeFilter = atomWithHash<TiltakstypeFilter>(
  "tiltakstypefilter",
  {
    sok: "",
    status: Tiltakstypestatus.AKTIV,
    kategori: Tiltakstypekategori.GRUPPE,
    sortering: SorteringTiltakstyper.NAVN_ASCENDING,
  },
  {
    setHash: "replaceState",
  }
);

export interface Tiltaksgjennomforingfilter {
  search: string;
  enhet: string;
  tiltakstype: string;
  status: TiltaksgjennomforingStatus;
  sortering: SorteringTiltaksgjennomforinger;
  fylkesenhet: string;
  avtale: string;
  arrangorOrgnr: string;
  antallGjennomforingerVises: number;
}

export const tiltaksgjennomforingfilter =
  atomWithHash<Tiltaksgjennomforingfilter>(
    "tiltakstypefilter",
    {
      search: "",
      enhet: "",
      tiltakstype: "",
      status: TiltaksgjennomforingStatus.GJENNOMFORES,
      sortering: SorteringTiltaksgjennomforinger.NAVN_ASCENDING,
      fylkesenhet: "",
      avtale: "",
      arrangorOrgnr: "",
      antallGjennomforingerVises: PAGE_SIZE,
    },
    {
      setHash: "replaceState",
    }
  );

export const tiltaksgjennomforingTilAvtaleFilter = atom<
  Pick<Tiltaksgjennomforingfilter, "search">
>({ search: "" });

export interface AvtaleFilterProps {
  sok: string;
  status: Avtalestatus;
  navRegion: string;
  tiltakstype: string;
  sortering: SorteringAvtaler;
  leverandor_orgnr: string;
  antallAvtalerVises: number;
}

const avtaleFilter = atomWithHash<AvtaleFilterProps>(
  "avtalefilter",
  {
    sok: "",
    status: Avtalestatus.AKTIV,
    navRegion: "",
    tiltakstype: "",
    sortering: SorteringAvtaler.NAVN_ASCENDING,
    leverandor_orgnr: "",
    antallAvtalerVises: AVTALE_PAGE_SIZE,
  },
  { setHash: "replaceState" }
);

export type AvtaleTabs = "arenaInfo" | "avtaler";

export const avtaleTabAtom = atomWithHash<AvtaleTabs>(
  "avtaleTab",
  "arenaInfo",
  {
    setHash: "replaceState",
  }
);

export { avtaleFilter };

export type TiltaksgjennomforingerTabs = "detaljer" | "nokkeltall";

export const tiltaksgjennomforingTabAtom =
  atomWithHash<TiltaksgjennomforingerTabs>(
    "tiltaksgjennomforingTab",
    "detaljer",
    {
      setHash: "replaceState",
    }
  );
