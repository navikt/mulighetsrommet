import { atomWithHash } from "jotai-location";
import { Innsatsgruppe, NavEnhet } from "mulighetsrommet-api-client";
import { atomWithStorage } from "jotai/utils";
import { atom } from "jotai";

interface AppContextData {
  fnr: string;
  enhet: string;
  overordnetEnhet?: string | null;
}

export const appContext = atom<Partial<AppContextData>>({});

export interface Tiltaksgjennomforingsfilter {
  search: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<Innsatsgruppe>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
}

export interface Tiltaksgjennomforingsfiltergruppe<T> {
  id: string;
  tittel: string;
  nokkel?: T;
}

export const tiltaksgjennomforingsfilter = atomWithStorage<Tiltaksgjennomforingsfilter>("filter", {
  search: "",
  innsatsgruppe: undefined,
  tiltakstyper: [],
});

export const paginationAtom = atomWithHash("page", 1, { setHash: "replaceState" });
export const faneAtom = atomWithHash("fane", "tab1", {
  setHash: "replaceState",
});

export type JoyrideStorage = {
  joyrideOversikten: boolean;
  joyrideOversiktenLastStep: boolean | null;
  joyrideDetaljer: boolean;
  joyrideDetaljerHarVistOpprettAvtale: boolean;
};

export const joyrideAtom = atomWithStorage<JoyrideStorage>("joyride_mulighetsrommet", {
  joyrideOversikten: true,
  joyrideOversiktenLastStep: null,
  joyrideDetaljer: true,
  joyrideDetaljerHarVistOpprettAvtale: true,
});

export const geografiskEnhetForPreviewAtom = atom<NavEnhet | undefined>(undefined);
