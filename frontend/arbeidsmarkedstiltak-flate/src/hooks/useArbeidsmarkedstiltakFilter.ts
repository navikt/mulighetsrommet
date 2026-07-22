import {
  ApentForPamelding,
  Innsatsgruppe,
  LagretFilterType,
  Tiltakskode,
} from "@arbeidsmarkedstiltak/api-client";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { brukersEnhetFilterHasChanged } from "@/apps/modia/delMedBruker/helpers";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { dequal } from "dequal";
import { useAtom, useAtomValue, useSetAtom } from "jotai";
import {
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";
import { SyncStorage } from "jotai/vanilla/utils/atomWithStorage";
import { useCallback, useEffect } from "react";
import { z } from "zod";

export const ArbeidsmarkedstiltakFilterSchema = z.object({
  search: z.string(),
  navEnheter: z.string().array(),
  innsatsgruppe: z
    .object({
      tittel: z.string(),
      nokkel: z.custom<Innsatsgruppe>(),
    })
    .optional(),
  tiltakstyper: z
    .object({
      id: z.custom<Tiltakskode>(),
      tittel: z.string(),
    })
    .array(),
  apentForPamelding: z.custom<ApentForPamelding>().array(),
  erSykmeldtMedArbeidsgiver: z.boolean(),
});

export type ArbeidsmarkedstiltakFilter = z.infer<typeof ArbeidsmarkedstiltakFilterSchema>;

export function useArbeidsmarkedstiltakFilter(): [
  ArbeidsmarkedstiltakFilter,
  (filter: ArbeidsmarkedstiltakFilter) => void,
] {
  const setSelectedFilterId = useSetAtom(selectedFilterIdAtom);
  const [value, setValue] = useAtom(filterAtom);

  return [
    value.filter,
    (filter: ArbeidsmarkedstiltakFilter) => {
      setSelectedFilterId(undefined);
      setValue({ brukerIKontekst: value.brukerIKontekst, filter });
    },
  ];
}

export function useArbeidsmarkedstiltakFilterValue() {
  const value = useAtomValue(filterAtom);
  return value.filter;
}

export function useArbeidsmarkedstiltakFilterMedBrukerIKontekst() {
  const [{ brukerIKontekst, filter }, setValue] = useAtom(filterAtom);

  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useBrukerdata();

  const brukersInnsatsgruppe = innsatsgrupper.find(
    (gruppe) => gruppe.nokkel === brukerdata.innsatsgruppe,
  );

  const defaultFilterForBruker: ArbeidsmarkedstiltakFilter = {
    search: "",
    navEnheter: brukerdata.enheter.map((enhet) => enhet.enhetsnummer),
    innsatsgruppe: brukersInnsatsgruppe
      ? {
          nokkel: brukersInnsatsgruppe.nokkel,
          tittel: brukersInnsatsgruppe.tittel,
        }
      : undefined,
    tiltakstyper: [],
    apentForPamelding: [],
    erSykmeldtMedArbeidsgiver: brukerdata.erSykmeldtMedArbeidsgiver,
  };

  const filterHasChanged =
    filter.search !== "" ||
    filter.apentForPamelding.length > 0 ||
    filter.innsatsgruppe?.nokkel !== brukerdata.innsatsgruppe ||
    brukersEnhetFilterHasChanged(filter, brukerdata) ||
    filter.tiltakstyper.length > 0;

  return {
    filter,
    filterHasChanged,
    resetFilterToDefaults() {
      setValue({
        brukerIKontekst,
        filter: defaultFilterForBruker,
      });
    },
  };
}

export function useArbeidsmarkedstiltakFilterUtenBrukerIKontekst() {
  const [filterIsInitialized, setFilterIsInitialized] = useAtom(filterIsInitializedAtom);
  const [selectedFilterId, setSelectedFilterId] = useAtom(selectedFilterIdAtom);
  const [{ filter }, setValue] = useAtom(filterAtom);

  const { data: savedFilters = [], status } = useLagredeFilter(
    LagretFilterType.GJENNOMFORING_MODIA,
  );
  const saveFilterMutation = useLagreFilter();
  const deleteFilterMutation = useSlettFilter();

  const defaultFilter = savedFilters.find((f) => f.isDefault);
  const defaultFilterValues =
    (defaultFilter?.filter as ArbeidsmarkedstiltakFilter | undefined) ?? defaultTiltakfilter;

  const selectFilter = useCallback(
    (id: string) => {
      const valgtFilter = savedFilters.find((f) => f.id === id);
      if (valgtFilter !== undefined) {
        setSelectedFilterId(valgtFilter.id);
        setValue({
          brukerIKontekst: null,
          filter: valgtFilter.filter as ArbeidsmarkedstiltakFilter,
        });
      }
    },
    [savedFilters, setSelectedFilterId, setValue],
  );

  const resetFilterToDefaults = useCallback(() => {
    if (defaultFilter) {
      setSelectedFilterId(defaultFilter.id);
    }

    setValue({
      brukerIKontekst: null,
      filter: defaultFilterValues,
    });
  }, [defaultFilter, defaultFilterValues, setSelectedFilterId, setValue]);

  const setDefaultFilter = useCallback(
    (id: string, isDefault: boolean) => {
      const filter = savedFilters.find((f) => f.id === id);
      if (filter) {
        saveFilterMutation.mutate({ ...filter, isDefault });
      }
    },
    [savedFilters, saveFilterMutation],
  );

  const saveFilter = useCallback(
    (namedFilter: { id: string; navn: string; filter: { [key: string]: unknown } }) => {
      saveFilterMutation.mutate(
        {
          ...namedFilter,
          type: LagretFilterType.GJENNOMFORING_MODIA,
          isDefault: false,
          sortOrder: 0,
        },
        {
          onSuccess() {
            saveFilterMutation.reset();
          },
        },
      );
    },
    [saveFilterMutation],
  );

  const deleteFilter = useCallback(
    (id: string) => {
      deleteFilterMutation.mutate(id);
    },
    [deleteFilterMutation],
  );

  useEffect(() => {
    if (status === "success" && !filterIsInitialized) {
      resetFilterToDefaults();
      setFilterIsInitialized(true);
    }
  }, [status, filterIsInitialized, setFilterIsInitialized, resetFilterToDefaults]);

  const filterHasChanged = !dequal(filter, defaultFilterValues);

  return {
    filter,
    filterHasChanged,
    selectedFilterId,
    savedFilters,
    selectFilter,
    resetFilterToDefaults,
    setDefaultFilter,
    saveFilter,
    deleteFilter,
  };
}

const filterMedBrukerIKontekstSchema = z.object({
  brukerIKontekst: z.string().nullable(),
  filter: z.custom<ArbeidsmarkedstiltakFilter>(),
});
export type FilterMedBrukerIKontekst = z.infer<typeof filterMedBrukerIKontekstSchema>;

export function getDefaultFilterForBrukerIKontekst(
  brukerIKontekst: string | null,
): FilterMedBrukerIKontekst {
  const defaultFilterForBrukerIKontekst = {
    brukerIKontekst,
    filter: defaultTiltakfilter,
  };

  const filterFromStorage = filterStorage.getItem(
    ARBEIDSMARKEDSTILTAK_FILTER_KEY,
    defaultFilterForBrukerIKontekst,
  );

  return filterFromStorage.brukerIKontekst === brukerIKontekst
    ? filterFromStorage
    : defaultFilterForBrukerIKontekst;
}

const filterValidator = (v: unknown): v is FilterMedBrukerIKontekst => {
  return Boolean(filterMedBrukerIKontekstSchema.safeParse(v).success);
};

const filterStorage: SyncStorage<FilterMedBrukerIKontekst> = withStorageValidator(filterValidator)(
  createJSONStorage(() => sessionStorage),
);

const ARBEIDSMARKEDSTILTAK_FILTER_KEY = "arbeidsmarkedstiltak-filter";

const defaultTiltakfilter: ArbeidsmarkedstiltakFilter = {
  search: "",
  navEnheter: [],
  innsatsgruppe: undefined,
  tiltakstyper: [],
  apentForPamelding: [],
  erSykmeldtMedArbeidsgiver: false,
};

export const filterAtom = atomWithStorage<FilterMedBrukerIKontekst>(
  ARBEIDSMARKEDSTILTAK_FILTER_KEY,
  { brukerIKontekst: null, filter: defaultTiltakfilter },
  filterStorage,
  { getOnInit: true },
);

export function isFilterReady(filter: ArbeidsmarkedstiltakFilter): boolean {
  return filter.innsatsgruppe !== undefined && filter.navEnheter.length !== 0;
}

const selectedFilterIdAtom = atomWithStorage<string | undefined>(
  "selected-filter-id",
  undefined,
  createJSONStorage(() => sessionStorage),
  { getOnInit: true },
);

const filterIsInitializedAtom = atomWithStorage<boolean>(
  "filter-is-initialized",
  false,
  createJSONStorage(() => sessionStorage),
  { getOnInit: true },
);
