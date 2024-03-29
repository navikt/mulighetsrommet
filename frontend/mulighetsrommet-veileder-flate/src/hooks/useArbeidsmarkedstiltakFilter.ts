import { ApentForInnsok, Innsatsgruppe, NavEnhet } from "mulighetsrommet-api-client";
import { atomWithStorage, createJSONStorage } from "jotai/utils";
import { useAtom, useAtomValue } from "jotai";
import { SyncStorage } from "jotai/vanilla/utils/atomWithStorage";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { brukersEnhetFilterHasChanged } from "@/apps/modia/delMedBruker/helpers";

export interface ArbeidsmarkedstiltakFilter {
  search: string;
  navEnheter: NavEnhet[];
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

export function useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst() {
  const [{ brukerIKontekst, filter }, setValue] = useAtom(filterAtom);

  const { data: brukerdata } = useHentBrukerdata();

  const filterHasChanged =
    filter.search !== "" ||
    filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT ||
    filter.innsatsgruppe?.nokkel !== brukerdata?.innsatsgruppe ||
    brukersEnhetFilterHasChanged(filter, brukerdata) ||
    filter.tiltakstyper.length > 0;

  return {
    filter,
    filterHasChanged,
    resetFilterToDefaults() {
      setValue({
        brukerIKontekst,
        filter: defaultTiltaksgjennomforingfilter,
      });
    },
  };
}

export function useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst() {
  const [{ filter }, setValue] = useAtom(filterAtom);

  const filterHasChanged =
    filter.search !== "" ||
    filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT ||
    filter.innsatsgruppe?.nokkel !== undefined ||
    filter.navEnheter.length !== 0 ||
    filter.tiltakstyper.length > 0;

  return {
    filter,
    filterHasChanged,
    resetFilterToDefaults() {
      setValue({
        brukerIKontekst: null,
        filter: defaultTiltaksgjennomforingfilter,
      });
    },
  };
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
  navEnheter: [],
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

export function isFilterReady(filter: ArbeidsmarkedstiltakFilter): boolean {
  return filter.innsatsgruppe !== undefined && filter.navEnheter.length !== 0;
}
