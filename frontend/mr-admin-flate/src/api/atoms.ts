import { ARRANGORER_PAGE_SIZE, AVTALE_PAGE_SIZE } from "@/constants";
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
  OppgaveType,
  SorteringArrangorer,
  SorteringAvtaler,
  SorteringTiltakstyper,
} from "@mr/api-client-v2";
import { z, ZodType } from "zod";

export function createSorteringProps(sortItems: z.ZodType) {
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

export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
