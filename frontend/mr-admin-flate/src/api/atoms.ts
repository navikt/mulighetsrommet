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

export const paginationAtom = atomWithHash("page", 1, {
  setHash: "replaceState",
});

export const avtalePaginationAtom = atomWithHash("avtalePage", 1, {
  setHash: "replaceState",
});

export const faneAtom = atomWithHash('fane', 'tab1', {
  setHash: 'replaceState',
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
    },
    {
      setHash: "replaceState",
    }
  );

const avtaleFilter = atomWithHash<{
  sok: string;
  status: Avtalestatus;
  fylkeenhet: string;
  tiltakstype: string;
  sortering: SorteringAvtaler;
}>(
  "avtalefilter",
  {
    sok: "",
    status: Avtalestatus.AKTIV,
    fylkeenhet: "",
    tiltakstype: "",
    sortering: SorteringAvtaler.NAVN_ASCENDING,
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
