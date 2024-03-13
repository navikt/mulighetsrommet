import { atom } from "jotai";

export type Sorteringer =
  | "tiltakstype-ascending"
  | "tiltakstype-ascending"
  | "oppstart-descending"
  | "oppstart-ascending"
  | "navn-ascending"
  | "navn-descending";

export const sorteringAtom = atom<Sorteringer>("tiltakstype-ascending");
