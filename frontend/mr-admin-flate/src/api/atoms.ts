import { ARRANGORER_PAGE_SIZE, AVTALE_PAGE_SIZE, PAGE_SIZE } from "@/constants";
import { SortState } from "@navikt/ds-react";
import { atom, WritableAtom } from "jotai";
import { atomFamily } from "jotai/utils";
import { RESET } from "jotai/vanilla/utils";
import {
  Avtalestatus,
  Avtaletype,
  NavEnhet,
  OppgaveType,
  SorteringArrangorer,
  SorteringAvtaler,
  SorteringGjennomforinger,
  SorteringTiltakstyper,
  GjennomforingStatus,
} from "@mr/api-client";
import { z, ZodType } from "zod";

type SetStateActionWithReset<Value> =
  | Value
  | typeof RESET
  | ((prev: Value) => Value | typeof RESET);

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

/**
 * Kopiert fra https://github.com/jotaijs/jotai-location/blob/main/src/atomWithHash.ts
 * med unntak av:
 * - det indre atom'et som er endret til et storage atom
 * - setHash har alltid "replaceState" oppførsel (dvs. lager ikke ny entry i history)
 * - Forsøker å lese inn hash først (ytterst i funksjonen her), uten dette funka ikke
 *   lenking med hash i ny fane
 */
function atomWithHashAndStorage<Value>(
  key: string,
  initialValue: Value,
  storage: Storage,
  zodSchema: ZodType,
): WritableAtom<Value, [SetStateActionWithReset<Value>], void> {
  const serialize = JSON.stringify;
  const deserialize = (str: string) => safeZodParse(zodSchema, initialValue, str);
  const subscribe = (callback: any) => {
    window.addEventListener("hashchange", callback);
    return () => {
      window.removeEventListener("hashchange", callback);
    };
  };
  const setHash = (searchParams: string) => {
    window.history.replaceState(
      window.history.state,
      "",
      `${window.location.pathname}${window.location.search}#${searchParams}`,
    );
  };

  let str = null;
  if (typeof window !== "undefined" && window.location) {
    const searchParams = new URLSearchParams(window.location.hash.slice(1));
    str = searchParams.get(key);
  }

  const strAtom = atomWithStorage<string | null>(key, str, storage, z.string().nullable());
  strAtom.onMount = (setAtom) => {
    if (typeof window === "undefined" || !window.location) {
      return undefined;
    }
    const callback = () => {
      const searchParams = new URLSearchParams(window.location.hash.slice(1));
      const str = searchParams.get(key);
      if (str != null) {
        setAtom(str);
      }
    };
    const unsubscribe = subscribe(callback);
    callback();
    return unsubscribe;
  };
  const valueAtom = atom((get) => {
    const str = get(strAtom);
    return str === null ? initialValue : deserialize(str);
  });
  return atom(
    (get) => get(valueAtom),
    (get, set, update: SetStateActionWithReset<Value>) => {
      const nextValue =
        typeof update === "function"
          ? (update as (prev: Value) => Value | typeof RESET)(get(valueAtom))
          : update;
      const searchParams = new URLSearchParams(window.location.hash.slice(1));
      if (nextValue === RESET) {
        set(strAtom, null);
        searchParams.delete(key);
      } else {
        const str = serialize(nextValue);
        set(strAtom, str);
        searchParams.set(key, str);
      }
      setHash(searchParams.toString());
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

export const tiltakstypeFilterAtom = atomWithHashAndStorage<TiltakstypeFilter>(
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
export type GjennomforingFilter = z.infer<typeof GjennomforingFilterSchema>;

export const defaultGjennomforingfilter: GjennomforingFilter = {
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

export const gjennomforingfilterAtom = atomWithStorage<GjennomforingFilter>(
  "tiltaksgjennomforing-filter",
  defaultGjennomforingfilter,
  sessionStorage,
  GjennomforingFilterSchema,
);

export const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  string,
  WritableAtom<GjennomforingFilter, [newValue: GjennomforingFilter], void>
>((avtaleId: string) => {
  return atomWithHashAndStorage(
    `tiltaksgjennomforing-filter-${avtaleId}`,
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
  personvernBekreftet: z.boolean().array(),
  page: z.number(),
  pageSize: z.number(),
  lagretFilterIdValgt: z.string().optional(),
});
export type AvtaleFilter = z.infer<typeof AvtaleFilterSchema>;

export const defaultAvtaleFilter: AvtaleFilter = {
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
  personvernBekreftet: [],
  page: 1,
  pageSize: AVTALE_PAGE_SIZE,
  lagretFilterIdValgt: undefined,
};

export const avtaleFilterAtom = atomWithHashAndStorage<AvtaleFilter>(
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

export const arrangorerFilterAtom = atomWithHashAndStorage<ArrangorerFilter>(
  "arrangorer-filter",
  defaultArrangorerFilter,
  sessionStorage,
  arrangorerFilterSchema,
);

const oppgaverFilterSchema = z.object({
  type: z.enum([OppgaveType.TILSAGN_TIL_BESLUTNING, OppgaveType.TILSAGN_TIL_ANNULLERING]).array(),
  tiltakstyper: z.array(z.string()),
});

export type OppgaverFilter = z.infer<typeof oppgaverFilterSchema>;

const defaultOppgaverFilter: OppgaverFilter = {
  type: [],
  tiltakstyper: [],
};

export const oppgaverFilterAtom = atomWithHashAndStorage<OppgaverFilter>(
  "oppgaver-filter",
  defaultOppgaverFilter,
  sessionStorage,
  oppgaverFilterSchema,
);

export const getAvtalerForTiltakstypeFilterAtom = atomFamily<
  string,
  WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>
>((tiltakstypeId: string) => {
  return atomWithHashAndStorage(
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
  "detaljer" | "pris-og-fakturering" | "personvern" | "redaksjonelt-innhold"
>("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["navEnhet"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["region"]);
export const oppgaverFilterAccordionAtom = atom<string[]>(["type"]);
