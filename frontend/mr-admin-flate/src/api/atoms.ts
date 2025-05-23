import { ARRANGORER_PAGE_SIZE, AVTALE_PAGE_SIZE, PAGE_SIZE } from "@/constants";
import { SortState } from "@navikt/ds-react";
import { atom, WritableAtom } from "jotai";
import { atomFamily } from "jotai/utils";
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

const safeZodParse = (zodSchema: ZodType, initialValue: unknown, str: string) => {
  try {
    const result = zodSchema.safeParse(JSON.parse(str));
    if (!result.success) {
      return initialValue;
    }
    return result.data;
  } catch {
    return initialValue;
  }
};

/**
 * atomWithStorage fra jotai rendrer først alltid initial value selv om den
 * finnes i storage (https://github.com/pmndrs/jotai/discussions/1879#discussioncomment-5626120)
 * Dette er anbefalt måte og ha en sync versjon av atomWithStorage
 */
function atomWithStorage<Value>(
  key: string,
  initialValue: Value,
  storage: Storage,
  zodSchema: ZodType,
) {
  const baseAtom = atom(storage.getItem(key) ?? JSON.stringify(initialValue));
  return atom(
    (get) => safeZodParse(zodSchema, initialValue, get(baseAtom)),
    (_, set, nextValue: Value) => {
      const str = JSON.stringify(nextValue);
      set(baseAtom, str);
      storage.setItem(key, str);
    },
  );
}

function createSorteringProps(sortItems: z.ZodType) {
  return z.object({
    tableSort: z.custom<SortState>(),
    sortString: sortItems,
  });
}

const tiltakstypeFilterSchema = z.object({
  sort: createSorteringProps(z.custom<SorteringTiltakstyper>()).optional(),
});
export type TiltakstypeFilter = z.infer<typeof tiltakstypeFilterSchema>;

export const defaultTiltakstypeFilter: TiltakstypeFilter = {
  sort: {
    sortString: SorteringTiltakstyper.NAVN_ASCENDING,
    tableSort: {
      orderBy: "navn",
      direction: "ascending",
    },
  },
};

export const tiltakstypeFilterAtom = atomWithStorage<TiltakstypeFilter>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
  sessionStorage,
  tiltakstypeFilterSchema,
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

export const gjennomforingfilterAtom = atomWithStorage<GjennomforingFilterType>(
  "gjennomforing-filter",
  defaultGjennomforingfilter,
  sessionStorage,
  GjennomforingFilterSchema,
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
    sessionStorage,
    GjennomforingFilterSchema,
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

export const avtaleFilterAtom = atomWithStorage<AvtaleFilterType>(
  "avtale-filter",
  defaultAvtaleFilter,
  sessionStorage,
  AvtaleFilterSchema,
);

const arrangorerFilterSchema = z.object({
  sok: z.string(),
  page: z.number(),
  pageSize: z.number(),
  sortering: createSorteringProps(z.custom<SorteringArrangorer>()),
});

export type ArrangorerFilter = z.infer<typeof arrangorerFilterSchema>;
export const defaultArrangorerFilter: ArrangorerFilter = {
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

export const arrangorerFilterAtom = atomWithStorage<ArrangorerFilter>(
  "arrangorer-filter",
  defaultArrangorerFilter,
  sessionStorage,
  arrangorerFilterSchema,
);

const oppgaverFilterSchema = z.object({
  type: z.nativeEnum(OppgaveType).array(),
  tiltakstyper: z.array(z.string()),
  regioner: z.array(z.string()),
});

export type OppgaverFilter = z.infer<typeof oppgaverFilterSchema>;

export const defaultOppgaverFilter: OppgaverFilter = {
  type: [],
  tiltakstyper: [],
  regioner: [],
};

export const oppgaverFilterAtom = atomWithStorage<OppgaverFilter>(
  "oppgaver-filter",
  defaultOppgaverFilter,
  sessionStorage,
  oppgaverFilterSchema,
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
    sessionStorage,
    AvtaleFilterSchema,
  );
});

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<
  "detaljer" | "okonomi" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["navEnhet"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
export const oppgaverFilterAccordionAtom = atom<string[]>(["type", "regioner"]);
