import { ExtractAtomValue } from "jotai";
import { atomWithHash } from "jotai-location";
import { atomWithStorage } from "jotai/utils";
import {
  Avtalestatus,
  SorteringAvtaler,
  SorteringTiltakstyper,
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { Rolle } from "../tilgang/tilgang";

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

export const rolleAtom = atomWithStorage<Rolle | undefined>(
  "mr-admin-rolle",
  undefined
);

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

const avtaleFilter = atomWithHash<{
  sok: string;
  status: Avtalestatus;
  enhet: string;
  sortering: SorteringAvtaler;
}>(
  "avtalefilter",
  {
    sok: "",
    status: Avtalestatus.AKTIV,
    enhet: "",
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
