import { ApentForInnsok, Innsatsgruppe } from "mulighetsrommet-api-client";
import { atomWithStorage, createJSONStorage } from "jotai/utils";
import { useAtom, useAtomValue } from "jotai";
import { SyncStorage } from "jotai/vanilla/utils/atomWithStorage";

export interface ArbeidsmarkedstiltakFilter {
  search: string;
  innsatsgruppe?: ArbeidsmarkedstiltakFilterGruppe<Innsatsgruppe>;
  tiltakstyper: ArbeidsmarkedstiltakFilterGruppe<string>[];
  apentForInnsok: ApentForInnsok;
}

export interface ArbeidsmarkedstiltakFilterGruppe<T> {
  id: string;
  tittel: string;
  nokkel?: T;
}

export function useArbeidsmarkedstiltakFilter(): [
  ArbeidsmarkedstiltakFilter,
  (filter: ArbeidsmarkedstiltakFilter) => void,
] {
  const [value, setValue] = useAtom(filterAtom);

  return [
    value.filter,
    (filter: ArbeidsmarkedstiltakFilter) => {
      setValue({ brukerIKontekst: value.brukerIKontekst, filter });
    },
  ];
}

export function useArbeidsmarkedstiltakFilterValue() {
  const value = useAtomValue(filterAtom);
  return value.filter;
}

export interface FilterMedBrukerIKontekst {
  brukerIKontekst: string | null;
  filter: ArbeidsmarkedstiltakFilter;
}

export function getDefaultFilterForBrukerIKontekst(
  brukerIKontekst: string | null,
): FilterMedBrukerIKontekst {
  const defaultFilterForBrukerIKontekst = {
    brukerIKontekst,
    filter: defaultTiltaksgjennomforingfilter,
  };

  const filterFromStorage = filterStorage.getItem(
    ARBEIDSMARKEDSTILTAK_FILTER_KEY,
    defaultFilterForBrukerIKontekst,
  );

  return filterFromStorage.brukerIKontekst === brukerIKontekst
    ? filterFromStorage
    : defaultFilterForBrukerIKontekst;
}

const filterStorage: SyncStorage<FilterMedBrukerIKontekst> = createJSONStorage(
  () => sessionStorage,
);

const ARBEIDSMARKEDSTILTAK_FILTER_KEY = "arbeidsmarkedstiltak-filter";

const defaultTiltaksgjennomforingfilter: ArbeidsmarkedstiltakFilter = {
  search: "",
  innsatsgruppe: undefined,
  tiltakstyper: [],
  apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
};

export const filterAtom = atomWithStorage<FilterMedBrukerIKontekst>(
  ARBEIDSMARKEDSTILTAK_FILTER_KEY,
  { brukerIKontekst: null, filter: defaultTiltaksgjennomforingfilter },
  filterStorage,
  { getOnInit: true },
);
