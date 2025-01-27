import { ApentForPamelding, Innsatsgruppe, NavEnhet } from "@mr/api-client-v2";
import {
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";
import { useAtom, useAtomValue } from "jotai";
import { SyncStorage } from "jotai/vanilla/utils/atomWithStorage";
import { z } from "zod";
import { brukersEnhetFilterHasChanged } from "@/apps/modia/delMedBruker/helpers";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";

export const ArbeidsmarkedstiltakFilterSchema = z.object({
  search: z.string(),
  navEnheter: z.custom<NavEnhet>().array(),
  innsatsgruppe: z
    .object({
      tittel: z.string(),
      nokkel: z.custom<Innsatsgruppe>(),
    })
    .optional(),
  tiltakstyper: z
    .object({
      id: z.string(),
      tittel: z.string(),
      nokkel: z.string().optional(),
    })
    .array(),
  apentForPamelding: z.custom<ApentForPamelding>(),
  erSykmeldtMedArbeidsgiver: z.boolean(),
});

export type ArbeidsmarkedstiltakFilter = z.infer<typeof ArbeidsmarkedstiltakFilterSchema>;

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

  const { data: brukerdata } = useBrukerdata();

  const filterHasChanged =
    filter.search !== "" ||
    filter.apentForPamelding !== ApentForPamelding.APENT_ELLER_STENGT ||
    filter.innsatsgruppe?.nokkel !== brukerdata?.innsatsgruppe ||
    (brukerdata && brukersEnhetFilterHasChanged(filter, brukerdata)) ||
    filter.tiltakstyper.length > 0;

  return {
    filter,
    filterHasChanged,
    resetFilterToDefaults() {
      setValue({
        brukerIKontekst,
        filter: defaultTiltakfilter,
      });
    },
  };
}

export function useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst() {
  const [{ filter }, setValue] = useAtom(filterAtom);

  const filterHasChanged =
    filter.search !== "" ||
    filter.apentForPamelding !== ApentForPamelding.APENT_ELLER_STENGT ||
    filter.innsatsgruppe?.nokkel !== undefined ||
    filter.navEnheter.length !== 0 ||
    filter.tiltakstyper.length > 0;

  return {
    filter,
    filterHasChanged,
    resetFilterToDefaults() {
      setValue({
        brukerIKontekst: null,
        filter: defaultTiltakfilter,
      });
    },
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
  apentForPamelding: ApentForPamelding.APENT_ELLER_STENGT,
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
