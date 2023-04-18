import { ExtractAtomValue } from "jotai";
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
export const paginationAtomTiltaksgjennomforingMedTiltakstype = atomWithHash(
  "pageOnGjennomforing",
  1,
  { setHash: "replaceState" }
);
export const avtalePaginationAtom = atomWithHash("avtalePage", 1, {
  setHash: "replaceState",
});

export const tiltakstypefilter = atomWithHash<{
  sok: string;
  status?: Tiltakstypestatus;
  kategori?: Tiltakstypekategori;
  sortering: SorteringTiltakstyper;
}>(
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
    },
    {
      setHash: "replaceState",
    }
  );

const avtaleFilter = atomWithHash<{
  sok: string;
  status: Avtalestatus;
  enhet: string;
  tiltakstype: string;
  sortering: SorteringAvtaler;
}>(
  "avtalefilter",
  {
    sok: "",
    status: Avtalestatus.AKTIV,
    enhet: "",
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

export type avtaleFilterType = ExtractAtomValue<typeof avtaleFilter>;

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
