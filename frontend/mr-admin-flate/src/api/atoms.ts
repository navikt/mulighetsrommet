import { ARRANGORER_PAGE_SIZE, AVTALE_PAGE_SIZE, PAGE_SIZE } from "@/constants";
import { SortState } from "@navikt/ds-react";
import { atom, WritableAtom } from "jotai";
import {
  atomFamily,
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";
import {
  Avtalestatus,
  Avtaletype,
  GjennomforingStatus,
  NavEnhet,
  OppgaveType,
  SorteringArrangorer,
  SorteringAvtaler,
  SorteringGjennomforinger,
  SorteringTiltakstyper,
} from "@mr/api-client-v2";
import { z, ZodType } from "zod";

function createSorteringProps(sortItems: z.ZodType) {
  return z.object({
    tableSort: z.custom<SortState>(),
    sortString: sortItems,
  });
}

export function createFilterValidator<T>(schema: ZodType<T>) {
  return (values: unknown): values is T => {
    return Boolean(schema.safeParse(values).success);
  };
}

const TiltakstypeFilterSchema = z.object({
  sort: createSorteringProps(z.custom<SorteringTiltakstyper>()).optional(),
});

export type TiltakstypeFilterType = z.infer<typeof TiltakstypeFilterSchema>;

export const defaultTiltakstypeFilter: TiltakstypeFilterType = {
  sort: {
    sortString: SorteringTiltakstyper.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
};

const tiltakstypeFilterStorage = withStorageValidator(
  createFilterValidator(TiltakstypeFilterSchema),
)(createJSONStorage(() => sessionStorage));

export const tiltakstypeFilterAtom = atomWithStorage<TiltakstypeFilterType>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
  tiltakstypeFilterStorage,
  { getOnInit: true },
);

export const GjennomforingFilterSchema = z.object({
  search: z.string(),
  navEnheter: z.custom<NavEnhet>().array(),
  tiltakstyper: z.string().array(),
  statuser: z.custom<GjennomforingStatus>().array(),
  sortering: createSorteringProps(z.custom<SorteringGjennomforinger>()),
  avtale: z.string(),
  arrangorer: z.string().array(),
  visMineGjennomforinger: z.boolean(),
  publisert: z.string().array(),
  page: z.number(),
  pageSize: z.number(),
});

export type GjennomforingFilterType = z.infer<typeof GjennomforingFilterSchema>;

export const defaultGjennomforingFilter: GjennomforingFilterType = {
  search: "",
  navEnheter: [],
  tiltakstyper: [],
  statuser: [GjennomforingStatus.GJENNOMFORES],
  sortering: {
    sortString: SorteringGjennomforinger.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  avtale: "",
  arrangorer: [],
  publisert: [],
  visMineGjennomforinger: false,
  page: 1,
  pageSize: PAGE_SIZE,
  // lagretFilterIdValgt: undefined,
};

const gjennomforingFilterStorage = withStorageValidator(
  createFilterValidator(GjennomforingFilterSchema),
)(createJSONStorage(() => sessionStorage));

export const gjennomforingfilterAtom = atomWithStorage<GjennomforingFilterType>(
  "gjennomforing-filter",
  defaultGjennomforingFilter,
  gjennomforingFilterStorage,
  { getOnInit: true },
);

const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  GjennomforingFilterType,
  WritableAtom<GjennomforingFilterType, [newValue: GjennomforingFilterType], void>
>(
  (filter: GjennomforingFilterType) => {
    return atomWithStorage(
      `gjennomforing-filter-${filter.avtale}`,
      filter,
      createJSONStorage(() => sessionStorage),
    );
  },
  (a, b) => a.avtale === b.avtale,
);

export function getGjennomforingerForAvtaleFilterAtom(avtaleId: string) {
  const defaultFilterValue = { ...defaultGjennomforingFilter, avtale: avtaleId };
  const filterAtom = gjennomforingerForAvtaleFilterAtomFamily(defaultFilterValue);
  return { defaultFilterValue, filterAtom };
}

export type FilterState<T> = {
  id?: string;
  values: T;
};

export type FilterManagerState<T> = {
  filter: FilterState<T>;
  defaultFilter: FilterState<T>;
  isInitialized: boolean;
};

export type FilterAction<T> =
  | { type: "UPDATE_DEFAULT"; payload?: FilterState<T> }
  | { type: "RESET_TO_DEFAULT" }
  | { type: "SELECT_FILTER"; payload: FilterState<T> }
  | { type: "SET_FILTER"; payload: T }
  | { type: "UPDATE_FILTER"; payload: Partial<T> };

export function createFilterAtomWithAPI<T extends object>(
  storageKey: string,
  fallbackFilter: T,
  validator: (value: unknown) => value is T,
) {
  const initialState = {
    isInitialized: false,
    defaultFilter: { id: undefined, values: fallbackFilter },
    filter: { values: fallbackFilter },
  };

  // Create storage
  const filterStateStorage = withStorageValidator<FilterManagerState<T>>(
    (value: unknown): value is FilterManagerState<T> => {
      if (!value || typeof value !== "object") return false;
      const state = value as any;
      return (
        state.filter &&
        validator(state.filter.values) &&
        (typeof state.filter.id === "string" || state.filter.id === undefined) &&
        (state.defaultFilter === undefined || validator(state.defaultFilter.values)) &&
        typeof state.isInitialized === "boolean"
      );
    },
  )(createJSONStorage(() => sessionStorage));

  function filterReducer(
    state: FilterManagerState<T>,
    action: FilterAction<T>,
  ): FilterManagerState<T> {
    switch (action.type) {
      case "UPDATE_DEFAULT": {
        const defaultFilter = action.payload ?? initialState.defaultFilter;
        if (!state.isInitialized) {
          return {
            ...state,
            isInitialized: true,
            filter: defaultFilter,
            defaultFilter,
          };
        }

        return { ...state, defaultFilter };
      }

      case "RESET_TO_DEFAULT":
        return {
          ...state,
          filter: state.defaultFilter,
        };

      case "SET_FILTER":
        return {
          ...state,
          filter: {
            id: undefined,
            values: action.payload,
          },
        };

      case "UPDATE_FILTER":
        return {
          ...state,
          filter: {
            id: undefined,
            values: { ...state.filter.values, ...action.payload },
          },
        };

      case "SELECT_FILTER":
        return {
          ...state,
          filter: action.payload,
        };

      default:
        return state;
    }
  }

  const filterManagerAtom = atomWithStorage<FilterManagerState<T>>(
    storageKey,
    initialState,
    filterStateStorage,
    { getOnInit: true },
  );

  return atom(
    (get) => get(filterManagerAtom),
    (get, set, action: FilterAction<T>) => {
      set(filterManagerAtom, filterReducer(get(filterManagerAtom), action));
    },
  );
}

export const AvtaleFilterSchema = z.object({
  sok: z.string(),
  statuser: z.custom<Avtalestatus>().array(),
  avtaletyper: z.custom<Avtaletype>().array(),
  navRegioner: z.string().array(),
  tiltakstyper: z.string().array(),
  sortering: createSorteringProps(z.custom<SorteringAvtaler>()),
  arrangorer: z.string().array(),
  visMineAvtaler: z.boolean(),
  personvernBekreftet: z.boolean().optional(),
  page: z.number(),
  pageSize: z.number(),
  lagretFilterIdValgt: z.string().optional(),
});

export type AvtaleFilterType = z.infer<typeof AvtaleFilterSchema>;

export const defaultAvtaleFilter: AvtaleFilterType = {
  sok: "",
  statuser: [Avtalestatus.AKTIV],
  avtaletyper: [],
  navRegioner: [],
  tiltakstyper: [],
  sortering: {
    sortString: SorteringAvtaler.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  arrangorer: [],
  visMineAvtaler: false,
  personvernBekreftet: undefined,
  page: 1,
  pageSize: AVTALE_PAGE_SIZE,
  lagretFilterIdValgt: undefined,
};

const avtaleFilterStorage = withStorageValidator(createFilterValidator(AvtaleFilterSchema))(
  createJSONStorage(() => sessionStorage),
);

export const avtaleFilterAtom = atomWithStorage<AvtaleFilterType>(
  "avtale-filter",
  defaultAvtaleFilter,
  avtaleFilterStorage,
  { getOnInit: true },
);

const ArrangorerFilterSchema = z.object({
  sok: z.string(),
  page: z.number(),
  pageSize: z.number(),
  sortering: createSorteringProps(z.custom<SorteringArrangorer>()),
});

export type ArrangorerFilterType = z.infer<typeof ArrangorerFilterSchema>;
export const defaultArrangorerFilter: ArrangorerFilterType = {
  sok: "",
  sortering: {
    sortString: SorteringArrangorer.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
  page: 1,
  pageSize: ARRANGORER_PAGE_SIZE,
};

const arrangorerFilterStorage = withStorageValidator(createFilterValidator(ArrangorerFilterSchema))(
  createJSONStorage(() => sessionStorage),
);

export const arrangorerFilterAtom = atomWithStorage<ArrangorerFilterType>(
  "arrangorer-filter",
  defaultArrangorerFilter,
  arrangorerFilterStorage,
  { getOnInit: true },
);

const OppgaverFilterSchema = z.object({
  type: z.nativeEnum(OppgaveType).array(),
  tiltakstyper: z.array(z.string()),
  regioner: z.array(z.string()),
});

export type OppgaverFilterType = z.infer<typeof OppgaverFilterSchema>;

export const defaultOppgaverFilter: OppgaverFilterType = {
  type: [],
  tiltakstyper: [],
  regioner: [],
};

const oppgaverFilterStorage = withStorageValidator(createFilterValidator(OppgaverFilterSchema))(
  createJSONStorage(() => sessionStorage),
);

export const oppgaverFilterAtom = atomWithStorage<OppgaverFilterType>(
  "oppgaver-filter",
  defaultOppgaverFilter,
  oppgaverFilterStorage,
  { getOnInit: true },
);

const avtalerForTiltakstypeFilterAtomFamily = atomFamily<
  AvtaleFilterType,
  WritableAtom<AvtaleFilterType, [newValue: AvtaleFilterType], void>
>(
  (filter: AvtaleFilterType) => {
    return atomWithStorage(
      `avtale-filter-${filter.tiltakstyper[0]}`,
      filter,
      createJSONStorage(() => sessionStorage),
    );
  },
  (a, b) => a.tiltakstyper[0] === b.tiltakstyper[0],
);

export function getAvtalerForTiltakstypeFilterAtom(tiltakstypeId: string) {
  const defaultFilterValue = { ...defaultAvtaleFilter, tiltakstyper: [tiltakstypeId] };
  const filterAtom = avtalerForTiltakstypeFilterAtomFamily(defaultFilterValue);
  return { defaultFilterValue, filterAtom };
}

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["navEnhet"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
