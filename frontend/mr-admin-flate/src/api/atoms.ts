import { atomWithHash } from "jotai-location";
import { atomWithStorage } from "jotai/utils";
import {
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { Rolle } from "../tilgang/tilgang";

export const paginationAtom = atomWithHash("page", 1);
export const paginationAtomTiltaksgjennomforingMedTiltakstype = atomWithHash(
  "pageOnGjennomforing",
  1
);

export const rolleAtom = atomWithStorage<Rolle | undefined>(
  "mr-admin-rolle",
  undefined
);

export const tiltakstypefilter = atomWithHash<{
  sok: string;
  status: Tiltakstypestatus;
  kategori?: Tiltakstypekategori;
}>(
  "tiltakstypefilter",
  {
    sok: "",
    status: Tiltakstypestatus.AKTIV,
    kategori: Tiltakstypekategori.GRUPPE,
  },
  {
    setHash: "replaceState",
  }
);
