import { ApentForInnsok, EmbeddedNavEnhet, Innsatsgruppe } from "mulighetsrommet-api-client";
import { atomWithStorage, createJSONStorage } from "jotai/utils";
import { useAtom, useAtomValue } from "jotai";
import { SyncStorage } from "jotai/vanilla/utils/atomWithStorage";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { brukersEnhetFilterHasChanged } from "../utils/Utils";

export interface RegionMap {
  [region: string]: string[];
}

export interface ArbeidsmarkedstiltakFilter {
  search: string;
  regionMap: RegionMap;
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

export function useResetArbeidsmarkedstiltakFilter() {
  const [{ brukerIKontekst, filter }, setValue] = useAtom(filterAtom);

  const { data: brukerdata } = useHentBrukerdata();

  const filterHasChanged =
    filter.innsatsgruppe?.nokkel !== brukerdata?.innsatsgruppe ||
    filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT ||
    filter.search !== "" ||
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
  regionMap: {},
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

export function navEnheter(filter: ArbeidsmarkedstiltakFilter): string[] {
  return Array.from(Object.values(filter.regionMap)).flat(1);
}

export function buildRegionMap(navEnheter: EmbeddedNavEnhet[]): RegionMap {
  const map: RegionMap = {};
  navEnheter.forEach((enhet: EmbeddedNavEnhet) => {
    const regionNavn = enhet.overordnetEnhet ?? "unknown";
    if (regionNavn in map) {
      map[regionNavn].push(enhet.enhetsnummer);
    } else {
      map[regionNavn] = [enhet.enhetsnummer];
    }
  });

  return map;
}
