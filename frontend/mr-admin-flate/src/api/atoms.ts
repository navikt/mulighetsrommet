import {
  Avtalestatus,
  Avtaletype,
  NavEnhet,
  SorteringAvtaler,
  SorteringTiltaksgjennomforinger,
  SorteringTiltakstyper,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";
import { atom, WritableAtom } from "jotai";
import { atomFamily } from "jotai/utils";
import { AVTALE_PAGE_SIZE, PAGE_SIZE } from "@/constants";
import { RESET } from "jotai/vanilla/utils";
import { ZodType, z } from "zod";

type SetStateActionWithReset<Value> =
  | Value
  | typeof RESET
  | ((prev: Value) => Value | typeof RESET);

const safeJSONParse = (initialValue: unknown) => (str: string) => {
  try {
    return JSON.parse(str);
  } catch (e) {
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
    (get) => {
      const result = zodSchema.safeParse(JSON.parse(get(baseAtom)));
      if (!result.success) {
        return initialValue;
      }
      return result.data;
    },
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
  schema: ZodType,
  options?: {
    serialize?: (val: Value) => string;
    deserialize?: (str: string) => Value;
    subscribe?: (callback: () => void) => () => void;
  },
): WritableAtom<Value, [SetStateActionWithReset<Value>], void> {
  const serialize = options?.serialize || JSON.stringify;
  const deserialize = options?.deserialize || safeJSONParse(initialValue);
  const subscribe =
    options?.subscribe ||
    ((callback) => {
      window.addEventListener("hashchange", callback);
      return () => {
        window.removeEventListener("hashchange", callback);
      };
    });
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

  const strAtom = atomWithStorage<string | null>(key, str, storage, schema);
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

const tiltakstypeFilterSchema = z.object({
  sort: z.custom<SorteringTiltakstyper>().optional(),
});
export type TiltakstypeFilter = z.infer<typeof tiltakstypeFilterSchema>;

export const defaultTiltakstypeFilter: TiltakstypeFilter = {
  sort: SorteringTiltakstyper.NAVN_ASCENDING,
};

export const tiltakstypeFilterAtom = atomWithHashAndStorage<TiltakstypeFilter>(
  "tiltakstype-filter",
  defaultTiltakstypeFilter,
  sessionStorage,
  tiltakstypeFilterSchema,
);

const tiltaksgjennomforingFilterSchema = z.object({
  search: z.string(),
  navEnheter: z.custom<NavEnhet>().array(),
  tiltakstyper: z.string().array(),
  statuser: z.custom<TiltaksgjennomforingStatus>().array(),
  sortering: z.custom<SorteringTiltaksgjennomforinger>(),
  avtale: z.string(),
  arrangorer: z.string().array(),
  visMineGjennomforinger: z.boolean(),
  page: z.number(),
  pageSize: z.number(),
});
export type TiltaksgjennomforingFilter = z.infer<typeof tiltaksgjennomforingFilterSchema>;

export const defaultTiltaksgjennomforingfilter: TiltaksgjennomforingFilter = {
  search: "",
  navEnheter: [],
  tiltakstyper: [],
  statuser: [],
  sortering: SorteringTiltaksgjennomforinger.NAVN_ASCENDING,
  avtale: "",
  arrangorer: [],
  visMineGjennomforinger: false,
  page: 1,
  pageSize: PAGE_SIZE,
};

export const tiltaksgjennomforingfilterAtom = atomWithStorage<TiltaksgjennomforingFilter>(
  "tiltaksgjennomforing-filter",
  defaultTiltaksgjennomforingfilter,
  sessionStorage,
  tiltaksgjennomforingFilterSchema,
);

export const gjennomforingerForAvtaleFilterAtomFamily = atomFamily<
  string,
  WritableAtom<TiltaksgjennomforingFilter, [newValue: TiltaksgjennomforingFilter], void>
>((avtaleId: string) => {
  return atomWithHashAndStorage(
    `tiltaksgjennomforing-filter-${avtaleId}`,
    {
      ...defaultTiltaksgjennomforingfilter,
      avtale: avtaleId,
    },
    sessionStorage,
    tiltaksgjennomforingFilterSchema,
  );
});

const avtaleFilterSchema = z.object({
  sok: z.string(),
  statuser: z.custom<Avtalestatus>().array(),
  avtaletyper: z.custom<Avtaletype>().array(),
  navRegioner: z.string().array(),
  tiltakstyper: z.string().array(),
  sortering: z.custom<SorteringAvtaler>(),
  arrangorer: z.string().array(),
  visMineAvtaler: z.boolean(),
  page: z.number(),
  pageSize: z.number(),
});
export type AvtaleFilter = z.infer<typeof avtaleFilterSchema>;

export const defaultAvtaleFilter: AvtaleFilter = {
  sok: "",
  statuser: [],
  avtaletyper: [],
  navRegioner: [],
  tiltakstyper: [],
  sortering: SorteringAvtaler.NAVN_ASCENDING,
  arrangorer: [],
  visMineAvtaler: false,
  page: 1,
  pageSize: AVTALE_PAGE_SIZE,
};

export const avtaleFilterAtom = atomWithHashAndStorage<AvtaleFilter>(
  "avtale-filter",
  defaultAvtaleFilter,
  sessionStorage,
  avtaleFilterSchema,
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
    avtaleFilterSchema,
  );
});

export const gjennomforingDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const avtaleDetaljerTabAtom = atom<"detaljer" | "redaksjonelt-innhold">("detaljer");

export const gjennomforingFilterAccordionAtom = atom<string[]>(["status"]);
export const avtaleFilterAccordionAtom = atom<string[]>(["status"]);
