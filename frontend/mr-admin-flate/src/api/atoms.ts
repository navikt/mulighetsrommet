import { atomWithHash } from "jotai-location";
import { atomWithStorage } from "jotai/utils";
import { Rolle } from "../tilgang/tilgang";
import { Side } from "./side/Side";

export const paginationAtom = atomWithHash("page", 1);
export const paginationAtomTiltaksgjennomforingMedTiltakstype = atomWithHash(
  "pageOnGjennomforing",
  1
);

export const rolleAtom = atomWithStorage<Rolle | undefined>(
  "mr-admin-rolle",
  undefined
);

export const tiltakstypefilter = atomWithHash<string>("tiltakstypefilter", "");

export const sideAtom = (fagansvarlig: boolean) =>
  atomWithStorage<Side>("side", fagansvarlig ? "/tiltakstyper" : "/mine");
