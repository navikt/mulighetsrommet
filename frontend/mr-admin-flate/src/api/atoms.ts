import { atom, ExtractAtomValue } from "jotai";
import { atomWithHash } from "jotai-location";
import { atomWithStorage } from "jotai/utils";
import {
  Avtalestatus,
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

const avtaleFilter = atom<{
  sok: string;
  status: Avtalestatus;
  enhet: string;
  sortering: string;
}>({
  sok: "",
  status: Avtalestatus.AKTIV,
  enhet: "",
  sortering: "",
});

export type avtaleFilterType = ExtractAtomValue<typeof avtaleFilter>;

export { avtaleFilter };
