import { atomWithHash } from "jotai-location";
import { ApentForInnsok, Innsatsgruppe, NavEnhet } from "mulighetsrommet-api-client";
import { atomWithStorage } from "jotai/utils";
import { atom } from "jotai";

export interface Tiltaksgjennomforingsfilter {
  search: string;
  innsatsgruppe?: Tiltaksgjennomforingsfiltergruppe<Innsatsgruppe>;
  tiltakstyper: Tiltaksgjennomforingsfiltergruppe<string>[];
  apentForInnsok: ApentForInnsok;
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
  apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
});

export const paginationAtom = atomWithHash("page", 1, { setHash: "replaceState" });
export const faneAtom = atomWithHash("fane", "tab1", {
  setHash: "replaceState",
});

type JoyrideStorage = {
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
