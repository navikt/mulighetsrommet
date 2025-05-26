import { ARRANGORER_PAGE_SIZE } from "@/constants";
import { SortState } from "@navikt/ds-react";
import { atom } from "jotai";
import {
  atomWithStorage,
  createJSONStorage,
  unstable_withStorageValidator as withStorageValidator,
} from "jotai/utils";
import { OppgaveType, SorteringArrangorer, SorteringTiltakstyper } from "@mr/api-client-v2";
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

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
