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

function createFilterValidator<T>(schema: ZodType<T>) {
  return (values: unknown): values is T => {
    return Boolean(schema.safeParse(values).success);
  };
}

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
  lagretFilterIdValgt: z.string().optional(),
});

export type GjennomforingFilterType = z.infer<typeof GjennomforingFilterSchema>;

export const defaultGjennomforingfilter: GjennomforingFilterType = {
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
  lagretFilterIdValgt: undefined,
};

const gjennomforingFilterStorage = withStorageValidator(
  createFilterValidator(GjennomforingFilterSchema),
)(createJSONStorage(() => sessionStorage));

export const gjennomforingfilterAtom = atomWithStorage<GjennomforingFilterType>(
  "gjennomforing-filter",
  defaultGjennomforingfilter,
  gjennomforingFilterStorage,
  { getOnInit: true },
);

export const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  string,
  WritableAtom<GjennomforingFilterType, [newValue: GjennomforingFilterType], void>
>((avtaleId: string) => {
  return atomWithStorage(
    `gjennomforing-filter-${avtaleId}`,
    {
      ...defaultGjennomforingfilter,
      avtale: avtaleId,
    },
    createJSONStorage(() => sessionStorage),
  );
});

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

export const getAvtalerForTiltakstypeFilterAtom = atomFamily<
  string,
  WritableAtom<AvtaleFilterType, [newValue: AvtaleFilterType], void>
>((tiltakstypeId: string) => {
  return atomWithStorage(
    `avtale-filter-${tiltakstypeId}`,
    {
      ...defaultAvtaleFilter,
      tiltakstyper: [tiltakstypeId],
    },
    createJSONStorage(() => sessionStorage),
  );
});

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["navEnhet"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
